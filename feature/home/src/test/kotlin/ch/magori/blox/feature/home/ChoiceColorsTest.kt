package ch.magori.blox.feature.home

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ChoiceColorsTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `in every theme a selected choice stands out from an unselected one by a contrast of 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val selected = choiceColors(palette, selected = true)
        val unselected = choiceColors(palette, selected = false)

        // Then (WCAG 2.1, 1.4.11: the state of a user interface component; and 1.4.1: the two states differ in
        // luminance, not in hue only)
        assertThat(contrast(selected.background, unselected.background)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, selected: {2}")
    @MethodSource("themesSelectedOrNot")
    fun `the label of a choice stands out from its background by a contrast of 4,5 to 1 at least`(
        theme: String,
        palette: BloxPalette,
        selected: Boolean,
    ) {
        // When
        val colors = choiceColors(palette, selected)

        // Then (WCAG 2.1, 1.4.3: text)
        assertThat(contrast(colors.ink, colors.background)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `an unselected choice is framed as a panel, a selected one in its own background`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val selected = choiceColors(palette, selected = true)
        val unselected = choiceColors(palette, selected = false)

        // Then every choice has a frame of the same width, so a choice keeps its size whether selected or not
        assertThat(unselected.frame).isEqualTo(palette.panelBorder)
        assertThat(selected.frame).isEqualTo(selected.background)
    }

    companion object {
        @JvmStatic
        fun themes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }

        @JvmStatic
        fun themesSelectedOrNot(): List<Arguments> = themes().flatMap { theme ->
            listOf(true, false).map { Arguments.of(*theme.get(), it) }
        }
    }
}
