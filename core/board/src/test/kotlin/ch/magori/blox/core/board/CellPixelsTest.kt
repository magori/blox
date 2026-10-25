package ch.magori.blox.core.board

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CellPixelsTest {
    @ParameterizedTest(name = "{0}/24 of a {1} px cell is {2} px")
    @CsvSource("2, 24, 2", "2, 48, 4", "3, 40, 5", "8, 25, 8", "1, 12, 1", "1, 10, 1", "3, 4, 1")
    fun `a fraction of the cell is drawn as a whole number of pixels, at least one`(
        twentyFourths: Float,
        cellSize: Float,
        pixels: Float,
    ) {
        // When
        val drawn = pixelsOf(fraction = twentyFourths / 24, cellSize = cellSize)

        // Then
        assertThat(drawn).isEqualTo(pixels)
    }
}
