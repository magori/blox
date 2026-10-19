package ch.magori.blox.feature.game.board

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/** Every board has the same columns and rows: the size of the board does not depend on its cells. */
private val BoardShape = Board.empty()

/**
 * Draws the [board] and the [piece] in play in [theme], on [background], the colour of the joints. No
 * logic here: the cells come from [displayedCells], their place from [BoardGeometry], their look from
 * the [CellPainter]s built by [painters].
 *
 * The geometry and the painters, with their paths, are built once per size, theme and background.
 * [board] and [piece] are read only while drawing: a move of the piece draws the board again, without
 * recomposing it nor building anything but its cells. Both must stay the same lambdas from one
 * composition to the next, reading a state rather than capturing a value: a new lambda changes the
 * modifier, and the geometry and the painters are built again.
 */
@Composable
internal fun BoardCanvas(
    board: () -> Board,
    piece: () -> Piece?,
    modifier: Modifier = Modifier,
    theme: BloxThemeKind = BloxTheme.kind,
    background: Color = BloxTheme.palette.boardBackground,
    painters: PaintersFactory = ::paintersFor,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val columns = BoardShape.columns
            val geometry = BoardGeometry.fit(size.width, size.height, columns, BoardShape.rows)
            val cellPainters = painters(theme, geometry.cellSize, background)
            onDrawBehind {
                val cells = displayedCells(board(), piece())
                for (index in cells.indices) {
                    val left = geometry.origin.x + index % columns * geometry.cellSize
                    val top = geometry.origin.y + index / columns * geometry.cellSize
                    translate(left, top) { cellPainters.getValue(cells[index]).draw(this) }
                }
            }
        },
    )
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

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview
@Composable
internal fun BoardCanvasOriginalPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(240.dp, 480.dp))
    }
}

@Preview
@Composable
private fun BoardCanvasColorfulPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        BoardCanvas({ previewBoard }, { previewPiece }, Modifier.size(240.dp, 480.dp))
    }
}
