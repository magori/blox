package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoardPointTest {
    @Test
    fun `a point of the board is where the canvas draws it, in a height that is no multiple of 20`() {
        // Given a canvas of 240 × 490 pixels: cells of 24 pixels, the board 5 pixels lower, centred in the height
        // When
        val point = boardPoint(width = 240f, height = 490f, column = 5f, row = 19f)

        // Then
        assertThat(point).isEqualTo(Offset(120f, 5f + 19 * 24f))
    }
}
