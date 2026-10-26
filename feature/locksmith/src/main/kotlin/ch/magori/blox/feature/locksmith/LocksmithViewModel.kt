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
    internal fun select(rank: Int): Unit = arrange { it.copy(game = it.game.select(rank)) }

    /** Turns the selected piece a quarter turn by [rotation]. */
    internal fun turn(rotation: Rotation): Unit = arrange { it.copy(game = it.game.turn(rotation)) }

    /**
     * A touch of the padlock on [position]: on the preview, places it (refused over a placed piece, which the preview
     * hides); on a placed piece, takes it back as a preview where it lay; elsewhere, aims the selected piece there. The
     * ring of metal is not touched: the pivot of the preview stays inside it.
     */
    internal fun tap(position: Position) {
        if (!position.isInside) return
        val current = state.value
        val rank = current.game.rankAt(position)
        when {
            current.preview?.cells?.contains(position) == true -> placeAim()
            rank != null -> takeBack(rank)
            else -> arrange { it.copy(aim = position) }
        }
    }

    /** Places the preview, if there is one; it is gone once placed, kept where it is when refused. */
    internal fun placeAim() {
        state.value.aim?.let(::place)
    }

    /** Places the selected piece, its pivot on [pivot]. */
    internal fun place(pivot: Position): Unit = judge { it.place(pivot) }

    /** Takes the placed piece of [rank] back: it becomes the preview, where it lay. */
    internal fun takeBack(rank: Int) {
        val pivot = state.value.game.placed[rank]?.pivot ?: return
        judge(aimAfter = pivot) { it.takeBack(rank) }
    }

    /** Asks for a hint, shown until the next placement accepted. */
    internal fun hint(): Unit = judge { it.hint() }

    /** Starts the same padlock over: nothing placed, no hint. */
    internal fun restart(): Unit =
        state.update { it.copy(game = it.game.restart(), events = emptyList(), hint = null, aim = null) }

    /**
     * An action of the player that the domain does not judge (choose, turn, aim): the events of the last judged action
     * are forgotten, a refusal told no longer once the preview has changed, without counting a move. Nothing changes once
     * the padlock is open.
     */
    private fun arrange(action: (LocksmithUiState) -> LocksmithUiState) = state.update { current ->
        if (current.game.isOpen) current else action(current).copy(events = emptyList())
    }

    /**
     * An action the domain judges, in one update: the next game, the events it tells, and the hint they leave shown;
     * the preview is gone once a piece is placed, or put on [aimAfter]. An action the domain ignores (it tells no event)
     * changes nothing, the events of the last one kept.
     */
    private fun judge(
        aimAfter: Position? = null,
        action: (PadlockGame) -> PadlockUpdate,
    ) = state.update { current ->
        val (game, events) = action(current.game)
        if (events.isEmpty()) return@update current
        val hint = when (val first = events.first()) {
            is PadlockEvent.HintGiven -> first.hint
            is PadlockEvent.PiecePlaced, is PadlockEvent.PieceTakenBack -> null
            else -> current.hint
        }
        val aim = when {
            aimAfter != null -> aimAfter
            events.any { it is PadlockEvent.PiecePlaced } -> null
            else -> current.aim
        }
        current.copy(game = game, events = events, move = current.move + 1, hint = hint, aim = aim)
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
