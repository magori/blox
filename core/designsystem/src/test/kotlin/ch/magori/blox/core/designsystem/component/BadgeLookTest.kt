package ch.magori.blox.core.designsystem.component

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class BadgeLookTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("palettes")
    fun `a badge is accent-ink on accent, in every theme`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val look = badgeLook(palette)

        // Then
        assertThat(look.fill).isEqualTo(palette.accent)
        assertThat(look.ink).isEqualTo(palette.accentInk)
    }

    companion object {
        @JvmStatic
        fun palettes(): List<Arguments> = listOf(
            Arguments.of("Original", BloxPalettes.Original),
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
