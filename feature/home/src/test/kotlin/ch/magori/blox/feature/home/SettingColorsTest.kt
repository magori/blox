package ch.magori.blox.feature.home

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class SettingColorsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `the label of a setting stands out from its row by a contrast of 4,5 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val row = settingColors(palette).row

        // Then (WCAG 2.1, 1.4.3: text)
        assertThat(contrast(row.ink, row.background)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `a switch off, its outline and its thumb, stands out from its row by a contrast of 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = settingColors(palette)
        val row = colors.row.background

        // Then (WCAG 2.1, 1.4.11: a user interface component)
        assertThat(contrast(colors.switch.uncheckedBorderColor, row)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(colors.switch.uncheckedThumbColor, colors.switch.uncheckedTrackColor))
            .isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `a switch is framed in button outline, in ink in Original, standing out from its row by 3 to 1`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = settingColors(palette)
        val switch = colors.switch

        // Then (WCAG 2.1, 1.4.11): the track stands out from the row by its frame, whatever its state, and the state
        // reads by the thumb, on the right when on (Material Switch), standing out from the track
        val frame = if (palette == BloxPalettes.Original) palette.ink else palette.buttonOutline
        assertThat(listOf(switch.checkedBorderColor, switch.uncheckedBorderColor)).containsOnly(frame)
        assertThat(contrast(frame, colors.row.background)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(switch.checkedThumbColor, switch.checkedTrackColor)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        @JvmStatic
        fun themes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }
    }
}
