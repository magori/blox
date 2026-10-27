package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class NameFieldFrameTest {
    @Test
    fun `the name field is framed in button outline at rest, in ink while it has the focus, in danger when refused`() {
        // Given
        val palette = BloxPalettes.ColorfulLight

        // When
        val frames = listOf(false to false, true to false, false to true, true to true).map { (focused, refused) ->
            nameFieldFrame(focused = focused, refused = refused, palette = palette)
        }

        // Then a refused name in danger, focused or not
        assertThat(frames).containsExactly(palette.buttonOutline, palette.ink, palette.danger, palette.danger)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `the frame of the name field at rest stands out from the panel by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val frame = nameFieldFrame(focused = false, refused = false, palette = palette)

        // Then (WCAG 2.1, 1.4.11): the panel inset of the box alone is about 1,1 to 1 on the panel
        assertThat(contrast(frame, palette.panel)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        @JvmStatic
        fun colorful(): List<Arguments> = listOf(
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
