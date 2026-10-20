package ch.magori.blox.core.designsystem.icon

import androidx.compose.ui.unit.IntRect

/** The side of a pixel drawing, in its pixels. */
internal const val PIXEL_GRID = 8

/** A run of full pixels on a line of a pixel drawing. */
private val FullRun = Regex("#+")

/**
 * The rectangles of the full pixels (`#`) of [rows], drawn in a box [sizePx] device pixels wide: each pixel as many
 * whole device pixels as fit eight times in the box, so that no edge is blurred, the drawing centred to the device
 * pixel. The full pixels in a row on a line make one rectangle.
 */
internal fun pixelRects(
    rows: List<String>,
    sizePx: Int,
): List<IntRect> {
    val cell = sizePx / PIXEL_GRID
    val margin = (sizePx - cell * PIXEL_GRID) / 2
    return rows.flatMapIndexed { y, row ->
        FullRun.findAll(row).map { it.range }.map { run ->
            IntRect(
                left = margin + run.first * cell,
                top = margin + y * cell,
                right = margin + (run.last + 1) * cell,
                bottom = margin + (y + 1) * cell,
            )
        }.toList()
    }
}
