package ch.magori.blox.feature.game.topbar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt

/** The glyph is cut in eighths: a bar is 2 eighths wide, so are the gap and the outer margins together. */
private const val EIGHTHS = 8

/**
 * The two upright bars of the pause glyph in a space of [size]: each a quarter of the width, a
 * quarter apart, centred; an eighth of the height above and below. Every edge is rounded to a whole
 * pixel, as the cells of the board are, so the glyph stays sharp. Drawn, not a Unicode character,
 * so it looks the same on every device.
 */
internal fun pauseBars(size: Size): List<Rect> {
    fun across(eighths: Int) = pixel(size.width * eighths / EIGHTHS)
    val top = pixel(size.height / EIGHTHS)
    val bottom = pixel(size.height * (EIGHTHS - 1) / EIGHTHS)
    return listOf(
        Rect(left = across(1), top = top, right = across(3), bottom = bottom),
        Rect(left = across(5), top = top, right = across(7), bottom = bottom),
    )
}

private fun pixel(value: Float): Float = value.roundToInt().toFloat()
