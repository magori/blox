package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class BoardLandingTest {
    @Test
    fun `a piece lands straight below, as low as it fits, onto the stack`() {
        // Given a T pointing down above a stack whose top row is 18
        val board = Board.parse(
            """
            ....OO....
            ....OO....
            """,
        )
        val piece = tPointingDown(at = Position(4, 5))

        // When
        val landed = board.landing(piece)

        // Then its lowest cell rests on the O, in row 17, in the same column
        assertThat(landed).isEqualTo(tPointingDown(at = Position(4, 16)))
    }

    @Test
    fun `a piece already resting on the floor or on the stack is returned as it is`() {
        // Given a T resting on the floor, and another resting on the stack
        val board = Board.parse(
            """
            ..OO......
            ..OO......
            """,
        )
        val onTheFloor = tPointingDown(at = Position(7, 18))
        val onTheStack = tPointingDown(at = Position(3, 16))

        // When
        val landed = listOf(onTheFloor, onTheStack).map(board::landing)

        // Then
        assertThat(landed).containsExactly(onTheFloor, onTheStack)
    }

    @Test
    fun `a piece never goes through an occupied cell, even with a hole below it`() {
        // Given an overhang in rows 15 and 16, an empty hole under it
        val board = Board.parse(
            """
            ...JJJ....
            .....J....
            ..........
            ..........
            ..........
            """,
        )
        val piece = tPointingDown(at = Position(4, 2))

        // When
        val landed = board.landing(piece)

        // Then it stops on the overhang, its lowest cell in row 14
        assertThat(landed).isEqualTo(tPointingDown(at = Position(4, 13)))
    }

    @Test
    fun `a piece in the hidden rows lands in the visible board`() {
        // Given a vertical I reaching row -2, as right after rotating it at spawn
        val piece = verticalI(at = Position(5, 0))

        // When
        val landed = Board.empty().landing(piece)

        // Then it stands on the floor, rows 16 to 19
        assertThat(landed).isEqualTo(verticalI(at = Position(5, 18)))
    }

    @Test
    fun `a piece above a stack reaching the top stays in the hidden rows`() {
        // Given column 4 filled from row 19 up to row 0, and a T pointing down in the hidden rows -2 and -1,
        // its lowest cell right above that column
        val board = Board.parse(List(20) { "....J....." }.joinToString("\n"))
        val piece = tPointingDown(at = Position(4, -2))

        // When
        val landed = board.landing(piece)

        // Then it cannot go down: it stays where it is, in the hidden rows
        assertThat(landed).isEqualTo(piece)
    }

    @Test
    fun `a piece that does not fit where it is is refused, as by lock`() {
        // Given a T on an occupied cell
        val board = Board.parse(
            """
            ....J.....
            """,
        )
        val piece = tPointingDown(at = Position(4, 18))

        // When, then
        assertThatIllegalArgumentException()
            .isThrownBy { board.landing(piece) }
            .withMessageContaining("does not fit")
    }

    @Test
    fun `finding where a piece lands changes neither the board nor the piece`() {
        // Given
        val text = """
            ..........
            ....OO....
            ....OO....
            """
        val board = Board.parse(text)
        val piece = tPointingDown(at = Position(4, 5))

        // When
        board.landing(piece)

        // Then
        assertThat(board).isEqualTo(Board.parse(text))
        assertThat(piece).isEqualTo(tPointingDown(at = Position(4, 5)))
    }
}
