package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.GlossBands
import ch.magori.blox.core.designsystem.theme.glossBands
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/** What builds the painters of every cell, for a variant, a cell size in pixels, a background and a density. */
internal typealias PaintersFactory = (
    variant: BloxThemeVariant,
    cellSize: Float,
    background: Color,
    density: Density,
) -> Map<Cell, CellPainter>

/** A painter for each cell that can be drawn on [background]: the empty cell and a cell of each piece. */
internal fun paintersFor(
    variant: BloxThemeVariant,
    cellSize: Float,
    background: Color,
    density: Density,
): Map<Cell, CellPainter> = (listOf(Cell.Empty) + PieceType.entries.map(Cell::Filled))
    .associateWith { cell -> CellPainter(cellStyle(variant, cell), cellSize, background, density) }

/**
 * One cell of [side] pixels in [style], drawn on [background], at [density], with its shapes, its gradient and its
 * gloss built once from its [cellLayout]; it draws at the origin, without allocating anything. The joint, if any,
 * takes the edge of the cell in the colour of [background]; the fill, the border and the gloss are drawn inside it,
 * the ring of a gloss in the joint, and the dot and the cross, if any, over them all.
 */
internal class CellPainter(style: CellStyle, side: Float, background: Color, density: Density) {
    private val layout = cellLayout(style, side, density)
    private val joint = style.joint?.let { background }
    private val cellSize = Size(side, side)
    private val fillPath: Path? = if (layout.shape == RectangleShape) null else outlinePath(layout, density)
    private val fill: DrawScope.() -> Unit = fillOf(style, layout.fill, fillPath)
    private val border: (DrawScope.() -> Unit)? = layout.border?.let { edge ->
        style.border?.let { border ->
            val width = pixelsOf(border.width, side)
            if (layout.shape == RectangleShape) {
                plainBorder(border.color, width, edge)
            } else {
                roundedBorder(border.color, width, edge, layout.shape, density)
            }
        }
    }
    private val centre: (DrawScope.() -> Unit)? = style.centre?.let { centreSquare(it, side) }
    private val gloss: GlossBands? = layout.gloss.takeIf { it.layers.isNotEmpty() }?.let {
        glossBands(it, layout.shape, layout.fill.size, LayoutDirection.Ltr, density)
    }
    private val cross: (DrawScope.() -> Unit)? = style.cross?.let { crossOver(it, layout.fill, side) }
    private val dot: (DrawScope.() -> Unit)? = style.dot?.let { centreDot(it, side) }

    /** Draws the cell, with its joint unless [withJoint] is false: the background under it then shows around its fill. */
    fun draw(
        scope: DrawScope,
        withJoint: Boolean = true,
    ) {
        if (withJoint) joint?.let { scope.drawRect(it, size = cellSize) }
        fill(scope)
        border?.invoke(scope)
        centre?.invoke(scope)
        // The ring of the gloss fits in the margin of the cell (cellLayout): it never covers a cell beside it.
        gloss?.let { bands -> scope.translate(layout.fill.left, layout.fill.top) { bands.draw(this) } }
        dot?.invoke(scope)
        cross?.invoke(scope)
    }

    /** Covers the fill of the cell, in its shape, with [color] at [alpha]: the cell lit. */
    fun drawLight(
        scope: DrawScope,
        color: Color,
        alpha: Float,
    ) {
        val path = fillPath
        if (path == null) {
            scope.drawRect(color, topLeft = layout.fill.topLeft, size = layout.fill.size, alpha = alpha)
        } else {
            scope.drawPath(path, color, alpha = alpha)
        }
    }
}

/** How far from the edges of the fill the arms of a cross end: a quarter of it, the cross in its middle half. */
private const val CROSS_MARGIN = 0.25f

