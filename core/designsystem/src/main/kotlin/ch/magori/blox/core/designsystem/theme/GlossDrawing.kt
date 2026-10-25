package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Draws [gloss] over what is drawn before it, a fill in [shape], and under the content: put it after the background.
 * As in CSS, its inner shadows are drawn inside the [frame] of the shape, if it has one, and its ring around the
 * shape. Nothing is drawn without a gloss (Original).
 */
fun Modifier.gloss(
    gloss: Gloss,
    shape: Shape,
    frame: Dp = 0.dp,
): Modifier = if (gloss.layers.isEmpty()) {
    this
} else {
    drawWithCache {
        val bands = glossBands(gloss, shape, frame.toPx(), size, layoutDirection, this)
        onDrawBehind { bands.forEach { band -> drawPath(band.path, band.color) } }
    }
}

/** Where a layer of a gloss is painted, and in which colour. */
internal class GlossBand(val path: Path, val color: Color)

/**
 * The bands of [gloss] on [shape] at [size], inside its frame of [frame] pixels, each built once: they are drawn again
 * as they are on each frame.
 */
internal fun glossBands(
    gloss: Gloss,
    shape: Shape,
    frame: Float,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): List<GlossBand> {
    /** [shape] grown by [by] pixels on each side (shrunk when negative), at its place. */
    fun grown(by: Float): Path = Path().apply {
        addOutline(shape.createOutline(Size(size.width + 2 * by, size.height + 2 * by), layoutDirection, density))
        translate(Offset(-by, -by))
    }
    val outline = grown(0f)
    val inside = grown(-frame)
    return gloss.layers.map { layer ->
        when (layer) {
            is GlossLayer.Inner -> {
                // Inside the frame, less itself moved by the offset: a band along the top (down) or the bottom (up).
                val moved = Path().apply {
                    addPath(inside)
                    translate(Offset(0f, with(density) { layer.offsetY.toPx() }))
                }
                GlossBand(Path.combine(PathOperation.Difference, inside, moved), layer.color)
            }

            is GlossLayer.Ring -> {
                val ring = grown(with(density) { layer.width.toPx() })
                GlossBand(Path.combine(PathOperation.Difference, ring, outline), layer.color)
            }
        }
    }
}
