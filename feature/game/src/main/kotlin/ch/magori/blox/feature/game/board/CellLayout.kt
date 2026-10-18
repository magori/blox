package ch.magori.blox.feature.game.board

import androidx.compose.ui.geometry.Rect

/**
 * Where the parts of a cell go, in pixels of the cell, from its top-left corner: the [fill], and the
 * outer edge of the [border], drawn inside the fill. Around the fill, up to the edge of the cell, the
 * joint, if any.
 */
internal data class CellLayout(val fill: Rect, val border: Rect?)

/**
 * The parts of a cell of [side] pixels in [style]. The joint takes [CellJoint.width] of the cell on each
 * edge, 1 px at least: two cells side by side are apart by twice as much.
 */
internal fun cellLayout(
    style: CellStyle,
    side: Float,
): CellLayout {
    val inset = style.joint?.let { pixelsOf(it.width, side) } ?: 0f
    val fill = Rect(inset, inset, side - inset, side - inset)
    return CellLayout(fill = fill, border = style.border?.let { fill })
}
