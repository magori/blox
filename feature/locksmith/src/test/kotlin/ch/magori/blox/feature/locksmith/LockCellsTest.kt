package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The cells of the padlock the screen draws: its 8 × 8 inside the ring of metal, and the preview over the ring. */
class LockCellsTest {
    @Test
    fun `the screen draws the 8 by 8 cells inside the ring, the hole as holes and the rest as metal`() {
        // When
        val cells = lockCells(LocksmithUiState(level = 1, game = PadlockGame.start(PADLOCK)))

        // Then
        val inside = 1..Padlock.SIZE - 2
        val grid = inside.flatMap { row -> inside.map { Position(it, row) } }
        assertThat(cells.keys).containsExactlyInAnyOrderElementsOf(grid)
        assertThat(cells.filterValues { it == LockCell.Hole }.keys).isEqualTo(PADLOCK.hole)
        assertThat(cells.filterKeys { it !in PADLOCK.hole }.values).containsOnly(LockCell.Metal)
    }

    @Test
    fun `a preview cell on the ring of metal is drawn crossed out`() {
        // Given: the I selected, flat, its pivot on (1, 1): it covers (-1, 1) to (2, 1)
        val state = LocksmithUiState(level = 1, game = PadlockGame.start(PADLOCK), aim = Position(1, 1))

        // When
        val rim = rimCells(state)

        // Then: (0, 1) is on the ring, drawn over the rim; (-1, 1) off the grid and not drawn; the others inside
        assertThat(rim).isEqualTo(mapOf(Position(0, 1) to LockCell.Preview(PieceType.I, fits = false)))
    }

    @Test
    fun `a placed piece shows its type on its cells`() {
        // Given: the I placed where the solution has it
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game

        // When
        val cells = lockCells(LocksmithUiState(level = 1, game = game))

        // Then
        assertThat((3..6).map { cells[Position(it, 1)] }).containsOnly(LockCell.Placed(PieceType.I))
    }

    @Test
    fun `the preview shows the selected piece, each cell off the hole or over a placed piece crossed out`() {
        // Given: the I placed, the O selected, aimed with its pivot on (3, 2): it covers (2, 2), (3, 2), (2, 3), (3, 3)
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game
        val state = LocksmithUiState(level = 1, game = game, aim = Position(3, 2))

        // When
        val cells = lockCells(state)

        // Then: (2, 2) and (3, 2) are free hole, (2, 3) and (3, 3) metal
        assertThat(cells[Position(2, 2)]).isEqualTo(LockCell.Preview(PieceType.O, fits = true))
        assertThat(cells[Position(3, 2)]).isEqualTo(LockCell.Preview(PieceType.O, fits = true))
        assertThat(cells[Position(2, 3)]).isEqualTo(LockCell.Preview(PieceType.O, fits = false))
        assertThat(cells[Position(3, 3)]).isEqualTo(LockCell.Preview(PieceType.O, fits = false))
    }

    @Test
    fun `a preview over a placed piece crosses out the cells they share`() {
        // Given: the I placed on row 1, the O aimed over (3, 1) and (4, 1)
        val game = PadlockGame.start(PADLOCK).place(Position(5, 1)).game
        val state = LocksmithUiState(level = 1, game = game, aim = Position(4, 1))

        // When
        val cells = lockCells(state)

        // Then
        assertThat(cells[Position(3, 1)]).isEqualTo(LockCell.Preview(PieceType.O, fits = false))
        assertThat(cells[Position(3, 2)]).isEqualTo(LockCell.Preview(PieceType.O, fits = true))
    }

    @Test
    fun `a hint marks the free cells where its piece fits`() {
        // Given
        val fit = Hint.Fit(
            rank = 1,
            piece = Piece(PieceType.O, ClassicRotationSystem.spawnOrientation(PieceType.O), Position(2, 1)),
        )
        val state = LocksmithUiState(level = 1, game = PadlockGame.start(PADLOCK), hint = fit)

        // When
        val cells = lockCells(state)

        // Then
        assertThat(fit.piece.cells.map { cells[it] }).containsOnly(LockCell.Hint)
        assertThat(cells[Position(3, 1)]).isEqualTo(LockCell.Hole)
    }

    @Test
    fun `a hint naming a placed piece marks that piece`() {
        // Given: the I placed one column to the left, on columns 2 to 5 of row 1
        val game = PadlockGame.start(PADLOCK).place(Position(4, 1)).game
        val state = LocksmithUiState(level = 1, game = game, hint = Hint.Blocking(rank = 0))

        // When
        val cells = lockCells(state)

        // Then
        assertThat((2..5).map { cells[Position(it, 1)] }).containsOnly(LockCell.Placed(PieceType.I, hinted = true))
    }

    @Test
    fun `the preview is drawn over the hint`() {
        // Given
        val fit = Hint.Fit(
            rank = 1,
            piece = Piece(PieceType.O, ClassicRotationSystem.spawnOrientation(PieceType.O), Position(2, 1)),
        )
        val state =
            LocksmithUiState(level = 1, game = PadlockGame.start(PADLOCK).select(1), hint = fit, aim = Position(2, 1))

        // When
        val cells = lockCells(state)

        // Then
        assertThat(fit.piece.cells.map { cells[it] }).containsOnly(LockCell.Preview(PieceType.O, fits = true))
    }

    private companion object {
        /** O on rank 1, I on rank 2, T on rank 3; given as I, O, T. */
        val PADLOCK = Padlock.parse(
            """
            ##########
            #112222###
            #11333####
            ####3#####
            ##########
            ##########
            ##########
            ##########
            ##########
            ##########
            """,
        )
    }
}
