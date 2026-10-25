package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.boundingRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Draws [gloss] over what is drawn before it, a fill in [shape], and under the content: put it after the background.
 * As in CSS, its inner shadows are drawn inside the [frame] of the shape, if it has one, and its ring around the
 * shape. Nothing is drawn without a gloss (Original). Its bands are built once, and again only when the size, the
 * density, the direction of reading or one of these values change.
 */
fun Modifier.gloss(
    gloss: Gloss,
    shape: Shape,
    frame: Dp = 0.dp,
): Modifier = if (gloss.layers.isEmpty()) this else this then GlossElement(gloss, shape, frame)

/** The gloss of a modifier: equal to another for the same values, so that a recomposition keeps its node as it is. */
private data class GlossElement(val gloss: Gloss, val shape: Shape, val frame: Dp) : ModifierNodeElement<GlossNode>() {
    override fun create(): GlossNode = GlossNode(gloss, shape, frame)

    override fun update(node: GlossNode) = node.update(gloss, shape, frame)

    override fun InspectorInfo.inspectableProperties() {
        name = "gloss"
        properties["gloss"] = gloss
        properties["shape"] = shape
        properties["frame"] = frame
    }
}

private class GlossNode(private var gloss: Gloss, private var shape: Shape, private var frame: Dp) :
    Modifier.Node(),
    DrawModifierNode {
    private val bands = GlossCache { key -> GlossBands(bandsOf(key)) }

    fun update(
        gloss: Gloss,
        shape: Shape,
        frame: Dp,
    ) {
        this.gloss = gloss
        this.shape = shape
        this.frame = frame
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val key = GlossKey(gloss, shape, frame.toPx(), size, layoutDirection, Density(density, fontScale))
        bands.get(key).draw(this)
        drawContent()
    }
}

/** What the bands of a gloss are built from: [frame] in pixels. */
internal data class GlossKey(
    val gloss: Gloss,
    val shape: Shape,
    val frame: Float,
    val size: Size,
    val layoutDirection: LayoutDirection,
    val density: Density,
)

/** What [build] gives for a [GlossKey], kept until it is asked for another key. */
internal class GlossCache<T>(private val build: (GlossKey) -> T) {
    private var built: Pair<GlossKey, T>? = null

    fun get(key: GlossKey): T {
        val kept = built?.takeIf { it.first == key } ?: (key to build(key)).also { built = it }
        return kept.second
    }
}

/**
 * The bands of [gloss] on [shape] at [size], without a frame, to draw over a fill of that shape and size, from its
 * top-left corner. For a painter that keeps its shapes and draws many of them, as the cells of the board (core:board):
 * built once, they are drawn without allocating anything; a modifier per cell would be too many nodes.
 */
fun glossBands(
    gloss: Gloss,
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): GlossBands = GlossBands(bandsOf(GlossKey(gloss, shape, frame = 0f, size, layoutDirection, density)))

/** The bands of a gloss, built once ([glossBands]): drawing them allocates nothing. */
class GlossBands internal constructor(private val bands: List<GlossBand>) {
    /** Draws the bands in [scope], over what is drawn before them. */
    fun draw(scope: DrawScope) {
        for (index in bands.indices) scope.drawPath(bands[index].path, bands[index].color)
    }
}

/** Where a layer of a gloss is painted, and in which colour. */
internal class GlossBand(val path: Path, val color: Color)

/** The bands of a gloss for [key]: inside its frame for its inner shadows, around its shape for its ring. */
private fun bandsOf(key: GlossKey): List<GlossBand> {
    val (gloss, shape, frame, size, layoutDirection, density) = key

    fun grown(by: Float): Path = Path().apply { addOutline(grownOutline(shape, size, by, layoutDirection, density)) }
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

/**
 * The outline of [shape] at [size] grown by [by] pixels on each side (shrunk when negative), at its place. As in CSS,
 * a rounded corner grows with it, or shrinks down to square: the inside of a frame follows its inner edge, a ring is
 * as round as its shape. Any other outline is the shape drawn larger or smaller.
 */
internal fun grownOutline(
    shape: Shape,
    size: Size,
    by: Float,
    layoutDirection: LayoutDirection,
    density: Density,
): Outline = when (val outline = shape.createOutline(size, layoutDirection, density)) {
    is Outline.Rectangle -> Outline.Rectangle(outline.rect.inflate(by))

    is Outline.Rounded -> Outline.Rounded(outline.roundRect.grown(by))

    is Outline.Generic -> {
        val resized = shape.createOutline(Size(size.width + 2 * by, size.height + 2 * by), layoutDirection, density)
        Outline.Generic(Path().apply { addOutline(resized) }.apply { translate(Offset(-by, -by)) })
    }
}

/** This rectangle grown by [by] on each side, each round corner grown by [by] too, down to square at least. */
private fun RoundRect.grown(by: Float): RoundRect {
    fun CornerRadius.grown() =
        if (x > 0f && y > 0f) CornerRadius((x + by).coerceAtLeast(0f), (y + by).coerceAtLeast(0f)) else this
    val rect = boundingRect.inflate(by)
    return RoundRect(
        rect = rect,
        topLeft = topLeftCornerRadius.grown(),
        topRight = topRightCornerRadius.grown(),
        bottomRight = bottomRightCornerRadius.grown(),
        bottomLeft = bottomLeftCornerRadius.grown(),
    )
}
