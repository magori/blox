package ch.magori.blox.core.board.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.PieceType

/**
 * What a cell of the musical garden shows (docs/jardin-musical.md, « Écran »), drawn by [GardenCellCanvas]: free, a
 * note of a piece placed, or the preview of a piece. Where it lies, in a band or under the play head, belongs to the
 * screen too, and is given to the canvas apart: the same cell may lie anywhere.
 */
@Immutable
sealed interface GardenCell {
    /** A free cell. */
    data object Free : GardenCell

    /**
     * A note: a cell of a piece of [type] placed in the garden; [inChord] when its pitch is one of the chord of its
     * group of 4 eighths: it then bears a dot. Which notes are of the chord belongs to the screen state.
     */
    data class Note(val type: PieceType, val inChord: Boolean = false) : GardenCell

    /**
     * A cell of the piece of [type] shown before it is placed. The cross marks this cell, not the whole piece: a cell
     * that does not [fits], out of the grid or over a note, is crossed out, the others of the piece are not.
     */
    data class Preview(val type: PieceType, val fits: Boolean) : GardenCell
}

/**
 * Where a cell lies in the column the play head is on: the frame of the play head rounds its corners at the [TOP] and
 * at the [BOTTOM] of the column, and runs along both sides of the cells in the [MIDDLE].
 */
enum class GardenPlayHead { TOP, MIDDLE, BOTTOM }
