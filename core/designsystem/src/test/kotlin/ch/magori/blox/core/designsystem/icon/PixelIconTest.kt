package ch.magori.blox.core.designsystem.icon

import androidx.compose.ui.unit.IntRect
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PixelIconTest {
    @Test
    fun `a full pixel becomes a square of whole device pixels`() {
        // Given a drawing of a single full pixel, in a box of 16 device pixels: 2 for each of the 8 pixels
        val rows = listOf("#.......") + List(7) { "........" }

        // When
        val rects = pixelRects(rows, sizePx = 16)

        // Then
        assertThat(rects).containsExactly(IntRect(left = 0, top = 0, right = 2, bottom = 2))
    }

    @Test
    fun `each pixel is as many whole device pixels as fit eight times in the box, the drawing centred to the pixel`() {
        // Given a box of 42 device pixels (16 dp at 2.625): pixels of 5, a drawing of 40, 1 to spare on each side
        val rows = listOf("#......#") + List(7) { "........" }

        // When
        val rects = pixelRects(rows, sizePx = 42)

        // Then
        assertThat(rects).containsExactly(IntRect(1, 1, 6, 6), IntRect(36, 1, 41, 6))
    }

    @Test
    fun `a run of full pixels on a row is one rectangle`() {
        // Given
        val rows = listOf("###..##.") + List(7) { "........" }

        // When
        val rects = pixelRects(rows, sizePx = 16)

        // Then
        assertThat(rects).containsExactly(IntRect(0, 0, 6, 2), IntRect(10, 0, 14, 2))
    }
}
