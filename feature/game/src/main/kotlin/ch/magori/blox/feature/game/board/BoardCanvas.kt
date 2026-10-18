package ch.magori.blox.feature.game.board

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
