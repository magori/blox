package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import ch.magori.blox.core.designsystem.theme.grownOutline

/** Draws [ring], if any, around [shape], off its edge: drawn outside the bounds, it takes no room. Built once a size. */
internal fun Modifier.focusRing(
    ring: FocusRing?,
    shape: Shape,
): Modifier = if (ring == null) {
    this
} else {
    drawWithCache {
        val width = ring.width.toPx()
        val outline = grownOutline(shape, size, ring.gap.toPx() + width / 2, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        val stroke = Stroke(width)
        onDrawBehind { drawPath(path, ring.color, style = stroke) }
    }
}
