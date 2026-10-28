package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AsciiGridTest {
    @Test
    fun `a grid ignores the common indentation and the blank lines around it, not those inside`() {
        // Given an indented drawing, blank lines above, inside and below
        val ascii = """

            #.#
              .

             ##

        """

        // When
        val grid = AsciiGrid.parse(ascii)

        // Then
        assertThat(grid.lines).containsExactly("#.#", "  .", "", " ##")
    }

    @Test
    fun `each character of a grid lies at its column and row, in reading order`() {
        // Given
        val grid = AsciiGrid.parse(
            """
            ab
            c.
            """,
        )

        // When
        val cells = grid.cells

        // Then
        assertThat(cells.toList()).containsExactly(
            Position(0, 0) to 'a',
            Position(1, 0) to 'b',
            Position(0, 1) to 'c',
            Position(1, 1) to '.',
        )
    }

    @Test
    fun `an empty line inside a grid has no cell, and the lines after it keep their row`() {
        // Given lines of unequal lengths, an empty one inside
        val grid = AsciiGrid.parse(
            """
            abc

            d
            """,
        )

        // When
        val cells = grid.cells

        // Then
        assertThat(cells.toList()).containsExactly(
            Position(0, 0) to 'a',
            Position(1, 0) to 'b',
            Position(2, 0) to 'c',
            Position(0, 2) to 'd',
        )
    }
}
