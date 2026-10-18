package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BoardStackHeightTest {
    @Test
    fun `an empty board has a stack height of 0`() {
        assertThat(Board.empty().stackHeight).isEqualTo(0)
    }

    @ParameterizedTest(name = "a cell on row {0} -> height {1}")
    @CsvSource("19, 1", "0, 20")
    fun `the stack height is 20 minus the first filled row`(
        row: Int,
        expectedHeight: Int,
    ) {
        val board = Board.empty().fill(Position(column = 3, row = row), PieceType.T)

        assertThat(board.stackHeight).isEqualTo(expectedHeight)
    }

    @Test
    fun `an isolated cell high up counts, even above a hole`() {
        val board = Board.parse(
            """
            ...T......
            ..........
            ..........
            ..........
            IIII.IIIII
            """,
        )

        assertThat(board.stackHeight).isEqualTo(5)
    }
}
