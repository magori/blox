package ch.magori.blox.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * The game of one [GameRoute]: one engine, created for this game from its seed (ADR 0014), started
 * at its level. The only writer of the game state (ADR 0012): the screen gives it the frames, it
 * turns them into ticks of the engine.
 */
class GameViewModel internal constructor(
    engineFactory: GameEngineFactory,
    route: GameRoute,
    private val rotationSystem: RotationSystem,
) : ViewModel() {
    private val engine = engineFactory.create(route.seed)
    private val loop = GameLoop()

    /** The last state the engine returned: the only one it is given back (ADR 0014). */
    private var gameState: GameState = engine.start(Level(route.startLevel)).state
    private val state = MutableStateFlow(gameState.toUiState(rotationSystem))

    internal val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /**
     * The screen starts drawing frames again (first display, back from another screen or from the
     * background): the time spent away is not played; the next frame only sets the time reference.
     */
    internal fun onFramesStarted() {
        loop.reset()
    }

    /** A frame was drawn at [frameNanos]: the engine plays the ticks due since the previous frame. */
    internal fun onFrame(frameNanos: Long) {
        val ticks = loop.advance(frameNanos)
        if (ticks == 0) return
        repeat(ticks) { gameState = engine.tick(gameState).state }
        state.value = gameState.toUiState(rotationSystem)
    }

    /**
     * Builds the view model of a game. The feature defines it, `app` builds it; the route comes in the
     * [CreationExtras] under [RouteKey], since only the navigation entry knows it (ADR 0013).
     */
    class Factory(private val engineFactory: GameEngineFactory, private val rotationSystem: RotationSystem) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            val route = requireNotNull(extras[RouteKey]) { "The game route is missing from the creation extras" }
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(engineFactory, route, rotationSystem) as T
        }
    }

    companion object {
        /** Where the navigation entry puts the [GameRoute] for [Factory]. */
        val RouteKey: CreationExtras.Key<GameRoute> = object : CreationExtras.Key<GameRoute> {}
    }
}
