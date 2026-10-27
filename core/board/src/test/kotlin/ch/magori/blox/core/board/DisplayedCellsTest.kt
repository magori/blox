package ch.magori.blox.core.board

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DisplayedCellsTest {
    @Test
    fun `without a piece in play the displayed cells are the 200 cells of rows 0 to 19, row by row`() {
        // Given
        val board = Board.parse(
            """
            ..........
            IIIIOO.SLL
            """,
        )

        // When
        val cells = displayedCells(board, piece = null)

        // Then: the cell of column c and row r is at index r × 10 + c
        assertThat(cells).hasSize(200)
        assertThat(cells[190]).isEqualTo(Cell.Filled(PieceType.I))
        assertThat(cells[196]).isEqualTo(Cell.Empty)
        assertThat(cells[199]).isEqualTo(Cell.Filled(PieceType.L))
        assertThat(cells.filterIsInstance<Cell.Filled>()).hasSize(9)
    }

    @Test
    fun `the piece in play is drawn over the board, without its cells in the hidden rows`() {
        // Given a board with a full bottom row and a T as it spawns, pointing down, its top row hidden (row -1)
        val board = Board.parse("IIIIIIIIII")
        val tDown = ClassicRotationSystem.spawnOrientation(PieceType.T)
        val piece = Piece(PieceType.T, tDown, pivot = Position(column = 4, row = -1))

        // When
        val cells = displayedCells(board, piece)

        // Then: only its cell (4, 0) is visible
        assertThat(cells).hasSize(200)
        assertThat(cells[4]).isEqualTo(Cell.Filled(PieceType.T))
        assertThat(cells.filter { it == Cell.Filled(PieceType.T) }).hasSize(1)
        assertThat(cells.filter { it == Cell.Filled(PieceType.I) }).hasSize(10)
    }

    @Test
    fun `the cells of the bottom rows are the last rows of the board, the piece drawn over them`() {
        // Given a stack of two rows and a T landed on it, pointing down, its top row in row 17
        val board = Board.parse(
            """
            ..........
            IIII.....O
            JJJ.....LL
            """,
        )
        val tDown = ClassicRotationSystem.spawnOrientation(PieceType.T)
        val piece = Piece(PieceType.T, tDown, pivot = Position(column = 5, row = 17))

        // When only the bottom 2 rows are shown
        val cells = displayedCells(board, piece, rows = 2)

        // Then: the cell of column c and row 18 + r is at index r × 10 + c; the row of the T above them is not shown
        assertThat(cells).hasSize(20)
        assertThat(cells[0]).isEqualTo(Cell.Filled(PieceType.I))
        assertThat(cells[5]).isEqualTo(Cell.Filled(PieceType.T))
        assertThat(cells[19]).isEqualTo(Cell.Filled(PieceType.L))
        assertThat(cells.filter { it == Cell.Filled(PieceType.T) }).hasSize(1)
    }

    @Test
    fun `asking more rows than the board has, or none, is refused`() {
        val board = Board.empty()

        assertThatThrownBy { displayedCells(board, piece = null, rows = 21) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { displayedCells(board, piece = null, rows = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
