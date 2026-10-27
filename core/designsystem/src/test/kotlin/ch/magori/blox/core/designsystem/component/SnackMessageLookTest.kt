package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class SnackMessageLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `the text of a message stands out from ink by 4,5 to 1 at least, day and night`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = messageColors(palette)

        // Then on ink, in the colour of the page (WCAG 2.1, 1.4.3)
        assertThat(colors.fill).isEqualTo(palette.ink)
        assertThat(colors.text).isEqualTo(palette.page)
        assertThat(contrast(colors.text, colors.fill)).isGreaterThanOrEqualTo(4.5)
    }

    companion object {
        @JvmStatic
        fun colorful(): List<Arguments> = listOf(
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
