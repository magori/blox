package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class BoardLockTest {
    @Test
    fun `locking a piece fills its four cells with its type on a new board`() {
        // Given
        val board = Board.parse(
            """
            ..........
            ..........
            IIII......
            """,
        )

        // When
        val locked = board.lock(tPointingDown(at = Position(5, 18)))

        // Then
        assertThat(locked).isEqualTo(
            Board.parse(
                """
                ..........
                ....TTT...
                IIII.T....
                """,
            ),
        )
    }

    @Test
    fun `cells in the hidden rows are lost, the visible ones join the board`() {
        // Given a vertical I whose pivot is on row 0: two cells in rows -2 and -1, two in rows 0 and 1
        val piece = verticalI(at = Position(4, 0))

        // When
        val locked = Board.empty().lock(piece)

        // Then
        assertThat(locked).isEqualTo(Board.empty().fill(Position(4, 0), PieceType.I).fill(Position(4, 1), PieceType.I))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("piecesThatDoNotFit")
    fun `locking a piece that does not fit is refused, even by a cell in a hidden row`(
        board: Board,
        piece: Piece,
    ) {
        assertThatIllegalArgumentException()
            .isThrownBy { board.lock(piece) }
            .withMessageContaining("does not fit")
    }

    @Test
    fun `the original board does not change`() {
        // Given
        val board = Board.parse(
            """
            ..........
            IIII......
            """,
        )

        // When
        board.lock(tPointingDown(at = Position(5, 18)))

        // Then
        assertThat(board).isEqualTo(
            Board.parse(
                """
                ..........
                IIII......
                """,
            ),
        )
    }

    private companion object {
        @JvmStatic
        fun piecesThatDoNotFit(): List<Arguments> = listOf(
            argumentSet(
                "a T whose lowest cell is on an occupied cell",
                Board.parse(
                    """
                    ..........
                    .....O....
                    """,
                ),
                tPointingDown(at = Position(5, 18)),
            ),
            argumentSet("a vertical I with a cell on row -3", Board.empty(), verticalI(at = Position(4, -1))),
            argumentSet("a T with a cell in column -1 of row -1", Board.empty(), tPointingDown(at = Position(0, -1))),
            argumentSet("a T with a cell in column 10 of row -1", Board.empty(), tPointingDown(at = Position(9, -1))),
        )
    }
}
