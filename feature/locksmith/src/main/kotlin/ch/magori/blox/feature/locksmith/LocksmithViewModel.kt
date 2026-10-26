package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

/**
 * The game of one [LocksmithRoute]: its padlock, generated from the seed of the route (ADR 0014, 0025), played by the
 * actions of the player, which the domain judges. The only writer of the state of the screen. A rank that is not given
 * is a programming error of the screen, refused by the domain.
 */
class LocksmithViewModel internal constructor(route: LocksmithRoute) : ViewModel() {
    private val state = MutableStateFlow(
        LocksmithUiState(level = route.level, game = PadlockGame.start(Padlock.generate(route.level, route.seed))),
    )

    /** What the screen shows. */
    internal val uiState: StateFlow<LocksmithUiState> = state.asStateFlow()

    /** Selects the free given piece of [rank]. */
    internal fun select(rank: Int): Unit = change { it.select(rank) }

    /** Turns the selected piece a quarter turn by [rotation]. */
    internal fun turn(rotation: Rotation): Unit = change { it.turn(rotation) }

    /** Places the selected piece, its pivot on [pivot]. */
    internal fun place(pivot: Position): Unit = judge { it.place(pivot) }

    /** Takes the placed piece of [rank] back. */
    internal fun takeBack(rank: Int): Unit = judge { it.takeBack(rank) }

    /** Asks for a hint, shown until the next placement accepted. */
    internal fun hint(): Unit = judge { it.hint() }

    /** Starts the same padlock over: nothing placed, no hint. */
    internal fun restart(): Unit = state.update { it.copy(game = it.game.restart(), events = emptyList(), hint = null) }

    /** An action that tells nothing to the screen but the next game. */
    private fun change(action: (PadlockGame) -> PadlockGame) = state.update { it.copy(game = action(it.game)) }

    /** An action the domain judges: the next game, the events it tells, and the hint they leave shown. */
    private fun judge(action: (PadlockGame) -> PadlockUpdate) = state.update { current ->
        val (game, events) = action(current.game)
        val hint = when (val first = events.firstOrNull()) {
            is PadlockEvent.HintGiven -> first.hint
            is PadlockEvent.PiecePlaced, is PadlockEvent.PieceTakenBack -> null
            else -> current.hint
        }
        current.copy(game = game, events = events, move = current.move + 1, hint = hint)
    }

    /**
     * Builds the view model of an entry of the Locksmith (ADR 0013). The route of the entry comes in the
     * creation extras under `RouteKey`, since only the navigation entry knows it.
     */
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            val route =
                requireNotNull(extras[RouteKey]) { "The route of the padlock is missing from the creation extras" }
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return LocksmithViewModel(route) as T
        }
    }

    internal companion object {
        /** The key of the route of the entry in the creation extras. */
        val RouteKey: CreationExtras.Key<LocksmithRoute> = object : CreationExtras.Key<LocksmithRoute> {}
    }
}
