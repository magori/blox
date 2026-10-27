package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BubbleDrawingTest {
    @Test
    fun `a bubble is lit from its highlight at the top and is in its colour from 55 percent of its height down`() {
        // Given a note of the melody by day, 44 px high: its highlight melody-hi, its colour melody
        val highlight = Color(0xFFE6DCFC)
        val fill = Color(0xFF7C4DEB)

        // When
        val gradient = bubbleGradient(highlight, fill, height = 44f)

        // Then (docs/design-system/components/Piece): the lower part, from 55 % down, is the colour alone, the one that
        // stands out from the cell (3:1 at least, TrackColorsTest)
        assertThat(gradient).isEqualTo(Brush.verticalGradient(0f to highlight, 0.55f to fill, startY = 0f, endY = 44f))
    }
}
