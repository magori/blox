package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnitType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File

class ValueDigitsTest {
    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `in each theme the ten digits of a value take the same width, so that a value does not move`(
        kind: BloxThemeKind,
    ) {
        // Given the font of the values of the theme, as embedded
        val typography = BloxTypographies.of(kind)
        val font = embeddedFont(fontFileOf(typography.value))

        // When the ten digits are laid out as a value
        val text = typography.valueText(DIGITS)

        // Then each one, with the spacing the value gives it, is as wide as the others
        val widths = DIGITS.indices.map { index -> advanceInEm(font, DIGITS[index]) + letterSpacingInEm(text, index) }
        assertThat(widths).allSatisfy { width -> assertThat(width).isCloseTo(widths.max(), within(TOLERANCE_EM)) }
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    fun `in each theme the values are laid out without kerning, which would bring two digits closer`(
        kind: BloxThemeKind,
    ) {
        // Given the style of the values of the theme
        val style = BloxTypographies.of(kind).value

        // Then it turns the kerning of the font off
        assertThat(style.fontFeatureSettings).isEqualTo("\"kern\" 0")
    }

    /** The letter spacing that [text] gives to its character at [index], in em, 0 without any. */
    private fun letterSpacingInEm(
        text: AnnotatedString,
        index: Int,
    ): Float {
        val spacings = text.spanStyles.filter { index in it.start until it.end }.map { it.item.letterSpacing }
        assertThat(spacings).withFailMessage { "Character $index of \"${text.text}\" has ${spacings.size} spacings" }
            .hasSizeLessThanOrEqualTo(1)
        val spacing = spacings.singleOrNull() ?: return 0f
        assertThat(spacing.type).isEqualTo(TextUnitType.Em)
        return spacing.value
    }

    /** The advance of [char] in [font], in em, as hmtx gives it. */
    private fun advanceInEm(
        font: Font,
        char: Char,
    ): Float =
        font.createGlyphVector(FontRenderContext(null, false, true), char.toString()).getGlyphMetrics(0).advance /
            font.size2D

    /** The embedded font [name], read from res/font. */
    private fun embeddedFont(name: String): Font {
        val dir = checkNotNull(System.getProperty("blox.fontDir")) { "The build gives res/font in blox.fontDir" }
        return Font.createFont(Font.TRUETYPE_FONT, File(dir, "$name.ttf")).deriveFont(EM_SIZE)
    }

    private companion object {
        const val DIGITS = "0123456789"
        const val EM_SIZE = 1000f
        const val TOLERANCE_EM = 0.002f
    }
}
