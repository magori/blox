package ch.magori.blox.core.game.model

import ch.magori.blox.core.game.cellsOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class BoardCollisionTest {
    @Test
    fun `a piece on an empty board touches nothing`() {
        // Given
        val board = Board.empty()
        val piece = tPointingDown(at = Position(5, 10))

        // When
        val fits = board.fits(piece)

        // Then
        assertThat(fits).isTrue()
    }

    @ParameterizedTest(name = "pivot at ({0}, {1})")
    @CsvSource("0, 10", "9, 10", "0, -1", "9, -1")
    fun `a cell in column -1 or 10 is a collision, even in a hidden row`(
        pivotColumn: Int,
        pivotRow: Int,
    ) {
        // Given a T lying flat, one cell left and one cell right of its pivot
        val piece = tPointingDown(at = Position(pivotColumn, pivotRow))

        // When
        val fits = Board.empty().fits(piece)

        // Then
        assertThat(fits).isFalse()
    }

    @Test
    fun `a cell on row 20, below the floor, is a collision`() {
        // Given a T pointing down, its lowest cell one row below its pivot
        val piece = tPointingDown(at = Position(5, 19))

        // When
        val fits = Board.empty().fits(piece)

        // Then
        assertThat(fits).isFalse()
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("piecesTouchingAnEdge")
    fun `a piece touching an edge without crossing it fits`(piece: Piece) {
        // When
        val fits = Board.empty().fits(piece)

        // Then
        assertThat(fits).isTrue()
    }

    @ParameterizedTest(name = "T pivot on row {0}: fits = {1}")
    @CsvSource("18, false", "17, true")
    fun `a cell on an occupied square is a collision, the same piece just above fits`(
        pivotRow: Int,
        expectedFits: Boolean,
    ) {
        // Given
        val board = Board.parse(
            """
            ..........
            ....OO....
            """,
        )

        // When
        val fits = board.fits(tPointingDown(at = Position(5, pivotRow)))

        // Then
        assertThat(fits).isEqualTo(expectedFits)
    }

    @ParameterizedTest(name = "I pivot on row {0}: fits = {1}")
    @CsvSource("0, true", "-1, false")
    fun `the hidden rows -1 and -2 are free, row -3 is a collision`(
        pivotRow: Int,
        expectedFits: Boolean,
    ) {
        // Given a vertical I, two cells above its pivot and one below
        val piece = verticalI(at = Position(4, pivotRow))

        // When
        val fits = Board.empty().fits(piece)

        // Then
        assertThat(fits).isEqualTo(expectedFits)
    }

    private companion object {
        val T_DOWN = Orientation(
            cellsOf(
                """
                T@T
                .T.
                """,
            ),
        )
        val I_VERTICAL = Orientation(
            cellsOf(
                """
                I
                I
                @
                I
                """,
            ),
        )

        fun tPointingDown(at: Position): Piece = Piece(PieceType.T, T_DOWN, at)

        fun verticalI(at: Position): Piece = Piece(PieceType.I, I_VERTICAL, at)

        @JvmStatic
        fun piecesTouchingAnEdge(): List<Arguments> = listOf(
            argumentSet("vertical I against the left wall, up to row -2", verticalI(at = Position(0, 0))),
            argumentSet("vertical I against the right wall, up to row -2", verticalI(at = Position(9, 0))),
            argumentSet("T pointing down on the floor", tPointingDown(at = Position(5, 18))),
        )
    }
}
