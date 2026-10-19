package ch.magori.blox.feature.game.controls

import androidx.compose.ui.text.font.FontWeight
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class KeyColorsTest {
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keys")
    fun `a held key stands out from the page, by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val colors = keyColors(palette, button, held = true)

        // Then (WCAG 2.1, 1.4.11: user interface components)
        assertThat(contrast(colors.background, palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keys")
    fun `a held key stands out from the same key not held, by a contrast of 1,5 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val held = keyColors(palette, button, held = true)
        val notHeld = keyColors(palette, button, held = false)

        // Then the player sees at a glance which keys are held
        assertThat(contrast(held.background, notHeld.background)).isGreaterThanOrEqualTo(1.5)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keys")
    fun `a held key looks other than a key not held`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val held = keyColors(palette, button, held = true)
        val notHeld = keyColors(palette, button, held = false)

        // Then
        assertThat(held.background).isNotEqualTo(notHeld.background)
    }

    @ParameterizedTest(name = "{0}, {1}, held: {3}")
    @MethodSource("keysHeldOrNot")
    fun `the glyph of a key stands out from its face, by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
        held: Boolean,
    ) {
        // When
        val colors = keyColors(palette, button, held)

        // Then (WCAG 2.1, 1.4.11: the glyph that tells the key, an arrow or a letter)
        assertThat(contrast(colors.ink, colors.background)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, {1}, held: {3}")
    @MethodSource("lettersHeldOrNot")
    fun `the letters A and B, large text, stand out from their key by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
        held: Boolean,
    ) {
        // When
        val colors = keyColors(palette, button, held)

        // Then (WCAG 2.1, 1.4.3: text; large text, as the letters are, asks for 3:1)
        assertThat(contrast(colors.ink, colors.background)).isGreaterThanOrEqualTo(3.0)
    }

    @Test
    fun `the letters A and B are large text, bold and of 18,66 sp at least`() {
        // Then (WCAG 2.1: 14 pt bold, 18.66 sp, is large text)
        assertThat(KeyLetterStyle.fontWeight ?: FontWeight.Normal).isGreaterThanOrEqualTo(FontWeight.Bold)
        assertThat(KeyLetterStyle.fontSize.value).isGreaterThanOrEqualTo(18.66f)
    }

    companion object {
        /** A key drawn in `button`, the one drawn in `accent`, in each theme. */
        @JvmStatic
        fun keys(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            listOf(ControlButton.LEFT, ControlButton.A).map { Arguments.of(theme, it, palette) }
        }

        @JvmStatic
        fun keysHeldOrNot(): List<Arguments> = keys().flatMap { key ->
            listOf(true, false).map { Arguments.of(*key.get(), it) }
        }

        /** The keys with a letter, A and B, held or not, in each theme. */
        @JvmStatic
        fun lettersHeldOrNot(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            listOf(ControlButton.A, ControlButton.B).flatMap { button ->
                listOf(true, false).map { Arguments.of(theme, button, palette, it) }
            }
        }
    }
}
