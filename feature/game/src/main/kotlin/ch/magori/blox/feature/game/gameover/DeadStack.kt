package ch.magori.blox.feature.game.gameover

import androidx.compose.runtime.Stable
import ch.magori.blox.core.board.BoardEffects
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position

/**
 * The stack of a game over, its last [board] and the [piece] that could not come in, which goes out row by row as the
 * game over [play] says (ADR 0024): each of its cells whose row is out is lit whole, in `cell-dead` given to the board as
 * its glow; empty cells stay as they are. Which cells are filled is worked out once: the stack of a game over no longer
 * changes, and a frame allocates nothing.
 */
@Stable
internal class DeadStack(board: Board, piece: Piece?, private val play: () -> GameOverPlay?) : BoardEffects {
    private val columns = board.columns

    private val filled = BooleanArray(board.columns * board.rows) { index ->
        board[Position(column = index % columns, row = index / columns)] != Cell.Empty
    }.also { cells ->
        piece?.cells?.filter { it in board }?.forEach { cells[it.row * columns + it.column] = true }
    }

    override fun cellLight(
        column: Int,
        row: Int,
    ): Float = if (filled[row * columns + column] && play()?.rowOut(row) == true) 1f else 0f
}
