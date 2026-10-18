package ch.magori.blox.feature.game.board

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * Draws [board] and the [piece] in play in [theme]. No logic here: the cells come from
 * [displayedCells], their place from [BoardGeometry], their look from [cellStyle] and [pixelsOf].
 * Everything that depends on the size is built once per size; a frame only reads and draws.
 */
@Composable
internal fun BoardCanvas(
    board: Board,
    piece: Piece?,
    modifier: Modifier = Modifier,
    theme: BloxThemeKind = BloxTheme.kind,
) {
    val cells = remember(board, piece) { displayedCells(board, piece) }
    val columns = board.columns
    val rows = board.rows
    Spacer(
        modifier = modifier.drawWithCache {
            val geometry = BoardGeometry.fit(size.width, size.height, columns, rows)
            val painters = paintersFor(theme, geometry.cellSize)
            onDrawBehind {
                for (index in cells.indices) {
                    val left = geometry.origin.x + index % columns * geometry.cellSize
                    val top = geometry.origin.y + index / columns * geometry.cellSize
                    translate(left, top) { painters.getValue(cells[index]).draw(this) }
                }
            }
        },
    )
}

/** A painter for each cell that can be drawn: the empty cell and a cell of each piece. */
private fun paintersFor(
    theme: BloxThemeKind,
    cellSize: Float,
): Map<Cell, CellPainter> = (listOf(Cell.Empty) + PieceType.entries.map(Cell::Filled))
    .associateWith { cell -> CellPainter(cellStyle(theme, cell), cellSize) }

/** One cell of [side] pixels in [style], with its shapes built once; it draws at the origin. */
private class CellPainter(style: CellStyle, side: Float) {
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

/** A board holding the seven pieces, and a T in play as it spawns, for the previews. */
private val previewBoard = Board.parse(
    """
    J.........
    JJJ..OO..L
    SSTTTOO.LL
    IIII.ZZSSL
    """,
)

private val previewPiece = Piece(
    PieceType.T,
    ClassicRotationSystem.spawnOrientation(PieceType.T),
    pivot = Position(column = 4, row = 6),
)

@Preview
@Composable
private fun BoardCanvasOriginalPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        BoardCanvas(previewBoard, previewPiece, Modifier.size(240.dp, 480.dp))
    }
}

@Preview
@Composable
private fun BoardCanvasColorfulPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        BoardCanvas(previewBoard, previewPiece, Modifier.size(240.dp, 480.dp))
    }
}
