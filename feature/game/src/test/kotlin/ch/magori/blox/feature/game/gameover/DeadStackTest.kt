package ch.magori.blox.feature.game.gameover

import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeadStackTest {
    @Test
    fun `the cells of the stack and of the piece go out with their row, the empty cells never`() {
        // Given the last board of a game, a T over its stack, and its game over once its two bottom rows are out
        val board = Board.parse(
            """
            ..........
            I.........
            IZZ.......
            """,
        )
        val piece = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 18))
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false).advance(ticks = 4)
        val effects = DeadStack(board, piece) { play }

        // Then the cells out are those of the two bottom rows that hold something: stack or piece
        val out = (0 until 20).flatMap { row -> (0 until 10).map { Position(it, row) } }
            .filter { effects.cellLight(it.column, it.row) == 1f }
        val pieceCells = piece.cells.filter { it.row >= 18 }
        assertThat(out).containsExactlyInAnyOrderElementsOf(
            listOf(Position(0, 18), Position(0, 19), Position(1, 19), Position(2, 19)) + pieceCells,
        )
        assertThat((0 until 20).all { row -> (0 until 10).all { effects.cellLight(it, row) in setOf(0f, 1f) } })
            .isTrue()
    }
}
