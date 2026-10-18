package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece

/**
 * Entry delay of the Classic mode (NES, 1989): 10 ticks when the piece locks in the two bottom
 * rows, then 2 more ticks for every group of 4 rows higher, up to 18 ticks.
 *
 * The height is the row of the pivot of the locked piece. That is a simplification: it is probably
 * what the ROM does, but that is not confirmed.
 */
public object ClassicEntryDelay : EntryDelay {
    /** @throws IllegalArgumentException if the pivot of [lockedPiece] is not in [Board.ROWS_WITH_HIDDEN]. */
    override fun ticksAfterLock(lockedPiece: Piece): Int {
        val pivotRow = lockedPiece.pivot.row
        require(pivotRow in Board.ROWS_WITH_HIDDEN) {
            "The pivot row must be in ${Board.ROWS_WITH_HIDDEN}: $pivotRow"
        }
        return when (pivotRow) {
            in 18..19 -> 10
            in 14..17 -> 12
            in 10..13 -> 14
            in 6..9 -> 16
            else -> 18
        }
    }
}
