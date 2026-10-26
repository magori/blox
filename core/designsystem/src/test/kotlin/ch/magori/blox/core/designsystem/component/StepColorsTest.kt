package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StepColorsTest {
    @Test
    fun `a disabled slider in Original, whose panel border is its accent, is drawn in button, never in its accent`() {
        // Given
        val palette = BloxPalettes.Original

        // When
        val colors = disabledStepColors(palette)

        // Then its fill and its frames in button, its track beyond the value in panel inset
        assertThat(colors)
            .isEqualTo(StepColors(reached = palette.button, beyond = palette.panelInset, frame = palette.button))
    }

    @Test
    fun `a disabled slider by day frames its track in panel border, panel inset alone not showing on the page`() {
        // Given
        val palette = BloxPalettes.ColorfulLight

        // When
        val colors = disabledStepColors(palette)

        // Then
        assertThat(colors)
            .isEqualTo(
                StepColors(reached = palette.panelBorder, beyond = palette.panelInset, frame = palette.panelBorder),
            )
    }
}
