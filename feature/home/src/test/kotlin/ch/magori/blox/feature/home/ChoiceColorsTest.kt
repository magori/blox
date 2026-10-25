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
    fun `in every theme a selected choice tells its state by its check, standing out from it by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val selected = choiceColors(palette, selected = true)
        val unselected = choiceColors(palette, selected = false)

        // Then (WCAG 2.1, 1.4.11: the state of a user interface component, and 1.4.1: a cue other than colour): only
        // the selected choice is checked, its check standing out from its background. The lilac of the day stands out
        // from an unselected choice by 2.6:1 only.
        assertThat(unselected.check).isNull()
        assertThat(contrast(checkNotNull(selected.check), selected.background)).isGreaterThanOrEqualTo(3.0)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `a selected choice is checked in the ink of its label, an unselected one is not checked`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val selected = choiceColors(palette, selected = true)
        val unselected = choiceColors(palette, selected = false)

        // Then a cue other than colour (WCAG 2.1, 1.4.1), as legible as the label on its background
        assertThat(selected.check).isEqualTo(palette.accentInk)
        assertThat(unselected.check).isNull()
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
