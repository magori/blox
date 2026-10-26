package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxButtonColorSets
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ChoiceLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a selected segment is filled in accent and checked in the ink of its label`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
    ) {
        // When
        val look = segmentLook(colors, selected = true, available = true)

        // Then
        assertThat(
            look,
        ).isEqualTo(SegmentLook(fill = palette.accent, ink = palette.accentInk, check = palette.accentInk))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `an unselected segment is clear on its group, its label in the ink of the secondary buttons, unchecked`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
    ) {
        // When
        val look = segmentLook(colors, selected = false, available = true)

        // Then the fill of the group shows through: the frame of the group, not of each segment, makes them seen
        assertThat(look).isEqualTo(SegmentLook(fill = null, ink = colors.secondary.ink, check = null))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `an unavailable segment is clear, its label in label, unchecked`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
    ) {
        // When
        val look = segmentLook(colors, selected = false, available = false)

        // Then
        assertThat(look).isEqualTo(SegmentLook(fill = null, ink = palette.label, check = null))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `a group is filled and framed as a secondary button, its frame standing out from the page by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
    ) {
        // When
        val look = groupLook(colors)

        // Then (WCAG 2.1, 1.4.11): what makes the choices seen is the frame of their group
        assertThat(look).isEqualTo(GroupLook(fill = colors.secondary.fill, frame = colors.secondary.frame!!))
        assertThat(contrast(look.frame, palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, selected: {3}, available: {4}")
    @MethodSource("variantsAndStates")
    fun `the label of a segment stands out from what is behind it by 4,5 to 1 at least, its check by 3 to 1`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
        selected: Boolean,
        available: Boolean,
    ) {
        // When
        val look = segmentLook(colors, selected, available)

        // Then (WCAG 2.1, 1.4.3 for the label, 1.4.11 for the check, the cue of the state)
        val behind = look.fill ?: groupLook(colors).fill
        assertThat(contrast(look.ink, behind)).isGreaterThanOrEqualTo(4.5)
        look.check?.let { assertThat(contrast(it, behind)).isGreaterThanOrEqualTo(3.0) }
    }

    @ParameterizedTest(name = "{0}, selected: {3}, available: {4}")
    @MethodSource("variantsAndStates")
    fun `a segment focused by the keyboard bears a 3 dp ring in ink, as a button`(
        theme: String,
        palette: BloxPalette,
        colors: BloxButtonColors,
        selected: Boolean,
        available: Boolean,
    ) {
        // When
        val focused = segmentLook(colors, selected, available, focused = true)
        val notFocused = segmentLook(colors, selected, available)

        // Then (WCAG 2.1, 2.4.7): the ring of the buttons, 3 dp off the edge of the fill
        assertThat(focused.focusRing).isEqualTo(FocusRing(color = palette.ink, width = 3.dp, gap = 3.dp))
        assertThat(notFocused.focusRing).isNull()
    }

    companion object {
        @JvmStatic
        fun variants(): List<Arguments> = listOf(
            Arguments.of("Original", BloxPalettes.Original, BloxButtonColorSets.Original),
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight, BloxButtonColorSets.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark, BloxButtonColorSets.ColorfulDark),
        )

        /** Each variant, with a segment selected, unselected, and unavailable. */
        @JvmStatic
        fun variantsAndStates(): List<Arguments> = variants().flatMap { variant ->
            listOf(true to true, false to true, false to false).map { (selected, available) ->
                Arguments.of(*variant.get(), selected, available)
            }
        }
    }
}
