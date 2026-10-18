package ch.magori.blox.feature.game.board

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position

/**
 * The cells to draw, row by row: the cell of column `c` and row `r` is at index
 * `r × board.columns + c`. They are the visible rows 0 to 19 of [board], with the cells of the
 * [piece] in play drawn over it. Its cells in the hidden rows -2 and -1 are not drawn.
 */
internal fun displayedCells(
    board: Board,
    piece: Piece?,
): List<Cell> {
    val cells = MutableList(board.rows * board.columns) { index ->
        board[Position(column = index % board.columns, row = index / board.columns)]
    }
    piece?.cells?.filter {
        it in board
    }?.forEach { cells[it.row * board.columns + it.column] = Cell.Filled(piece.type) }
    return cells
}
