package ch.magori.blox.feature.locksmith

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

/** The rim of metal around the cells, in parts of a cell: thin, the ring of the padlock no longer drawn as cells. */
private const val RIM_PART = 0.25f
