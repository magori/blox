package ch.magori.blox.core.board.lock

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
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

/**
 * A cell of a padlock at the side of the screen (the preview on the ring, in column 0 or 9), in pixels: [width] wide at
 * rest, as high as a whole cell, [narrowest] when the padlock shakes it towards the edge of the screen; its cross, when
 * cut, as [lockCutCross] sizes it, of the same size whatever is shown of the cell, or that of a whole cell when [cross] is
 * null. Which part is shown belongs to the screen ([LockCellShown]).
 */
@Immutable
data class LockCellCut(val width: Int, val narrowest: Int = width, val cross: LockCutCross? = null) {
    init {
        require(narrowest in 0..width) { "A cell of $width pixels cannot narrow to $narrowest." }
    }
}

/**
 * What is shown of a cell at the side of a padlock, from [start] to [end] whole pixels of its width at rest: as the
 * padlock shakes, the screen narrows it rather than let it come closer to the edge. Packed in a [Long]: read at each
 * frame, it allocates nothing.
 */
@JvmInline
value class LockCellSpan private constructor(private val packed: Long) {
    constructor(start: Int, end: Int) : this((start.toLong() shl Int.SIZE_BITS) or (end.toLong() and LOW_BITS))

    val start: Int get() = (packed shr Int.SIZE_BITS).toInt()

    val end: Int get() = packed.toInt()

    override fun toString(): String = "LockCellSpan(start=$start, end=$end)"
}

/** What is shown of a cell at the side of a padlock, read in the drawing alone, frame by frame. */
fun interface LockCellShown {
    fun span(): LockCellSpan
}

/** How bright a cell is lit, from 0 (not at all) to 1, read in the drawing alone, frame by frame. */
fun interface LockCellGlow {
    fun brightness(): Float
}

/** The light of a cell: as bright as its [glow] says, in [color]. */
@Immutable
class LockCellLight(val glow: LockCellGlow, val color: Color)

/** The lower half of a [Long]. */
private const val LOW_BITS = 0xFFFFFFFFL
