package ch.magori.blox.core.board.garden

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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

/**
 * How a note of the garden is drawn on one frame, besides its look (ADR 0024): [GardenCellCanvas] reads it only while
 * drawing, so that an effect that plays draws the cell again without recomposing it. Its answers may change from one
 * frame to the next, read from states. At rest, as by default, the note is drawn as without effects.
 */
@Stable
interface GardenCellEffects {
    /** How much of the note is shown, over the free cell under it: 0 not at all, 1 wholly. */
    fun shown(): Float = 1f

    /** How far above its place the note is drawn, in cells: it falls there. */
    fun lift(): Float = 0f

    /** How large the note is drawn, around its centre: 1 as it is. */
    fun scale(): Float = 1f

    /** How much the note is lit, from 0 (not at all) to 1, its fill covered by the colour of the glow. */
    fun light(): Float = 0f
}
