package ch.magori.blox.core.board

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position

/**
 * The cells to draw, row by row: the cell of column `c` and of the `r`-th row shown is at index
 * `r × board.columns + c`. They are the bottom [rows] of the visible rows 0 to 19 of [board], all of them unless told
 * otherwise, with the cells of the [piece] in play drawn over them. Its cells above the rows shown, the hidden rows -2
 * and -1 included, are not drawn.
 */
internal fun displayedCells(
    board: Board,
    piece: Piece?,
    rows: Int = board.rows,
): List<Cell> {
    val firstRow = board.rows - rows
    val cells = MutableList(rows * board.columns) { index ->
        board[Position(column = index % board.columns, row = firstRow + index / board.columns)]
    }
    piece?.cells?.filter {
        it in board && it.row >= firstRow
    }?.forEach { cells[(it.row - firstRow) * board.columns + it.column] = Cell.Filled(piece.type) }
    return cells
}
