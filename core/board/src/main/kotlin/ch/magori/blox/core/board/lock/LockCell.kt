package ch.magori.blox.core.board.lock

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.PieceType

/**
 * What a cell of a padlock of the Locksmith shows (docs/regles-serrurier.md, « Écran »), drawn by [LockCellCanvas]:
 * its metal, a cell of its hole, empty or marked by a hint, a piece placed in it, or the preview of a piece.
 */
@Immutable
sealed interface LockCell {
    /** The metal of the padlock, around its hole. */
    data object Metal : LockCell

    /** A cell of the hole, empty. */
    data object Hole : LockCell

    /** A cell of the hole where the hint shows a piece fits (`Hint.Fit`), marked until the next placement. */
    data object Hint : LockCell

    /**
     * A cell of a piece of [type] placed in the hole; [hinted] when the hint names this piece as the one that blocks
     * (`Hint.Blocking`): it then bears the mark of a hint.
     */
    data class Placed(val type: PieceType, val hinted: Boolean = false) : LockCell

    /**
     * A cell of the piece of [type] shown before it is placed. The cross marks this cell, not the whole piece: a cell
     * that does not [fits], out of the hole or over a placed piece, is crossed out, the others of the piece are not.
     * Whether a cell fits belongs to the screen state: the domain judges the placement only.
     */
    data class Preview(val type: PieceType, val fits: Boolean) : LockCell
}
