package ch.magori.blox.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.data.DailyChallengeRepository
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
import ch.magori.blox.core.haptics.Haptic
import ch.magori.blox.core.haptics.Haptics
import ch.magori.blox.core.music.LiveMusic
import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.SoundEffects
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.feedback.ClearingRows
import ch.magori.blox.feature.game.feedback.GameFeedback
import ch.magori.blox.feature.game.haptics.hapticFor
import ch.magori.blox.feature.game.music.ClassicMusicDirection
import ch.magori.blox.feature.game.music.GameMusic
import ch.magori.blox.feature.game.music.LiveMusicDirection
import ch.magori.blox.feature.game.music.MusicDirection
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.results.GameEnd
import ch.magori.blox.feature.game.sound.soundEffectsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
    setup: GameSetup,
    route: GameRoute,
    private val preferences: UserPreferencesRepository,
    private val music: MusicPlayer,
    private val haptics: Haptics,
    private val soundEffects: SoundEffectPlayer,
    private val composeLive: suspend (seed: Long) -> LiveMusic,
    savedState: SavedStateHandle,
    private val appScope: CoroutineScope,
) : ViewModel() {
    private val engine = setup.engines.create(route.seed)

    /** Where the results of this game go, as its variant says (ADR 0026). */
    private val results = setup.results

    /** Reads the music of this game, as its variant says (ADR 0026): null for none. */
    private val gameMusic = setup.music

    /** What the screen says of the variant of this game (ADR 0026): its title, its best to beat, Copier or not. */
    internal val labels: GameLabels = setup.labels

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
     * What happened in the game and on which tick, for the effects the screen draws (ADR 0024): followed tick by tick,
     * from the events and the phase of the engine.
     */
    private var feedback = GameFeedback()

    /**
     * Whether this game was restored after its process died: its [savedState] says it had started. It is then the same
     * game, whose start was already told to its results.
     */
    private val restored = savedState.contains(STARTED_KEY)

    /**
     * The last state the engine returned: the only one it is given back (ADR 0014). A game whose process
     * died while it was under way (its [savedState] says it had started) is not taken up where it stood,
     * which the engine cannot do: it starts again from its route, paused, until the player resumes it or
     * leaves it.
     */
    private var gameState: GameState = engine.start(Level(route.startLevel)).state.let { started ->
        if (restored) engine.apply(started, Input.Pause).state else started
    }

    // Until the settings answer, where the keys stand is not known: none is shown, and the game does not run.
    private val state =
        MutableStateFlow(gameState.toUiState(nextPieces::getValue).withLeftHanded(null, gameState.isUnderWay))

    internal val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /**
     * The music this game plays from its start, from « Mes musiques » or the settings: a score, the music file of the
     * player, or the calm music of its live music; null when none is played.
     */
    private var gameSong: Music? = null

    /**
     * What the music of this game does as the game goes, chosen with its music ([directMusic]); null until the music is
     * known, or when the game has none.
     */
    private var musicDirection: MusicDirection? = null

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

    /**
     * Whether the game plays its sound effects (a rotation, lines cleared, a level up, the game over), from the
     * settings; false until they answer, as the vibrations.
     */
    private var soundEffectsOn = false

    /** Whether a key pressed vibrates, from the settings; false until they answer, as the vibrations. */
    private var keyVibration = false

    /**
     * The start of the game, told to its [results] once: a daily try is counted as soon as it starts, and a game
     * [restored] after the process died is the same try (null). In the [appScope]: a game left at once is still
     * counted. Its end waits for it: an end told before its start would count a second try, or none. Results that
     * cannot be written do not stop the game ([unlessUnwritable]).
     */
    private val started: Job? = if (restored) null else appScope.launch { unlessUnwritable { results.start() } }

    init {
        // Marks the game as started, in the saved state of its navigation entry: what the system gives back if the
        // process dies, unlike this view model. A configuration change keeps the view model, and so the game.
        savedState[STARTED_KEY] = true
        // The record shown in the top bar: its real value, followed as it changes. Unreadable records are
        // absorbed by the repository (its contract); any other failure is a bug, and is not swallowed here.
        viewModelScope.launch {
            results.best.collect { record ->
                state.update { it.copy(record = record).withBestBeaten(it.bestBeaten) }
            }
        }
        // The music of the variant of the game ([GameVariants]), at the normal tempo, as every new play of the player.
        // Unreadable musics give none, unreadable settings the defaults (the contracts of the repositories).
        viewModelScope.launch {
            directMusic(gameMusic(), route.seed)
            // The musics or the settings may answer once the game is paused or over: the music then waits, or never
            // comes.
            if (gameState.isUnderWay) play(playMusic())
        }
        // Whether the game and its keys vibrate, the game plays its sound effects and its keys are laid out for the left
        // hand, from the settings, read once, apart: the music of the player does not wait for them.
        viewModelScope.launch {
            val settings = preferences.preferences.first()
            vibrations = settings.vibrations
            soundEffectsOn = settings.soundEffects
            keyVibration = settings.keyVibration
            state.update { it.withLeftHanded(settings.leftHanded, gameState.isUnderWay) }
            // At once, so that the first rotation does not wait for its track to open.
            if (soundEffectsOn) soundEffects.warmUp(SoundEffects.all)
        }
    }

    /**
     * [chosen] is the music of this game: the song it plays and its [musicDirection], the Classic one for a music played
     * as it is, the live one for the live music, composed from the [seed] of the game ([composeLive]), which starts on
     * its calm music; a game already over composes none, which it would never play. Nothing for no music. The direction
     * starts from where the game stands once its music is known: a music read or composed late may come after a level
     * up, or once the stack is in danger.
     */
    private suspend fun directMusic(
        chosen: GameMusic?,
        seed: Long,
    ) {
        when (chosen) {
            null -> Unit

            is GameMusic.Fixed -> {
                gameSong = chosen.music
                musicDirection = ClassicMusicDirection(inDanger = gameState.stackInDanger)
            }

            GameMusic.Live -> {
                if (gameState.phase is Phase.GameOver) return
                val live = composeLive(seed)
                gameSong = live.calm.score
                musicDirection = gameState.let {
                    LiveMusicDirection(
                        live,
                        startLevel = it.progress.startLevel,
                        level = it.progress.level,
                        inDanger = it.stackInDanger,
                    )
                }
            }
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
     * Plays the music of this game from its start, then lets the [musicDirection] start: it prepares what it will need
     * (the Classic, its danger tempo), and answers for where the game stands (a tempo, the tense music of a live game
     * already in danger). Null when the game has no music.
     */
    private fun playMusic(): Playback? = gameSong?.let { music.play(it).also { musicDirection?.onStart(music) } }

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
            val before = gameState
            val update = engine.tick(before)
            gameState = update.state
            feedback = feedback.after(
                tick = gameState.tickCount,
                events = update.events,
                clearing = gameState.phase.clearingRows(),
                scoreBefore = before.progress.score,
                score = gameState.progress.score,
            )
            if (GameEvent.GameOver in update.events) over = true
            musicDirection?.onEvents(update.events, music)
            if (vibrations) update.events.mapNotNull(::hapticFor).forEach(haptics::play)
            if (soundEffectsOn) soundEffectsFor(update.events).forEach(soundEffects::play)
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

    /**
     * The screen state of the game state, with what the engine does not know kept: the record, whether it is beaten,
     * the game over.
     */
    private fun publish() {
        state.update {
            gameState.toUiState(nextPieces::getValue)
                .copy(record = it.record, gameOver = it.gameOver, feedback = feedback)
                .withLeftHanded(it.leftHanded, gameState.isUnderWay)
                .withBestBeaten(it.bestBeaten)
        }
    }

    /**
     * The game is over in [finalState]: its score is given to its [results] at once (the records, or the results of
     * the day), so that a record is not lost if the process dies; the game over screen shows once they answered
     * whether it is a new one, so that what it offers to copy (a daily game) holds this game.
     */
    private fun onGameOver(finalState: GameState) {
        stopMusic()
        viewModelScope.launch {
            val result = endResults(finalState.progress.score.value)
            val gameOver = finalState.toGameOverUiState(
                newBest = result?.newBest == true,
                resultsKept = result?.kept == true,
            )
            state.update { it.copy(gameOver = gameOver) }
        }
    }

    /**
     * Gives [score] to the [results], once its start is written: whether it is a new record, and whether they keep it.
     * Results that cannot be written (an [IOException], the contract of the repositories) give null, and do not stop
     * the game over from showing, without a new record; any other failure is a bug, and is not swallowed here.
     */
    private suspend fun endResults(score: Int): GameEnd? = try {
        // Not cancellable: the score is saved even if the view model is cleared while it is being written.
        withContext(NonCancellable) {
            started?.join()
            results.end(score)
        }
    } catch (_: IOException) {
        null
    }

    /**
     * The game screen is left (Menu, Rejouer, system back): its music stops with it. A game left before its end gives
     * its [results] the points of the moment, in the [appScope]: the scope of this view model is cancelled by now.
     */
    override fun onCleared() {
        stopMusic()
        if (gameState.phase is Phase.GameOver) return
        val points = gameState.progress.score.value
        appScope.launch {
            started?.join()
            unlessUnwritable { results.leave(points) }
        }
    }

    /**
     * Writes to the [results] with [write]: results that cannot be written (an [IOException], the contract of the
     * repositories) are given up, the game goes on; any other failure is a bug, and is not swallowed here.
     */
    private suspend fun unlessUnwritable(write: suspend () -> Unit) {
        try {
            write()
        } catch (_: IOException) {
            // Nothing to do: the results stay as they were, as the contract of the repositories says.
        }
    }

    /**
     * A button of the screen went down or up: the engine records it at once in its controller, and the
     * ticks of the next frame read it. Several changes between two frames are all recorded, in order. A key
     * pressed vibrates at once, under the finger, when the player chose it, only while the game runs: not in
     * pause, not once it is over; a key released does not.
     */
    internal fun onControl(change: ControlChange) {
        // Before the settings answer, no key is shown and the game does not run: a key of a keyboard does nothing.
        if (state.value.leftHanded == null) return
        gameState = engine.apply(gameState, change.toInput()).state
        if (keyVibration && change is ControlChange.Pressed && gameState.isUnderWay) haptics.play(Haptic.KEY_PRESS)
    }

    /**
     * Builds the view model of a game. The feature defines it, `app` builds it; the route comes in the
     * [CreationExtras] under [RouteKey], since only the navigation entry knows it (ADR 0013). [musicOf]
     * gives the music of a choice, null for none: a score or the music file of the player ([GameMusic.Fixed]), or the
     * live music ([GameMusic.Live]); `app` reads it with the music file kept, under the contract of
     * `MusicFileRepository`: never an [IOException]. [gameMusic] reads the score of the music of the player chosen for
     * the games, null for none; `app` reads it from its musics, under the contract of `MusicLibrary.gameMusic`: never an
     * [IOException], unreadable musics give none. The live music of a game is composed from its seed off the main
     * thread.
     *
     * [engineFactory] deals the pieces of the Classic, whose score goes to the [highScores]; the daily game deals those
     * of its seed and goes to [dailyChallenge] (ADR 0026), one write at a time, in the order they are asked for.
     * [appScope] lives as long as the application, on `Dispatchers.Main.immediate`: a game tells its results its start
     * there, and the points of a game left before its end once its own scope is cancelled, at once, in the order of the
     * games.
     */
    class Factory(
        engineFactory: GameEngineFactory,
        highScores: HighScoreRepository,
        private val preferences: UserPreferencesRepository,
        private val music: MusicPlayer,
        private val haptics: Haptics,
        private val soundEffects: SoundEffectPlayer,
        musicOf: suspend (MusicChoice) -> GameMusic?,
        gameMusic: suspend () -> Score?,
        dailyChallenge: DailyChallengeRepository,
        private val appScope: CoroutineScope,
    ) : ViewModelProvider.Factory {
        private val variants = GameVariants(
            engineFactory,
            highScores,
            preferences,
            musicOf,
            gameMusic,
            dailyChallenge,
            dailyWrites = Mutex(),
            dailyEngines = StableClassicEngines,
        )

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
                variants.setupOf(route.variant),
                route,
                preferences,
                music,
                haptics,
                soundEffects,
                ::composeLiveMusic,
                savedState,
                appScope,
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

/**
 * The live music of a game composed from its [seed], off the main thread: composing it costs some tens of milliseconds
 * (docs/musique-vivante.md, Composer les deux ambiances).
 */
private suspend fun composeLiveMusic(seed: Long): LiveMusic =
    withContext(Dispatchers.Default) { LiveMusic.compose(seed) }

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

/** The full rows of [this] phase and the ticks left before they are cleared, while it clears them; null otherwise. */
private fun Phase.clearingRows(): ClearingRows? =
    (this as? Phase.Clearing)?.let { ClearingRows(it.rowsCleared.clearedRows, it.ticksLeft.value) }
