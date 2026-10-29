package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.OpeningOutcome
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockResult

/**
 * What the screen of the Locksmith shows: padlock [level], from 1, and its [game]; the [events] of the last action of
 * the player, for the feedback and the sounds, and [move], counting every action, so that two same events in a row
 * stay two feedbacks; the [hint] shown, until the next placement accepted, a piece taken back or a restart; the cell
 * the player [aim]s the selected piece at, its pivot there: the preview, a state of the screen that the domain does not
 * judge; whether the last action [turned] the selected piece, which the message then tells.
 *
 * Immutable for Compose: the game and its events are value objects (compose-stability.conf), the list of [events]
 * never changed once built.
 */
@Immutable
internal data class LocksmithUiState(
    val level: Int,
    val game: PadlockGame,
    val events: List<PadlockEvent> = emptyList(),
    val move: Int = 0,
    val hint: Hint? = null,
    val aim: Position? = null,
    val turned: Boolean = false,
    val unrecorded: Boolean = false,
    val outcome: OpeningOutcome? = null,
    val best: PadlockResult? = null,
    val frontier: Int? = null,
) {
    /** The selected piece where the player aims it, its pivot on [aim]; null without an aim or a selected piece. */
    val preview: Piece?
        get() {
            return aim?.let(game::selectedAt)
        }
}
