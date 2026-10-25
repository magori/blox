package ch.magori.blox.core.board

import kotlin.math.roundToInt

/**
 * A thickness given as a [fraction] of the cell (border, bevel, centre square), drawn on a cell of
 * [cellSize] pixels: rounded to a whole number of pixels, and never thinner than one pixel.
 */
internal fun pixelsOf(
    fraction: Float,
    cellSize: Float,
): Float = (fraction * cellSize).roundToInt().coerceAtLeast(1).toFloat()
