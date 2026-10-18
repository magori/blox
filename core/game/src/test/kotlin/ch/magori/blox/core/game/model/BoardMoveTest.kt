package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class BoardMoveTest {
    @ParameterizedTest(name = "{0} moves the pivot from (5, 10) to ({1}, {2})")
    @CsvSource("LEFT, 4, 10", "RIGHT, 6, 10", "DOWN, 5, 11")
    fun `a piece moves one column left, one column right or one row down, the original piece is unchanged`(
        direction: Direction,
        expectedColumn: Int,
        expectedRow: Int,
    ) {
        // Given
        val piece = tPointingDown(at = Position(5, 10))

        // When
        val moved = Board.empty().move(piece, direction)

        // Then
        assertThat(moved).isEqualTo(tPointingDown(at = Position(expectedColumn, expectedRow)))
        assertThat(piece).isEqualTo(tPointingDown(at = Position(5, 10)))
    }

    @ParameterizedTest(name = "{0} with the pivot in column {1}")
    @CsvSource("LEFT, 1", "RIGHT, 8")
    fun `a move against the left or right wall is refused`(
        direction: Direction,
        pivotColumn: Int,
    ) {
        // Given a T lying flat, its outer cell already against the wall
        val piece = tPointingDown(at = Position(pivotColumn, 10))

        // When
        val moved = Board.empty().move(piece, direction)

        // Then
        assertThat(moved).isNull()
    }

    @Test
    fun `a move down onto the floor is refused`() {
        // Given a T pointing down, its lowest cell already on row 19
        val piece = tPointingDown(at = Position(5, 18))

        // When
        val moved = Board.empty().move(piece, Direction.DOWN)

        // Then
        assertThat(moved).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Direction::class)
    fun `a move onto an occupied cell is refused`(direction: Direction) {
        // Given a T pointing down at (5, 17), boxed in on its left, its right and below
        val board = Board.parse(
            """
            ...I...I..
            ..........
            .....O....
            """,
        )
        val piece = tPointingDown(at = Position(5, 17))

        // When
        val moved = board.move(piece, direction)

        // Then
        assertThat(moved).isNull()
    }

    @Test
    fun `a move within the hidden rows is accepted`() {
        // Given a vertical I reaching row -2, as right after rotating it at spawn
        val piece = verticalI(at = Position(5, 0))

        // When
        val moved = Board.empty().move(piece, Direction.LEFT)

        // Then
        assertThat(moved).isEqualTo(verticalI(at = Position(4, 0)))
    }
}
