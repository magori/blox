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
import ch.magori.blox.core.game.engine.Button
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.Input
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.music.Score
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.music.tempoFactorFor
import ch.magori.blox.feature.game.navigation.GameRoute
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
    private val scoreOf: (MusicChoice) -> Score?,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val engine = engineFactory.create(route.seed)

    /** The rotation system of the rules of the engine, which the screen draws the pieces with. */
    private val rotationSystem = engine.rules.rotationSystem
    private val loop = GameLoop()

    /**
     * The last state the engine returned: the only one it is given back (ADR 0014). A game whose process
     * died while it was under way (its [savedState] says it had started) is not taken up where it stood,
     * which the engine cannot do: it starts again from its route, paused, until the player resumes it or
     * leaves it.
     */
    private var gameState: GameState = engine.start(Level(route.startLevel)).state.let { started ->
        if (savedState.contains(STARTED_KEY)) engine.apply(started, Input.Pause).state else started
    }
    private val state = MutableStateFlow(gameState.toUiState(rotationSystem))

    internal val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /** The score of the music of this game, from the settings; null when none is played. */
    private var score: Score? = null

    /** The music of this game while it plays: stopping it leaves the music of another game alone. */
    private var playback: Playback? = null

    init {
        // Marks the game as started, in the saved state of its navigation entry: what the system gives back if the
        // process dies, unlike this view model. A configuration change keeps the view model, and so the game.
        savedState[STARTED_KEY] = true
        // The record shown in the top bar: its real value, followed as it changes. Unreadable records are
        // absorbed by the repository (its contract); any other failure is a bug, and is not swallowed here.
        viewModelScope.launch {
            highScores.highScore.collect { record -> state.update { it.copy(record = record) } }
        }
        // The music chosen on the home screen, at the normal tempo, as every new play of the player. Unreadable
        // settings give the defaults (the contract of the repository).
        viewModelScope.launch {
            val choice = preferences.preferences.first().music
            score = scoreOf(choice)
            // The settings may answer once the game is paused or over: the music then waits, or never comes.
            if (isUnderWay()) playback = score?.let(music::play)
        }
    }

    /**
     * The screen starts drawing frames again (first display, back from another screen or from the
     * background): the time spent away is not played; the next frame only sets the time reference.
     */
    internal fun onFramesStarted() {
        loop.reset()
    }

    /**
     * A frame was drawn at [frameNanos]: the engine plays the ticks due since the previous frame. Once
     * the game is over, a frame does nothing: the game over screen state is published once, apart. While the
     * game is paused, the screen hands no frame, and resuming starts the loop again ([onFramesStarted]); a
     * frame that comes anyway plays its ticks, which change nothing, not even the duration of the game, and
     * only moves the time reference on.
     */
    internal fun onFrame(frameNanos: Long) {
        if (gameState.phase is Phase.GameOver) return
        val ticks = loop.advance(frameNanos)
        // No tick due on this frame (the first one, which only sets the time reference, or one that comes before
        // a whole tick has passed): the game has not changed, there is nothing to publish.
        if (ticks == 0) return
        var over = false
        repeat(ticks) {
            val update = engine.tick(gameState)
            gameState = update.state
            if (GameEvent.GameOver in update.events) over = true
            update.events.filterIsInstance<GameEvent.StackDanger>().forEach { music.setTempoFactor(tempoFactorFor(it)) }
        }
        publish()
        if (over) onGameOver(gameState)
    }

    /** The player pauses the game (pause button, system back): nothing moves until it resumes. */
    internal fun onPause() {
        gameState = engine.apply(gameState, Input.Pause).state
        publish()
        playback?.stop()
    }

    /** The player resumes the paused game (Reprendre, system back): it goes on where it stood. */
    internal fun onResume() {
        val wasPaused = gameState.paused
        gameState = engine.apply(gameState, Input.Resume).state
        publish()
        // Only when the game really goes on again (not resumed twice, not over): the music goes on where it
        // stopped, or starts if the game was paused before it started.
        if (wasPaused && isUnderWay()) playback = playback?.resume() ?: score?.let(music::play)
    }

    /** The screen state of the game state, with what the engine does not know kept: the record, the game over. */
    private fun publish() {
        state.update { gameState.toUiState(rotationSystem).copy(record = it.record, gameOver = it.gameOver) }
    }

    /**
     * The game is over in [finalState]: its score is submitted to the records at once, so that a record is
     * not lost if the process dies; the game over screen shows once the records answered whether it is a
     * new one.
     */
    private fun onGameOver(finalState: GameState) {
        playback?.stop()
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

    /** Whether the game goes on: neither paused nor over. */
    private fun isUnderWay(): Boolean = !gameState.paused && gameState.phase !is Phase.GameOver

    /** The game screen is left (Menu, Rejouer, system back): its music stops with it. */
    override fun onCleared() {
        playback?.stop()
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
     * [CreationExtras] under [RouteKey], since only the navigation entry knows it (ADR 0013). [scoreOf]
     * gives the score of a music choice, null for none; `app` defines it, for the home screen too.
     */
    class Factory(
        private val engineFactory: GameEngineFactory,
        private val highScores: HighScoreRepository,
        private val preferences: UserPreferencesRepository,
        private val music: MusicPlayer,
        private val scoreOf: (MusicChoice) -> Score?,
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
            return GameViewModel(engineFactory, route, highScores, preferences, music, scoreOf, savedState) as T
        }
    }

    companion object {
        /** Where the navigation entry puts the [GameRoute] for [Factory]. */
        val RouteKey: CreationExtras.Key<GameRoute> = object : CreationExtras.Key<GameRoute> {}

        /** The key, in the saved state of the entry, of the mark that its game has started. */
        private const val STARTED_KEY = "ch.magori.blox.feature.game.started"
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
