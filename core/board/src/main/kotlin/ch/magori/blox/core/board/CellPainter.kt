package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/** What builds the painters of every cell, for a theme, a cell size in pixels and a background: [paintersFor]. */
internal typealias PaintersFactory = (
    theme: BloxThemeKind,
    cellSize: Float,
    background: Color,
) -> Map<Cell, CellPainter>

/** A painter for each cell that can be drawn on [background]: the empty cell and a cell of each piece. */
internal fun paintersFor(
    theme: BloxThemeKind,
    cellSize: Float,
    background: Color,
): Map<Cell, CellPainter> = (listOf(Cell.Empty) + PieceType.entries.map(Cell::Filled))
    .associateWith { cell -> CellPainter(cellStyle(theme, cell), cellSize, background) }

/**
 * One cell of [side] pixels in [style], drawn on [background], with its shapes built once from its
 * [cellLayout]; it draws at the origin. The joint, if any, takes the edge of the cell in the colour of
 * [background]; the fill and the border are drawn inside it.
 */
internal class CellPainter(style: CellStyle, side: Float, background: Color) {
    private val layout = cellLayout(style, side)
    private val joint = style.joint?.let { background }
    private val cellSize = Size(side, side)
    private val fill = style.fill
    private val border: (DrawScope.() -> Unit)? = layout.border?.let { edge ->
        when (val border = style.border) {
            is CellBorder.Plain -> plainBorder(border.color, pixelsOf(border.width, side), edge)
            is CellBorder.Bevel -> bevel(border, pixelsOf(border.width, side), edge)
            null -> null
        }
    }
    private val centre: (DrawScope.() -> Unit)? = style.centre?.let { centreSquare(it, side) }

    fun draw(scope: DrawScope) {
        joint?.let { scope.drawRect(it, size = cellSize) }
        scope.drawRect(fill, topLeft = layout.fill.topLeft, size = layout.fill.size)
        border?.invoke(scope)
        centre?.invoke(scope)
    }
}

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

/** Light trapezoids of [width] pixels on the top and left edges of [edge], dark ones on the bottom and right. */
private fun bevel(
    bevel: CellBorder.Bevel,
    width: Float,
    edge: Rect,
): DrawScope.() -> Unit {
    val (left, top, right, bottom) = listOf(edge.left, edge.top, edge.right, edge.bottom)
    val outer = listOf(Offset(left, top), Offset(right, top), Offset(right, bottom), Offset(left, bottom))
    val inner = listOf(
        Offset(left + width, top + width),
        Offset(right - width, top + width),
        Offset(right - width, bottom - width),
        Offset(left + width, bottom - width),
    )
    val topSide = trapezoid(outer[0], outer[1], inner[1], inner[0])
    val leftSide = trapezoid(outer[0], inner[0], inner[3], outer[3])
    val bottomSide = trapezoid(outer[3], inner[3], inner[2], outer[2])
    val rightSide = trapezoid(outer[1], outer[2], inner[2], inner[1])
    return {
        drawPath(topSide, bevel.light)
        drawPath(leftSide, bevel.light)
        drawPath(bottomSide, bevel.dark)
        drawPath(rightSide, bevel.dark)
    }
}

private fun trapezoid(
    first: Offset,
    second: Offset,
    third: Offset,
    fourth: Offset,
): Path = Path().apply {
    moveTo(first.x, first.y)
    lineTo(second.x, second.y)
    lineTo(third.x, third.y)
    lineTo(fourth.x, fourth.y)
    close()
}
