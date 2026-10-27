package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame

/**
 * What the screen of the Locksmith shows: padlock [level], from 1, and its [game]; the [events] of the last action of
 * the player, for the feedback and the sounds, and [move], counting every action, so that two same events in a row
 * stay two feedbacks; the [hint] shown, until the next placement accepted, a piece taken back or a restart; the cell
 * the player [aim]s the selected piece at, its pivot there: the preview, a state of the screen that the domain does not
 * judge.
 */
internal data class LocksmithUiState(
    val level: Int,
    val game: PadlockGame,
    val events: List<PadlockEvent> = emptyList(),
    val move: Int = 0,
    val hint: Hint? = null,
    val aim: Position? = null,
) {
    /** The selected piece where the player aims it, its pivot on [aim]; null without an aim or a selected piece. */
    val preview: Piece?
        get() {
            return aim?.let(game::selectedAt)
        }
}
