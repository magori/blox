package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class BoardClearTest {
    @Test
    fun `a full row is cleared and the rows above fall down one row`() {
        val result = clearing(
            """
            ....T.....
            IIIIOOJJLL
            """,
        )

        assertThat(result).isEqualTo(
            cleared(
                """
                ....T.....
                """,
                clearedRows = listOf(19),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("contiguousFullRows")
    fun `two three and four contiguous full rows are cleared`(
        before: String,
        expected: RowsCleared,
    ) {
        assertThat(clearing(before)).isEqualTo(expected)
    }

    @Test
    fun `with full rows that are not contiguous everything falls to the right place`() {
        val result = clearing(
            """
            ....T.....
            IIIIOOJJLL
            SSZZ.TTIII
            LLLJJJOOII
            """,
        )

        assertThat(result).isEqualTo(
            cleared(
                """
                ....T.....
                SSZZ.TTIII
                """,
                clearedRows = listOf(17, 19),
            ),
        )
    }

    @Test
    fun `without a full row the board does not change and no row is cleared`() {
        val board = Board.parse(
            """
            ....T.....
            IIII.OJJLL
            """,
        )

        val result = board.clearFullRows()

        assertThat(result.board).isSameAs(board)
        assertThat(result.clearedRows).isEmpty()
    }

    @Test
    fun `empty rows appear at the top, as many as rows cleared`() {
        val result = clearing(
            """
            T.........
            .J........
            ..Z.......
            ...O......
            ....S.....
            .....L....
            ......I...
            .......T..
            ........J.
            .........Z
            O.........
            .S........
            ..L.......
            ...I......
            ....T.....
            .....J....
            ......Z...
            IIIIOOJJLL
            .......O..
            SSZZTTTIII
            """,
        )

        assertThat(result).isEqualTo(
            cleared(
                """
                ..........
                ..........
                T.........
                .J........
                ..Z.......
                ...O......
                ....S.....
                .....L....
                ......I...
                .......T..
                ........J.
                .........Z
                O.........
                .S........
                ..L.......
                ...I......
                ....T.....
                .....J....
                ......Z...
                .......O..
                """,
                clearedRows = listOf(17, 19),
            ),
        )
    }

    @Test
    fun `a full top row is replaced by an empty row`() {
        val result = clearing(
            """
            IIIIOOJJLL
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            T.........
            """,
        )

        assertThat(result).isEqualTo(
            cleared(
                """
                T.........
                """,
                clearedRows = listOf(0),
            ),
        )
    }

    @Test
    fun `the result gives the number of lines cleared at once`() {
        val result = clearing(
            """
            .T........
            IIIIOOJJLL
            SSZZTTTIII
            """,
        )

        assertThat(result.lineClear).isEqualTo(LineClear(2))
    }

    private companion object {
        /** When: the full rows of the board drawn in [before] are cleared. */
        fun clearing(before: String): RowsCleared = Board.parse(before).clearFullRows()

        fun cleared(
            after: String,
            clearedRows: List<Int>,
        ): RowsCleared = RowsCleared(Board.parse(after), clearedRows)

        @JvmStatic
        fun contiguousFullRows(): List<Arguments> = listOf(
            argumentSet(
                "two rows",
                """
                .T........
                IIIIOOJJLL
                SSZZTTTIII
                """,
                cleared(
                    """
                    .T........
                    """,
                    clearedRows = listOf(18, 19),
                ),
            ),
            argumentSet(
                "three rows",
                """
                ..O.......
                IIIIOOJJLL
                SSZZTTTIII
                LLLJJJOOII
                """,
                cleared(
                    """
                    ..O.......
                    """,
                    clearedRows = listOf(17, 18, 19),
                ),
            ),
            argumentSet(
                "four rows, with a row left below",
                """
                ...J......
                IIIIOOJJLL
                SSZZTTTIII
                LLLJJJOOII
                TTTZZZSSOO
                IIII.IIIII
                """,
                cleared(
                    """
                    ...J......
                    IIII.IIIII
                    """,
                    clearedRows = listOf(15, 16, 17, 18),
                ),
            ),
        )
    }
}
