package ch.magori.blox.core.game.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PositionTest {
    @Test
    fun `the offset from one position to another is their difference, column by column and row by row`() {
        // Given
        val from = Position(2, 5)
        val to = Position(7, 3)

        // When
        val offset = to - from

        // Then
        assertThat(offset).isEqualTo(Position(5, -2))
        assertThat(from + offset).isEqualTo(to)
    }

    @Test
    fun `the neighbours of a cell are the four cells that share an edge with it`() {
        // Given
        val cell = Position(4, 6)

        // When
        val neighbours = cell.neighbours()

        // Then
        assertThat(neighbours).containsExactlyInAnyOrder(Position(5, 6), Position(3, 6), Position(4, 7), Position(4, 5))
    }

    @Test
    fun `the first cell in reading order is on the topmost row, and the leftmost of that row`() {
        // Given cells whose leftmost lies on a lower row
        val cells = setOf(Position(3, 2), Position(0, 4), Position(5, 1), Position(2, 1))

        // When
        val first = cells.firstInReadingOrder()

        // Then
        assertThat(first).isEqualTo(Position(2, 1))
    }
}
