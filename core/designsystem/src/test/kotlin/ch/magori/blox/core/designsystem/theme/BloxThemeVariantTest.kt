package ch.magori.blox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class BloxThemeVariantTest {
    @Test
    fun `the Colorful theme is the day variant in light mode and the night variant in dark mode`() {
        // When the Colorful theme is resolved in each mode of the system
        val day = BloxThemeVariant.of(BloxThemeKind.COLORFUL, darkSystem = false)
        val night = BloxThemeVariant.of(BloxThemeKind.COLORFUL, darkSystem = true)

        // Then
        assertThat(day).isEqualTo(BloxThemeVariant.COLORFUL_LIGHT)
        assertThat(night).isEqualTo(BloxThemeVariant.COLORFUL_DARK)
    }

    @ParameterizedTest(name = "dark system: {0}")
    @ValueSource(booleans = [false, true])
    fun `the Original theme has a single variant, whatever the mode of the system`(darkSystem: Boolean) {
        // When
        val variant = BloxThemeVariant.of(BloxThemeKind.ORIGINAL, darkSystem)

        // Then
        assertThat(variant).isEqualTo(BloxThemeVariant.ORIGINAL)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variants")
    fun `each variant has its palette and its Material colour scheme, light or dark`(
        variant: BloxThemeVariant,
        palette: BloxPalette,
        dark: Boolean,
    ) {
        // When
        val scheme = colorSchemeOf(variant)

        // Then the Material components take the colours of the palette, on the base of their mode
        assertThat(paletteOf(variant)).isEqualTo(palette)
        assertThat(scheme.primary).isEqualTo(palette.accent)
        assertThat(scheme.background).isEqualTo(palette.page)
        assertThat(scheme.onBackground).isEqualTo(palette.ink)
        assertThat(scheme.surface).isEqualTo(palette.panel)
        assertThat(scheme.onSurface).isEqualTo(palette.ink)
        assertThat(scheme.onPrimary).isEqualTo(palette.accentInk)
        assertThat(scheme.error).isEqualTo(if (dark) darkColorScheme().error else lightColorScheme().error)
    }

    private companion object {
        @JvmStatic
        fun variants(): List<Arguments> = listOf(
            Arguments.of(BloxThemeVariant.ORIGINAL, BloxPalettes.Original, false),
            Arguments.of(BloxThemeVariant.COLORFUL_LIGHT, BloxPalettes.ColorfulLight, false),
            Arguments.of(BloxThemeVariant.COLORFUL_DARK, BloxPalettes.ColorfulDark, true),
        )
    }
}
