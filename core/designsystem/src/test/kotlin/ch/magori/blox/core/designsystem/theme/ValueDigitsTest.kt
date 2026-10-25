package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ValueDigitsTest {
    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `in each theme the ten digits of a value take the same width, so that a value does not move`(
        kind: BloxThemeKind,
    ) {
        // Given each style of numbers of the theme, with its embedded font and the features it turns on
        digitStyles(BloxTypographies.of(kind)).forEach { (name, style) ->
            val font = embeddedFont(fontFileOf(style))
            val tabular = TABULAR in featuresOn(style)

            // Then each digit is as wide as the others, at every weight of the font
            val widths = DIGITS.map { digit ->
                val drawn = font.glyph(digit)
                assertThat(drawn).withFailMessage { "${fontFileOf(style)} has no glyph for $digit" }.isNotZero()
                val glyph = if (tabular) font.substitute(TABULAR, drawn) else drawn
                Width(advance = font.advance(glyph), variations = font.advanceVariations(glyph))
            }
            assertThat(widths.distinct()).withFailMessage { "The digits of $name have ${widths.distinct()}" }.hasSize(1)
        }
    }

    @Test
    fun `in the Colorful theme the values take the tabular digits of Manrope, its tnum feature`() {
        // Given the style of the values of the Colorful theme and its font
        val style = BloxTypographies.Colorful.value
        val font = embeddedFont(fontFileOf(style))

        // Then the font has tnum, and the style turns it on
        assertThat(font.features).contains(TABULAR)
        assertThat(featuresOn(style)).contains(TABULAR)
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `in each theme the values are laid out without kerning, which would bring two digits closer`(
        kind: BloxThemeKind,
    ) {
        // Given each style of numbers of the theme
        digitStyles(BloxTypographies.of(kind)).forEach { (name, style) ->
            // Then it turns the kerning of the font off
            assertThat(style.fontFeatureSettings?.split(',')?.map { it.trim() }).withFailMessage { "$name is kerned" }
                .contains("\"kern\" 0")
        }
    }

    /** The styles of [typography] that lay numbers out: the values, and the styles of tokens json marked digits. */
    private fun digitStyles(typography: BloxTypography): Map<String, TextStyle> = mapOf("value" to typography.value) +
        TypographyTokensTest.codedStyles().filter { it.digits }.associate { it.name to it.read(typography) }

    /** A width of a glyph: its advance at the default instance, and how it varies with the weight. */
    private data class Width(val advance: Int, val variations: Map<Int, Int>)

    /** The features that [style] turns on, as CSS writes them: `"tnum"` or `"tnum" 1`. */
    private fun featuresOn(style: TextStyle): List<String> = style.fontFeatureSettings.orEmpty().split(',')
        .map { it.trim().split(' ') }
        .filter { it.size == 1 || it[1] == "1" }
        .map { it[0].trim('"') }

    private companion object {
        const val DIGITS = "0123456789"
        const val TABULAR = "tnum"
    }
}
