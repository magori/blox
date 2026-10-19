package ch.magori.blox.feature.game.topbar

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class PauseButtonColorsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("palettes")
    fun `the pause button stands out from the page by its outline, by a contrast of 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = pauseButtonColors(palette)

        // Then (WCAG 2.1, 1.4.11: the edge of the button, drawn over its face, tells where the button is)
        assertThat(contrast(colors.outline, palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("palettes")
    fun `the pause button has the face and the ink of a button, its glyph standing out by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = pauseButtonColors(palette)

        // Then (WCAG 2.1, 1.4.11: the glyph tells what the button does)
        assertThat(colors.background).isEqualTo(palette.button)
        assertThat(colors.ink).isEqualTo(palette.buttonInk)
        assertThat(contrast(colors.ink, colors.background)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        @JvmStatic
        fun palettes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }
    }
}
