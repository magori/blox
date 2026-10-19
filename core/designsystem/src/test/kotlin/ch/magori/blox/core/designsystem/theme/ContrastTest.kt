package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

internal class ContrastTest {
    @Test
    fun `black on white has the greatest contrast, 21 to 1, whatever the order`() {
        // When / Then (WCAG 2.1, contrast ratio)
        assertThat(contrast(Color.Black, Color.White)).isCloseTo(21.0, within(1e-6))
        assertThat(contrast(Color.White, Color.Black)).isCloseTo(21.0, within(1e-6))
    }
}
