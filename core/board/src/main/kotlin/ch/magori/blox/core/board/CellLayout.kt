package ch.magori.blox.core.board

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import ch.magori.blox.core.designsystem.theme.Gloss
import kotlin.math.ceil

/**
 * Where the parts of a cell go, in pixels of the cell, from its top-left corner: the [fill], in [shape], and the
 * outer edge of the [border], drawn inside the fill; the [gloss] over the fill. Around the fill, up to the edge of
 * the cell, the joint, if any.
 */
internal data class CellLayout(val fill: Rect, val border: Rect?, val shape: Shape, val gloss: Gloss)

/**
 * The parts of a cell of [side] pixels in [style], at [density]. The joint takes its width on each edge, in whole
 * pixels, 1 px at least: two cells side by side are apart by twice as much. A bubble has the corners of `radius-cell`
 * and its gloss from 16 dp; below, corners of 2 dp and no gloss, which would weigh too much on so small a cell
 * (docs/design-system/components/Piece).
 */
internal fun cellLayout(
    style: CellStyle,
    side: Float,
    density: Density,
): CellLayout {
    val inset = when (val joint = style.joint) {
        null -> 0f

        is CellJoint.Fraction -> pixelsOf(joint.width, side)

        // Rounded up: the ring of a gloss, as wide, fits in it at every density.
        is CellJoint.Fixed -> ceil(with(density) { joint.width.toPx() }).coerceAtLeast(1f)
    }
    val fill = Rect(inset, inset, side - inset, side - inset)
    val bubble = style.bubble
    val large = with(density) { side.toDp() } >= LARGE_CELL
    return CellLayout(
        fill = fill,
        border = style.border?.let { fill },
        shape = when {
            bubble == null -> RectangleShape
            large -> BloxShapeSets.Colorful.cell
            else -> SMALL_CELL_CORNERS
        },
        gloss = bubble?.gloss?.takeIf { large } ?: Gloss.None,
    )
}

/** From this size, a bubble has its corners of `radius-cell` and its gloss. */
private val LARGE_CELL = 16.dp
private val SMALL_CELL_CORNERS = RoundedCornerShape(2.dp)
