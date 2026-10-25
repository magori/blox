package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The reader of font files that the tests of the digits rely on, checked on the embedded fonts. */
class OpenTypeFontTest {
    @Test
    fun `a character that the font does not draw takes the missing glyph 0`() {
        // Given the pixel font, which has no Chinese
        val font = embeddedFont("press_start_2p_regular")

        // When
        val glyph = font.glyph('中')

        // Then
        assertThat(glyph).isEqualTo(0)
    }

    @Test
    fun `the advance of a glyph varies by region of the axes, a region without delta not counted`() {
        // Given Manrope, whose proportional 0 widens with the weight and whose tabular 0 does not
        val font = embeddedFont("manrope_variable")
        val zero = font.glyph('0')

        // When
        val proportional = font.advanceVariations(zero)
        val tabular = font.advanceVariations(font.substitute("tnum", zero))

        // Then the first region of the font (its only one, wght 200 to 800) widens the 0 by 192 units
        assertThat(proportional).isEqualTo(mapOf(0 to 192))
        assertThat(tabular).isEmpty()
    }
}
