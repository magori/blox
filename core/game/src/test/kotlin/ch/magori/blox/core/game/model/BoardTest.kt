package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class BoardTest {
    @Test
    fun `an empty board has 10 columns and 20 rows`() {
        // Given
        val board = Board.empty()

        // Then
        assertThat(board.columns).isEqualTo(10)
        assertThat(board.rows).isEqualTo(20)
    }

    @Test
    fun `every cell of an empty board is empty`() {
        // Given
        val board = Board.empty()

        // When
        val cells = board.cellsOfRows(0..19)

        // Then
        assertThat(cells).hasSize(200).containsOnly(Cell.Empty)
    }

    @ParameterizedTest(name = "column {0}, row {1} is outside")
    @CsvSource("-1, 0", "10, 0", "0, -1", "0, 20", "-1, -1", "10, 20")
    fun `a position outside columns 0 to 9 or rows 0 to 19 is outside the board`(
        column: Int,
        row: Int,
    ) {
        // Given
        val board = Board.empty()

        // When
        val inside = Position(column, row) in board

        // Then
        assertThat(inside).isFalse()
    }

    @ParameterizedTest(name = "column {0}, row {1} is inside")
    @CsvSource("0, 0", "9, 0", "0, 19", "9, 19")
    fun `the four corners are inside the board`(
        column: Int,
        row: Int,
    ) {
        // Given
        val board = Board.empty()

        // When
        val inside = Position(column, row) in board

        // Then
        assertThat(inside).isTrue()
    }

    @Test
    fun `parse reads a dot as an empty cell and a piece letter as a cell filled by that piece`() {
        // Given
        val text = "IOTSZJL..."

        // When
        val board = Board.parse(text)

        // Then
        assertThat(board.cellsOfRows(19..19)).containsExactly(
            Cell.Filled(PieceType.I),
            Cell.Filled(PieceType.O),
            Cell.Filled(PieceType.T),
            Cell.Filled(PieceType.S),
            Cell.Filled(PieceType.Z),
            Cell.Filled(PieceType.J),
            Cell.Filled(PieceType.L),
            Cell.Empty,
            Cell.Empty,
            Cell.Empty,
        )
    }

    @Test
    fun `parse reads the text as the bottom rows and leaves the missing rows above empty`() {
        // Given
        val text =
            """

            T.........
            ..........
            IIIIOO.SLL

            """

        // When
        val board = Board.parse(text)

        // Then
        assertThat(board[Position(0, 17)]).isEqualTo(Cell.Filled(PieceType.T))
        assertThat(board[Position(1, 17)]).isEqualTo(Cell.Empty)
        assertThat(board[Position(0, 18)]).isEqualTo(Cell.Empty)
        assertThat(board[Position(0, 19)]).isEqualTo(Cell.Filled(PieceType.I))
        assertThat(board[Position(6, 19)]).isEqualTo(Cell.Empty)
        assertThat(board[Position(9, 19)]).isEqualTo(Cell.Filled(PieceType.L))
        assertThat(board.cellsOfRows(0..16)).containsOnly(Cell.Empty)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidTexts")
    fun `parse rejects a row that is not 10 characters long, an unknown character and more than 20 rows`(
        text: String,
        expectedMessage: String,
    ) {
        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { Board.parse(text) }
            .withMessageContaining(expectedMessage)
    }

    @Test
    fun `filling a cell returns a new board and leaves the original unchanged`() {
        // Given
        val original = Board.parse(
            """
            ..........
            IIII......
            """,
        )

        // When
        val filled = original.fill(Position(4, 19), PieceType.T)

        // Then
        assertThat(filled).isEqualTo(
            Board.parse(
                """
                ..........
                IIIIT.....
                """,
            ),
        )
        assertThat(original).isEqualTo(
            Board.parse(
                """
                ..........
                IIII......
                """,
            ),
        )
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("accessesOutsideTheBoard")
    fun `reading or filling a cell outside the board is rejected`(access: (Board) -> Any) {
        // Given
        val board = Board.empty()

        // When / Then
        assertThatIllegalArgumentException()
            .isThrownBy { access(board) }
            .withMessageContaining("is outside the board")
    }

    @Test
    fun `two boards described by the same text are equal`() {
        // Given
        val text =
            """
            .....T....
            IIIIOO.SLL
            """

        // When
        val first = Board.parse(text)
        val second = Board.parse(text)

        // Then
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
    }

    @Test
    fun `a board is written as 20 rows of 10 characters separated by newlines`() {
        // Given
        val board = Board.empty()
            .fill(Position(5, 18), PieceType.T)
            .fill(Position(0, 19), PieceType.I)
            .fill(Position(9, 19), PieceType.L)

        // When
        val text = board.toString()

        // Then
        val expected = List(18) { ".........." } + ".....T...." + "I........L"
        assertThat(text).isEqualTo(expected.joinToString("\n"))
    }

    @Test
    fun `parsing the text of a board gives back the same board`() {
        // Given
        val board = Board.parse(
            """
            T.........
            ZZ.....OO.
            .ZZ...SOO.
            IIIIJJSSLL
            """,
        )

        // When
        val roundTrip = Board.parse(board.toString())

        // Then
        assertThat(roundTrip).isEqualTo(board)
    }

    private fun Board.cellsOfRows(rows: IntRange): List<Cell> =
        rows.flatMap { row -> (0 until columns).map { column -> this[Position(column, row)] } }

    private companion object {
        @JvmStatic
        fun invalidTexts(): List<Arguments> = listOf(
            argumentSet("a row of 9 characters", ".........", "10 characters"),
            argumentSet("a row of 11 characters", "...........", "10 characters"),
            argumentSet("a blank row between two rows", "..........\n\n..........", "10 characters"),
            argumentSet("an unknown character", "....X.....", "'X'"),
            argumentSet("a lowercase piece letter", "....t.....", "'t'"),
            argumentSet("21 rows", List(21) { ".........." }.joinToString("\n"), "20 rows"),
        )

        @JvmStatic
        fun accessesOutsideTheBoard(): List<Arguments> =
            listOf(Position(10, 0), Position(-1, 5), Position(0, 20), Position(0, -1)).flatMap { position ->
                listOf(
                    argumentSet("get $position", { board: Board -> board[position] }),
                    argumentSet("fill $position", { board: Board -> board.fill(position, PieceType.T) }),
                )
            }
    }
}
