package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
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
 * the ring of a gloss in the joint.
 */
internal class CellPainter(style: CellStyle, side: Float, background: Color, density: Density) {
    private val layout = cellLayout(style, side, density)
    private val joint = style.joint?.let { background }
    private val cellSize = Size(side, side)
    private val fill: DrawScope.() -> Unit = fillOf(style, layout, density)
    private val border: (DrawScope.() -> Unit)? = layout.border?.let { edge ->
        style.border?.let { plainBorder(it.color, pixelsOf(it.width, side), edge) }
    }
    private val centre: (DrawScope.() -> Unit)? = style.centre?.let { centreSquare(it, side) }
    private val gloss: GlossBands? = layout.gloss.takeIf { it.layers.isNotEmpty() }?.let {
        glossBands(it, layout.shape, layout.fill.size, LayoutDirection.Ltr, density)
    }

    fun draw(scope: DrawScope) {
        joint?.let { scope.drawRect(it, size = cellSize) }
        fill(scope)
        border?.invoke(scope)
        centre?.invoke(scope)
        // The ring of the gloss fits in the margin of the cell (cellLayout): it never covers a cell beside it.
        gloss?.let { bands -> scope.translate(layout.fill.left, layout.fill.top) { bands.draw(this) } }
    }
}

/**
 * The fill of a cell: a square of its colour; or, for a bubble, its rounded shape, lit from the top as in the design
 * system, from the highlight at the top to the fill at 55 % of its height.
 */
private fun fillOf(
    style: CellStyle,
    layout: CellLayout,
    density: Density,
): DrawScope.() -> Unit {
    val rect = layout.fill
    if (layout.shape == RectangleShape) return { drawRect(style.fill, topLeft = rect.topLeft, size = rect.size) }
    val path = Path().apply {
        addOutline(layout.shape.createOutline(rect.size, LayoutDirection.Ltr, density))
        translate(rect.topLeft)
    }
    val highlight = style.bubble?.highlight ?: return { drawPath(path, style.fill) }
    val brush = Brush.verticalGradient(
        0f to highlight,
        HIGHLIGHT_END to style.fill,
        startY = rect.top,
        endY = rect.bottom,
    )
    return { drawPath(path, brush) }
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
