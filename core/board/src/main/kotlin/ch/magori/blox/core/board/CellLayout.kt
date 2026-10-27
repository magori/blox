package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.bubbleLook
import kotlin.math.ceil

/**
 * Where the parts of a cell go, in pixels of the cell, from its top-left corner: the [fill], in [shape], and the
 * outer edge of the [border], drawn inside the fill; the [gloss] over the fill. Around the fill, up to the edge of
 * the cell, the joint, if any.
 */
internal data class CellLayout(val fill: Rect, val border: Rect?, val shape: Shape, val gloss: Gloss)

/**
 * The parts of a cell of [side] pixels in [style], at [density], cut to [width] pixels when narrower (a cell of the
 * Locksmith at the edge of the screen), as high and as round. The joint takes its width on each edge, in whole
 * pixels, 1 px at least: two cells side by side are apart by twice as much. A bubble has the look of [bubbleLook] at
 * the size of the cell, joint included: the corners of `radius-cell` and its gloss from 16 dp; below, corners of 2 dp
 * and no gloss (docs/design-system/components/Piece).
 */
internal fun cellLayout(
    style: CellStyle,
    side: Float,
    density: Density,
    width: Float = side,
): CellLayout {
    val inset = when (val joint = style.joint) {
        null -> 0f

        is CellJoint.Fraction -> pixelsOf(joint.width, side)

        // Rounded up: the ring of a gloss, as wide, fits in it at every density.
        is CellJoint.Fixed -> ceil(with(density) { joint.width.toPx() }).coerceAtLeast(1f)
    }
    val fill = Rect(inset, inset, width - inset, side - inset)
    val look = style.bubble?.let { bubbleLook(with(density) { side.toDp() }, it.gloss) }
    return CellLayout(
        fill = fill,
        border = style.border?.let { fill },
        shape = look?.shape ?: RectangleShape,
        gloss = look?.gloss ?: Gloss.None,
    )
}
