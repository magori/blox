package ch.magori.blox.feature.game.board

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
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
}
