package ch.magori.blox.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.game.engine.Button
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.Input
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.haptics.Haptics
import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.Score
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.haptics.hapticFor
import ch.magori.blox.feature.game.music.DANGER_TEMPO
import ch.magori.blox.feature.game.music.tempoFactorFor
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.sidepanel.nextPieceCells
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.reflect.KClass

/**
 * The game of one [GameRoute]: one engine, created for this game from its seed (ADR 0014), started
 * at its level. The only writer of the game state (ADR 0012): the screen gives it the frames, it
 * turns them into ticks of the engine.
 *
 * A game is never taken up where it stood (ADR 0014): once its process died, the navigation restores
 * its entry and a new view model plays the game of the route again, paused, so that nothing moves nor
 * plays before the player chooses to resume it.
 */
class GameViewModel internal constructor(
    engineFactory: GameEngineFactory,
    route: GameRoute,
    private val highScores: HighScoreRepository,
    private val preferences: UserPreferencesRepository,
    private val music: MusicPlayer,
    private val haptics: Haptics,
    private val musicOf: suspend (MusicChoice) -> Music?,
    private val gameMusic: suspend () -> Score?,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val engine = engineFactory.create(route.seed)

    /**
     * The cells of the next piece shown, for each type, in the spawn orientation of the rules of the engine:
     * computed once, so that a frame neither computes them again nor hands the screen a new instance.
     */
    private val nextPieces = PieceType.entries.associateWith { nextPieceCells(it, engine.rules.rotationSystem) }

    /**
     * The frames of the screen turned into ticks of 1/60 s (ADR 0006 and 0012). A frame never gives more than
     * [MAX_TICKS_PER_FRAME] ticks: after a long pause (application in the background), the surplus is dropped
     * instead of being caught up. Below 15 frames per second (60 / 4), the cap therefore makes the game run slower
     * instead of catching up, which is intended (ADR 0006). Above 60 Hz, the ticks would fall unevenly on the frames
     * (one or none in turn at 90 Hz): the game screen asks the display for 60 Hz while it shows ([gameDisplayMode]).
     */
    private val frames = FrameTicks(maxTicksPerFrame = MAX_TICKS_PER_FRAME)

    /**
     * The last state the engine returned: the only one it is given back (ADR 0014). A game whose process
     * died while it was under way (its [savedState] says it had started) is not taken up where it stood,
     * which the engine cannot do: it starts again from its route, paused, until the player resumes it or
     * leaves it.
     */
    private var gameState: GameState = engine.start(Level(route.startLevel)).state.let { started ->
        if (savedState.contains(STARTED_KEY)) engine.apply(started, Input.Pause).state else started
    }
    private val state = MutableStateFlow(gameState.toUiState(nextPieces::getValue))

    internal val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /**
     * The music of this game, from « Mes musiques » or the settings: a score, or the music file of the player; null
     * when none is played.
     */
    private var gameSong: Music? = null

    /**
     * The music of this game, playing or stopped: stopping it leaves the music of another game alone. Kept
     * once stopped, so that the resume goes on where it stopped. Its [Playback.isPlaying] is followed
     * ([musicWatch]): a music that stops by itself (an audio output that fails) is stopped, which gives the
     * audio focus back, and the game goes on in silence; the resume after a pause tries again.
     */
    private var playback: Playback? = null

    /**
     * The following of the [playback] last played, to stop it once its music stopped by itself; cancelled when
     * it is stopped on purpose. Null until a music plays.
     */
    private var musicWatch: Job? = null

    /**
     * Whether the game vibrates on the lines cleared and at the game over, from the settings; false until they
     * answer, so that a game never vibrates against the choice of the player.
     */
    private var vibrations = false

    init {
        // Marks the game as started, in the saved state of its navigation entry: what the system gives back if the
        // process dies, unlike this view model. A configuration change keeps the view model, and so the game.
        savedState[STARTED_KEY] = true
        // The record shown in the top bar: its real value, followed as it changes. Unreadable records are
        // absorbed by the repository (its contract); any other failure is a bug, and is not swallowed here.
        viewModelScope.launch {
            highScores.highScore.collect { record -> state.update { it.copy(record = record) } }
        }
        // The music of the player chosen for the games (« Mes musiques »), which plays instead of the one chosen on the
        // home screen, even when that one is none; the settings are read only without it. At the normal tempo, as every
        // new play of the player. Unreadable musics give none, unreadable settings the defaults (the contracts of the
        // repositories).
        viewModelScope.launch {
            gameSong = gameMusic() ?: musicOf(preferences.preferences.first().music)
            // The musics or the settings may answer once the game is paused or over: the music then waits, or never
            // comes.
            if (gameState.isUnderWay) play(playMusic())
        }
        // Whether the game vibrates, from the settings, read apart: the music of the player does not wait for them.
        viewModelScope.launch {
            vibrations = preferences.preferences.first().vibrations
        }
    }

    /**
     * [started] is the music of this game from now on, followed until it stops; null when the game has no music.
     * A music plays only at the start of the game or when it resumes, once the following of the one before
     * was cancelled by the pause, or ended with its music: there is none to cancel here.
     */
    private fun play(started: Playback?) {
        playback = started
        musicWatch = started?.let { viewModelScope.launch { watch(it) } }
    }

    /**
     * Waits for [started] to stop by itself (the audio output fails; with the audio focus, the focus lost for
     * good or the headphones unplugged; a score has no natural end, it loops), then stops it, which gives the
     * audio focus back. The game goes on in silence. Changes of tempo are still given to the player, which
     * keeps them for the resume and plays nothing: the music comes back only with the resume after a pause,
     * once the audio output may have come back.
     */
    private suspend fun watch(started: Playback) {
        started.isPlaying.first { !it }
        started.stop()
    }

    /**
     * Stops the music of this game, if any, without taking its stop for a music that stopped by itself. After
     * a music that stopped by itself, its playback is stopped again: it was already, which does nothing (the
     * contract of [Playback.stop]).
     */
    private fun stopMusic() {
        musicWatch?.cancel()
        playback?.stop()
    }

    /**
     * Plays the music of this game from its start, its danger tempo prepared at once: entering the danger,
     * the most tense moment of the game, then renders nothing. Null when the game has no music.
     */
    private fun playMusic(): Playback? = gameSong?.let { music.play(it).also { music.prepareTempo(DANGER_TEMPO) } }

    /**
     * The screen starts drawing frames again (first display, back from another screen or from the
     * background): the time spent away is not played; the next frame only sets the time reference.
     */
    internal fun onFramesStarted() {
        frames.reset()
    }

    /**
     * A frame was drawn at [frameNanos]: the engine plays the ticks due since the previous frame. The
     * screen hands frames only while the game runs ([GameUiState.running]). Once the game is over, a frame
     * that comes anyway does nothing: the game over screen state is published once, apart. While the game is
     * paused, resuming starts the loop again ([onFramesStarted]); a frame that comes anyway plays its ticks,
     * which change nothing, not even the duration of the game, and only moves the time reference on.
     */
    internal fun onFrame(frameNanos: Long) {
        if (gameState.phase is Phase.GameOver) return
        val ticks = frames.advance(frameNanos)
        // No tick due on this frame (the first one, which only sets the time reference, or one that comes before
        // a whole tick has passed): the game has not changed, there is nothing to publish.
        if (ticks == 0) return
        var over = false
        repeat(ticks) {
            val update = engine.tick(gameState)
            gameState = update.state
            if (GameEvent.GameOver in update.events) over = true
            update.events.filterIsInstance<GameEvent.StackDanger>().forEach { music.setTempoFactor(tempoFactorFor(it)) }
            if (vibrations) update.events.mapNotNull(::hapticFor).forEach(haptics::play)
        }
        publish()
        if (over) onGameOver(gameState)
    }

    /** The player pauses the game (pause button, system back): nothing moves until it resumes. */
    internal fun onPause() {
        gameState = engine.apply(gameState, Input.Pause).state
        publish()
        stopMusic()
    }

    /** The player resumes the paused game (Reprendre, system back): it goes on where it stood. */
    internal fun onResume() {
        val wasPaused = gameState.paused
        gameState = engine.apply(gameState, Input.Resume).state
        publish()
        // Only when the game really goes on again (not resumed twice, not over): the music goes on where it
        // stopped, or starts if the game was paused before it started.
        if (wasPaused && gameState.isUnderWay) play(playback?.resume() ?: playMusic())
    }

    /** The screen state of the game state, with what the engine does not know kept: the record, the game over. */
    private fun publish() {
        state.update { gameState.toUiState(nextPieces::getValue).copy(record = it.record, gameOver = it.gameOver) }
    }

    /**
     * The game is over in [finalState]: its score is submitted to the records at once, so that a record is
     * not lost if the process dies; the game over screen shows once the records answered whether it is a
     * new one.
     */
    private fun onGameOver(finalState: GameState) {
        stopMusic()
        viewModelScope.launch {
            val gameOver = finalState.toGameOverUiState(isNewRecord(finalState.progress.score.value))
            state.update { it.copy(gameOver = gameOver) }
        }
    }

    /**
     * Submits [score] to the records: whether it is a new record. Records that cannot be written (an
     * [IOException], the contract of the repository) do not stop the game over from showing, without a new
     * record; any other failure is a bug, and is not swallowed here.
     */
    private suspend fun isNewRecord(score: Int): Boolean = try {
        // Not cancellable: the score is saved even if the view model is cleared while it is being written.
        withContext(NonCancellable) { highScores.submit(score) }.isNewRecord
    } catch (_: IOException) {
        false
    }

    /** The game screen is left (Menu, Rejouer, system back): its music stops with it. */
    override fun onCleared() {
        stopMusic()
    }

    /**
     * A button of the screen went down or up: the engine records it at once in its controller, and the
     * ticks of the next frame read it. Several changes between two frames are all recorded, in order.
     */
    internal fun onControl(change: ControlChange) {
        gameState = engine.apply(gameState, change.toInput()).state
    }

    /**
     * Builds the view model of a game. The feature defines it, `app` builds it; the route comes in the
     * [CreationExtras] under [RouteKey], since only the navigation entry knows it (ADR 0013). [musicOf]
     * gives the music of a choice, a score or the music file of the player, null for none; `app` reads it with the
     * music file kept, under the contract of `MusicFileRepository`: never an [IOException]. [gameMusic] reads
     * the score of the music of the player chosen for the games, null for none; `app` reads it from its musics, under
     * the contract of `MusicLibrary.gameMusic`: never an [IOException], unreadable musics give none.
     */
    class Factory(
        private val engineFactory: GameEngineFactory,
        private val highScores: HighScoreRepository,
        private val preferences: UserPreferencesRepository,
        private val music: MusicPlayer,
        private val haptics: Haptics,
        private val musicOf: suspend (MusicChoice) -> Music?,
        private val gameMusic: suspend () -> Score?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            val route = requireNotNull(extras[RouteKey]) { "The game route is missing from the creation extras" }
            // The saved state of the entry, from its owner (the decorator of Navigation 3): the system gives it back
            // after the process died, with the mark that the game had started.
            val savedState = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(
                engineFactory,
                route,
                highScores,
                preferences,
                music,
                haptics,
                musicOf,
                gameMusic,
                savedState,
            ) as T
        }
    }

    companion object {
        /** Where the navigation entry puts the [GameRoute] for [Factory]. */
        val RouteKey: CreationExtras.Key<GameRoute> = object : CreationExtras.Key<GameRoute> {}

        /** The key, in the saved state of the entry, of the mark that its game has started. */
        private const val STARTED_KEY = "ch.magori.blox.feature.game.started"

        /** The most ticks a frame gives: the game is never caught up by more than 4 / 60 s at once. */
        private const val MAX_TICKS_PER_FRAME = 4
    }
}

/** The input of the engine for [this] change of a button of the screen. */
private fun ControlChange.toInput(): Input {
    val engineButton = when (button) {
        ControlButton.LEFT -> Button.LEFT
        ControlButton.DOWN -> Button.DOWN
        ControlButton.RIGHT -> Button.RIGHT
        ControlButton.A -> Button.A
        ControlButton.B -> Button.B
    }
    return when (this) {
        is ControlChange.Pressed -> Input.Press(engineButton)
        is ControlChange.Released -> Input.Release(engineButton)
    }
}
