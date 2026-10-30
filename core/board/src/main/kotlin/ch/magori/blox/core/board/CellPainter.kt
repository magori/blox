package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import ch.magori.blox.core.designsystem.theme.BubblePainter
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
 * the ring of a gloss in the joint, and the dot and the cross, if any, over them all. A bubble is drawn by the
 * [BubblePainter] of the design system, as the notes of the music editor are. Cut to [width] pixels, it keeps its
 * height and its corners, its cross a square of [crossSide] in the middle of what is left.
 */
internal class CellPainter(
    style: CellStyle,
    side: Float,
    background: Color,
    density: Density,
    width: Float = side,
    crossSide: Float? = null,
) {
    private val layout = cellLayout(style, side, density, width)
    private val joint = style.joint?.let { background }
    private val cellSize = Size(width, side)
    private val bubble: BubblePainter? = style.bubble?.let {
        BubblePainter(style.fill, it.highlight, layout.shape, layout.gloss, layout.fill.size, density)
    }
    private val flatFill = style.fill
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
    private val cross: (DrawScope.() -> Unit)? = style.cross?.let { crossOver(it, layout.fill, side, crossSide) }
    private val dot: (DrawScope.() -> Unit)? = style.dot?.let { centreDot(it, side, layout.fill.center) }

    /** Draws the cell, with its joint unless [withJoint] is false: the background under it then shows around its fill. */
    fun draw(
        scope: DrawScope,
        withJoint: Boolean = true,
    ) {
        drawBody(scope, withJoint)
        drawMarks(scope)
    }

    /**
     * The cell without its marks: its joint, unless [withJoint] is false, its fill, its border, its centre and its
     * gloss.
     */
    fun drawBody(
        scope: DrawScope,
        withJoint: Boolean = true,
    ) {
        val fill = layout.fill
        if (withJoint) joint?.let { scope.drawRect(it, size = cellSize) }
        if (bubble == null) {
            scope.drawRect(flatFill, topLeft = fill.topLeft, size = fill.size)
        } else {
            scope.translate(fill.left, fill.top) { bubble.drawFill(this) }
        }
        border?.invoke(scope)
        centre?.invoke(scope)
        // The ring of the gloss fits in the margin of the cell (cellLayout): it never covers a cell beside it.
        bubble?.let { scope.translate(fill.left, fill.top) { it.drawGloss(this) } }
    }

    /** The marks of the cell, over its body and whatever is drawn on it: its dot and its cross. */
    fun drawMarks(scope: DrawScope) {
        dot?.invoke(scope)
        cross?.invoke(scope)
    }

    /** Covers the fill of the cell, in its shape, with [color] at [alpha]: the cell lit. */
    fun drawLight(
        scope: DrawScope,
        color: Color,
        alpha: Float,
    ) {
        val fill = layout.fill
        if (bubble == null) {
            scope.drawRect(color, topLeft = fill.topLeft, size = fill.size, alpha = alpha)
        } else {
            scope.translate(fill.left, fill.top) { bubble.drawCover(this, color, alpha) }
        }
    }
}

/** How far from the edges of the fill the arms of a cross end: a quarter of it, the cross in its middle half. */
private const val CROSS_MARGIN = 0.25f

/**
 * The two diagonals of [cross] across the middle of [fill], each drawn on its halo, in whole pixels: across its middle
 * half, or across a square of [crossSide] in its centre when the cell is cut.
 */
private fun crossOver(
    cross: CellCross,
    fill: Rect,
    side: Float,
    crossSide: Float?,
): DrawScope.() -> Unit {
    val width = pixelsOf(cross.width, side)
    val halo = crossHalo(cross, side)
    val half = (crossSide ?: wholeCross(fill.width)) / 2
    val topLeft = Offset(fill.center.x - half, fill.center.y - half)
    val bottomRight = Offset(fill.center.x + half, fill.center.y + half)
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

/** The width of the halo of [cross] in a cell of [side] pixels: twice its stroke. */
internal fun crossHalo(
    cross: CellCross,
    side: Float,
): Float = pixelsOf(cross.width, side) * 2

/** The side of the cross of a cell that is not cut, its fill [fill] pixels wide: across its middle half. */
internal fun wholeCross(fill: Float): Float = fill * (1 - 2 * CROSS_MARGIN)

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

/** The [dot] on the [centre] of the fill of a cell of [side] pixels, cut or not, on its halo. */
private fun centreDot(
    dot: CellDot,
    side: Float,
    centre: Offset,
): DrawScope.() -> Unit {
    val radius = pixelsOf(dot.size, side) / 2
    val haloRadius = dot.haloSize?.let { pixelsOf(it, side) / 2 } ?: (radius * DOT_HALO)
    return {
        drawCircle(dot.halo, radius = haloRadius, center = centre)
        drawCircle(dot.color, radius = radius, center = centre)
    }
}

/** How much wider than a dot its halo is, when its size is not given. */
private const val DOT_HALO = 1.5f
