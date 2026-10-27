package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The corners and the relief of a bubble at its size ([bubbleLook]). */
@Immutable
data class BubbleLook(val shape: Shape, val gloss: Gloss)

/**
 * The look of a bubble (docs/design-system/components/Piece) whose smaller side is [side]: from 16 dp, the corners of
 * `radius-cell` and its [gloss]; below, corners of 2 dp and no gloss, which would weigh too much on so small a bubble.
 * The cells of the board (core:board) and the notes of the music editor (feature:composer) are such bubbles.
 */
fun bubbleLook(
    side: Dp,
    gloss: Gloss,
): BubbleLook = if (side >= LARGE_BUBBLE) {
    BubbleLook(BloxShapeSets.Colorful.cell, gloss)
} else {
    BubbleLook(SMALL_BUBBLE_CORNERS, Gloss.None)
}

/** From this size, a bubble has its corners of `radius-cell` and its gloss. */
private val LARGE_BUBBLE = 16.dp
private val SMALL_BUBBLE_CORNERS = RoundedCornerShape(2.dp)

/**
 * A bubble of [size] pixels in [shape], at [density]: its fill lit from the top as in the design system, from
 * [highlight] at the top to [fill] at 55 % of its height, or flat in [fill] without a highlight; its [gloss] over it.
 * Its path, its gradient and its gloss are built once: it draws at the origin, without allocating anything, so that a
 * painter of many cells or notes keeps one per size.
 */
class BubblePainter(fill: Color, highlight: Color?, shape: Shape, gloss: Gloss, size: Size, density: Density) {
    private val path = Path().apply { addOutline(shape.createOutline(size, LayoutDirection.Ltr, density)) }
    private val brush: Brush? = highlight?.let { bubbleGradient(it, fill, size.height) }
    private val color = fill
    private val bands: GlossBands? = gloss.takeIf { it.layers.isNotEmpty() }?.let {
        glossBands(it, shape, size, LayoutDirection.Ltr, density)
    }

    /** Draws the fill in [scope]. */
    fun drawFill(scope: DrawScope) {
        if (brush == null) scope.drawPath(path, color) else scope.drawPath(path, brush)
    }

    /** Draws the gloss in [scope], over what is drawn before it; its ring, if any, goes around the bubble. */
    fun drawGloss(scope: DrawScope) {
        bands?.draw(scope)
    }

    /** Draws the fill, then the gloss, in [scope]. */
    fun draw(scope: DrawScope) {
        drawFill(scope)
        drawGloss(scope)
    }
}

/**
 * The fill of a bubble [height] pixels high, from its top: [highlight] at the top, [fill] from 55 % of its height down
 * (components/Piece).
 */
internal fun bubbleGradient(
    highlight: Color,
    fill: Color,
    height: Float,
): Brush = Brush.verticalGradient(0f to highlight, HIGHLIGHT_END to fill, startY = 0f, endY = height)

/** Where the gradient of a bubble reaches its colour: 55 % of its height (components/Piece). */
private const val HIGHLIGHT_END = 0.55f
