package ch.magori.blox.core.board.next

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.board.BoardGeometry
import ch.magori.blox.core.board.CellPainter
import ch.magori.blox.core.board.R
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/**
 * Draws the [next] piece in [theme], with the cells of the board, of [cellSize] pixels, on
 * [background], the colour of their joints (the panel of the next piece), centred in the space given
 * to it; nothing when [next] is null. No logic here: the cells come from the screen state
 * ([nextPieceCells]), their place from [BoardGeometry], their look from [CellPainter].
 */
@Composable
fun NextPieceCanvas(
    next: NextPieceCells?,
    cellSize: Float,
    modifier: Modifier = Modifier,
    theme: BloxThemeKind = BloxTheme.kind,
    background: Color = BloxTheme.palette.panel,
) {
    val description = nextPieceName(next)
    Spacer(
        modifier = modifier
            .semantics { contentDescription = description }
            .drawWithCache {
                if (next == null) return@drawWithCache onDrawBehind {}
                val geometry = BoardGeometry.centred(size.width, size.height, next.columns, next.rows, cellSize)
                val painter = CellPainter(cellStyle(theme, Cell.Filled(next.type)), geometry.cellSize, background)
                onDrawBehind {
                    for (cell in next.cells) {
                        val topLeft = geometry.cellTopLeft(cell)
                        translate(topLeft.x, topLeft.y) { painter.draw(this) }
                    }
                }
            },
    )
}

/** The name of the [next] piece for TalkBack, or that there is none when it is hidden. */
@Composable
@ReadOnlyComposable
fun nextPieceName(next: NextPieceCells?): String =
    next?.let { pieceName(it.type) } ?: stringResource(R.string.core_board_next_none)

/** The name of a piece, for TalkBack. */
@Composable
@ReadOnlyComposable
fun pieceName(type: PieceType): String = stringResource(
    when (type) {
        PieceType.I -> R.string.core_board_piece_i
        PieceType.O -> R.string.core_board_piece_o
        PieceType.T -> R.string.core_board_piece_t
        PieceType.S -> R.string.core_board_piece_s
        PieceType.Z -> R.string.core_board_piece_z
        PieceType.J -> R.string.core_board_piece_j
        PieceType.L -> R.string.core_board_piece_l
    },
)
