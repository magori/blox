package ch.magori.blox.feature.game.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/** A painter for each cell that can be drawn: the empty cell and a cell of each piece. */
internal fun paintersFor(
    theme: BloxThemeKind,
    cellSize: Float,
): Map<Cell, CellPainter> = (listOf(Cell.Empty) + PieceType.entries.map(Cell::Filled))
    .associateWith { cell -> CellPainter(cellStyle(theme, cell), cellSize) }

/** One cell of [side] pixels in [style], with its shapes built once; it draws at the origin. */
internal class CellPainter(style: CellStyle, side: Float) {
    private val fill = style.fill
    private val cellSize = Size(side, side)
    private val border: (DrawScope.() -> Unit)? = when (val border = style.border) {
        is CellBorder.Plain -> plainBorder(border.color, pixelsOf(border.width, side), side)
        is CellBorder.Bevel -> bevel(border, pixelsOf(border.width, side), side)
        null -> null
    }
    private val centre: (DrawScope.() -> Unit)? = style.centre?.let { centreSquare(it, side) }

    fun draw(scope: DrawScope) {
        scope.drawRect(fill, size = cellSize)
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

/** A border of [width] pixels inside the cell. */
private fun plainBorder(
    color: Color,
    width: Float,
    side: Float,
): DrawScope.() -> Unit {
    val stroke = Stroke(width)
    val topLeft = Offset(width / 2, width / 2)
    val size = Size(side - width, side - width)
    return { drawRect(color, topLeft = topLeft, size = size, style = stroke) }
}

/** Light trapezoids on the top and left edges, dark ones on the bottom and right edges. */
private fun bevel(
    bevel: CellBorder.Bevel,
    width: Float,
    side: Float,
): DrawScope.() -> Unit {
    val far = side - width
    val top = trapezoid(Offset(0f, 0f), Offset(side, 0f), Offset(far, width), Offset(width, width))
    val left = trapezoid(Offset(0f, 0f), Offset(width, width), Offset(width, far), Offset(0f, side))
    val bottom = trapezoid(Offset(0f, side), Offset(width, far), Offset(far, far), Offset(side, side))
    val right = trapezoid(Offset(side, 0f), Offset(side, side), Offset(far, far), Offset(far, width))
    return {
        drawPath(top, bevel.light)
        drawPath(left, bevel.light)
        drawPath(bottom, bevel.dark)
        drawPath(right, bevel.dark)
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