/** The two diagonals of [cross] across the middle of [fill], each drawn on its halo, in whole pixels. */
private fun crossOver(
    cross: CellCross,
    fill: Rect,
    side: Float,
): DrawScope.() -> Unit {
    val width = pixelsOf(cross.width, side)
    val halo = width * 2
    val margin = fill.width * CROSS_MARGIN
    val topLeft = Offset(fill.left + margin, fill.top + margin)
    val bottomRight = Offset(fill.right - margin, fill.bottom - margin)
    val topRight = Offset(bottomRight.x, topLeft.y)
    val bottomLeft = Offset(topLeft.x, bottomRight.y)
    // The lines written out: a lambda taking a colour and a width would box them at each frame.
    return {
        drawLine(cross.halo, topLeft, bottomRight, strokeWidth = halo, cap = StrokeCap.Round)
        drawLine(cross.halo, bottomLeft, topRight, strokeWidth = halo, cap = StrokeCap.Round)
        drawLine(cross.color, topLeft, bottomRight, strokeWidth = width, cap = StrokeCap.Round)
        drawLine(cross.color, bottomLeft, topRight, strokeWidth = width, cap = StrokeCap.Round)
    }
}

/**
 * The fill of a cell: a square of its colour; or, for a bubble, its rounded shape, lit from the top as in the design
 * system, from the highlight at the top to the fill at 55 % of its height. [rect] is the fill, [path] its outline for a
 * bubble, null for a square.
 */
private fun fillOf(
    style: CellStyle,
    rect: Rect,
    path: Path?,
): DrawScope.() -> Unit {
    if (path == null) return { drawRect(style.fill, topLeft = rect.topLeft, size = rect.size) }
    val highlight = style.bubble?.highlight ?: return { drawPath(path, style.fill) }
    val brush = Brush.verticalGradient(
        0f to highlight,
        HIGHLIGHT_END to style.fill,
        startY = rect.top,
        endY = rect.bottom,
    )
    return { drawPath(path, brush) }
}

/** The outline of the fill of [layout], in its rounded shape, at its place in the cell. */
private fun outlinePath(
    layout: CellLayout,
    density: Density,
): Path = Path().apply {
    addOutline(layout.shape.createOutline(layout.fill.size, LayoutDirection.Ltr, density))
    translate(layout.fill.topLeft)
}

/** Where the gradient of a bubble reaches the colour of its piece: 55 % of its height (components/Piece). */
private const val HIGHLIGHT_END = 0.55f

/** The centre square, in whole pixels. */
private fun centreSquare(
    centre: CellCentre,
    side: Float,
): DrawScope.() -> Unit {
    val centreSide = pixelsOf(centre.size, side)
    val inset = ((side - centreSide) / 2).toInt().toFloat()
    val topLeft = Offset(inset, inset)
    val size = Size(centreSide, centreSide)
    return { drawRect(centre.color, topLeft = topLeft, size = size) }
}

/** A border of [width] pixels inside [edge], its outer edge. */
private fun plainBorder(
    color: Color,
    width: Float,
    edge: Rect,
): DrawScope.() -> Unit {
    val stroke = Stroke(width)
    val topLeft = edge.topLeft + Offset(width / 2, width / 2)
    val size = Size(edge.width - width, edge.height - width)
    return { drawRect(color, topLeft = topLeft, size = size, style = stroke) }
}

/** A border of [width] pixels inside [edge], its outer edge, along the rounded corners of [shape]. */
private fun roundedBorder(
    color: Color,
    width: Float,
    edge: Rect,
    shape: Shape,
    density: Density,
): DrawScope.() -> Unit {
    val stroke = Stroke(width)
    val path = Path().apply {
        addOutline(shape.createOutline(Size(edge.width - width, edge.height - width), LayoutDirection.Ltr, density))
        translate(edge.topLeft + Offset(width / 2, width / 2))
    }
    return { drawPath(path, color, style = stroke) }
}

/** The [dot] in the centre of a cell of [side] pixels, on its halo, half as wide again. */
private fun centreDot(
    dot: CellDot,
    side: Float,
): DrawScope.() -> Unit {
    val radius = pixelsOf(dot.size, side) / 2
    val haloRadius = radius * DOT_HALO
    val centre = Offset(side / 2, side / 2)
    return {
        drawCircle(dot.halo, radius = haloRadius, center = centre)
        drawCircle(dot.color, radius = radius, center = centre)
    }
}

/** How much wider than a dot its halo is. */
private const val DOT_HALO = 1.5f
