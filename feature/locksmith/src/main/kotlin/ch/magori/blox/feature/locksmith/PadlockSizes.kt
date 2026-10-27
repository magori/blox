package ch.magori.blox.feature.locksmith

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.lock.LockCellCut
import ch.magori.blox.core.board.lock.LockCellSpan
import ch.magori.blox.core.board.lock.LockCutCross
import ch.magori.blox.core.board.lock.lockCutCross
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** The side of a [cell] of the padlock and the thickness of its [rim] of metal, in whole pixels. */
internal data class PadlockSizes(val cell: Int, val rim: Int) {
    /**
     * How far a cell of the preview on the ring, drawn whole from the inner edge of the rim, passes its outer edge: the
     * room the screen keeps above and below the padlock, so that such a cell covers neither the chords nor the message.
     */
    val overflow: Int get() = cell - rim

    /**
     * The room kept above and below the padlock, past the [gap] to its neighbours, which takes its share of the
     * [overflow]; none once the padlock is [open], since no preview is left to overflow.
     */
    fun room(
        gap: Int,
        open: Boolean,
    ): Int = if (open) 0 else (overflow - gap).coerceAtLeast(0)

    /**
     * How many pixels are left between the outer edge of the rim and the edge of the screen, the padlock centred in a
     * column of [width] pixels, [sideMargin] pixels between that column and each edge of the screen.
     */
    fun beyond(
        width: Int,
        sideMargin: Int,
    ): Int = (width - (INSIDE.count() * cell + 2 * rim)) / 2 + sideMargin

    /**
     * A cell of the preview on the ring at the side of the padlock, [beyond] pixels left between the outer edge of the rim
     * and the edge of the screen: whole when it passes the rim by its [overflow] [margin] pixels at least from that edge;
     * otherwise cut there, its inner part kept under the rim. Its cross, at [density], as large as its fill holds it
     * with its halo ([lockCutCross]); where it would fall under [MIN_CROSS], the cell is cut a little closer to the edge,
     * [MIN_SIDE_MARGIN] from it at the closest, rounded up to a whole pixel, rather than its stroke thinned. Where even
     * there its cross would fall under [MIN_CROSS], on a screen narrower than any the Locksmith is made for, the cell is
     * cut there all the same, its cross as large as its fill then holds it, its stroke kept.
     */
    fun sideCell(
        beyond: Int,
        margin: Int,
        density: Density,
    ): SideCell {
        val least = with(density) { MIN_CROSS.toPx() }
        val closest = minOf(margin, ceil(with(density) { MIN_SIDE_MARGIN.toPx() }).toInt())
        val cells = (margin downTo closest).map { left ->
            val width = if (beyond - left >= overflow) cell else rim + beyond - left
            val gap = beyond - (width - rim)
            SideCell(
                width = width,
                cross = lockCutCross(cell.toFloat(), width.toFloat(), density),
                whole = width == cell,
                slack = (gap - margin).coerceAtLeast(0),
            )
        }
        return cells.firstOrNull { it.cross.side >= least } ?: cells.last()
    }
}

/**
 * The sizes of the padlock in [width] pixels: [INSIDE] cells and a rim of a quarter of a cell on each side fit it, each
 * cell [maxCell] pixels at most. Whole pixels, so that the cells leave no seam between them on any density; the rim,
 * rounded, may take a pixel more than its quarter, and the cell then shrinks by one so that the whole still fits.
 */
internal fun padlockSizes(
    width: Float,
    maxCell: Float,
): PadlockSizes {
    val cells = INSIDE.count()
    val largest = floor(minOf(maxCell, width / (cells + 2 * RIM_PART))).toInt()
    return generateSequence(largest) { it - 1 }
        .map { cell -> PadlockSizes(cell = cell, rim = (cell * RIM_PART).roundToInt()) }
        .first { cells * it.cell + 2 * it.rim <= width }
}

/**
 * A cell of the preview on the ring at the side of the padlock, as wide as [width] pixels from the inner edge of the rim
 * at rest, [whole] or cut, its [cross] as large as its fill holds it; [slack] pixels may part its outer edge from the
 * edge of the screen before it comes closer to it than its margin, or than at rest when it gave up some of it for its
 * cross.
 */
internal data class SideCell(val width: Int, val cross: LockCutCross, val whole: Boolean, val slack: Int = 0) {
    /**
     * What is shown of this cell, [onTheLeft] of the padlock or on its right, the padlock shaken by [shift] pixels (to the
     * right when positive), in whole pixels of the cell at rest: its inner edge follows the padlock; its outer edge too,
     * but never past its [slack], the pixels it would pass taken off whole, so that it never comes closer to the edge
     * of the screen than at rest, even by a fraction of a pixel.
     */
    fun shownAt(
        shift: Float,
        onTheLeft: Boolean,
    ): LockCellSpan = if (onTheLeft) {
        LockCellSpan(start = trimmed(-shift), end = width)
    } else {
        LockCellSpan(start = 0, end = width - trimmed(shift))
    }

    /** The narrowest this cell is shown while the padlock shakes [amplitude] pixels to each side at most. */
    fun narrowest(amplitude: Float): Int = width - trimmed(amplitude)

    /** This cell for the canvas, shaken [amplitude] pixels at most: whole, it keeps the cross of a whole cell. */
    fun cut(amplitude: Float): LockCellCut =
        LockCellCut(width = width, narrowest = narrowest(amplitude), cross = cross.takeUnless { whole })

    /** The whole pixels taken off this cell when the padlock moves [towardsTheEdge] pixels towards its edge. */
    private fun trimmed(towardsTheEdge: Float): Int = ceil(towardsTheEdge - slack).toInt().coerceIn(0, width)
}

/** The rim of metal around the cells, in parts of a cell: thin, the ring of the padlock no longer drawn as cells. */
private const val RIM_PART = 0.25f

/** The smallest cross of a cell at the side: under it, the cell is cut closer to the edge of the screen. */
private val MIN_CROSS = 10.dp

/** The closest a cell at the side comes to the edge of the screen to keep its cross at [MIN_CROSS]. */
private val MIN_SIDE_MARGIN = 3.dp
