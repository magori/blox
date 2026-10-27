package ch.magori.blox.feature.game.controls

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class KeyColorsTest {
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("allKeys")
    fun `a key at rest stands out from the page by its outline, by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val colors = keyColors(palette, button, held = false)

        // Then (WCAG 2.1, 1.4.11: the edge of the key, drawn over its face, tells where the key is)
        assertThat(contrast(colors.outline, palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keys")
    fun `a held key keeps the outline it has at rest`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val held = keyColors(palette, button, held = true)
        val notHeld = keyColors(palette, button, held = false)

        // Then pressing changes the face only: the key keeps the edge that draws it on the page
        assertThat(held.outline).isEqualTo(notHeld.outline)
    }

    @ParameterizedTest(name = "{0}, held: {2}")
    @MethodSource("palettesHeldOrNot")
    fun `the key A is framed in the pressed shade of the accent, held or not`(
        theme: String,
        palette: BloxPalette,
        held: Boolean,
    ) {
        // When
        val colors = keyColors(palette, ControlButton.A, held)

        // Then its edge stands out from the page where the accent alone does not (2.3:1 by day)
        assertThat(colors.outline).isEqualTo(palette.accentPressed)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keys")
    fun `a held key stands out from the page by its outline, by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
    ) {
        // When
        val colors = keyColors(palette, button, held = true)

        // Then (WCAG 2.1, 1.4.11: the edge of the key, kept while it is held, tells where the key is)
        assertThat(contrast(colors.outline, palette.page)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("keysButTheAOfTheDay")
    fun `a held key stands out from the key at rest by a contrast of 1,5 to 1 at least`(
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

    @Test
    fun `for A in the day the held key differs from it at rest by less than 1,5 to 1, so its 2 dp drop tells it`() {
        // When
        val held = keyColors(BloxPalettes.ColorfulLight, ControlButton.A, held = true)
        val notHeld = keyColors(BloxPalettes.ColorfulLight, ControlButton.A, held = false)

        // Then its pressed lilac cannot stand 1.5:1 from the accent and 4.5:1 under the ink at once: 1.41:1 between
        // #9A6CF0 and #B48CFF. The drop, which is no animation and stays in reduced effects, tells it (KeyDropTest).
        assertThat(held.background).isEqualTo(Color(0xFF9A6CF0))
        assertThat(notHeld.background).isEqualTo(Color(0xFFB48CFF))
        assertThat(contrast(held.background, notHeld.background)).isCloseTo(1.41, within(0.01))
        assertThat(keyDrop(held = true)).isEqualTo(2.dp)
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
    @MethodSource("allKeysHeldOrNot")
    fun `the glyph of a key stands out from its face, by a contrast of 3 to 1 at least`(
        theme: String,
        button: ControlButton,
        palette: BloxPalette,
        held: Boolean,
    ) {
        // When
        val colors = keyColors(palette, button, held)

        // Then (WCAG 2.1, 1.4.11: the glyph that tells the key, a triangle or a pictogram of rotation, alone on B and
        // A since Couleurs v2: their letter is left to TalkBack, which reads it in the name of the key)
        assertThat(contrast(colors.ink, colors.background)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        /** A key drawn in `button`, the one drawn in `accent`, in each theme. */
        @JvmStatic
        fun keys(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            listOf(ControlButton.LEFT, ControlButton.A).map { Arguments.of(theme, it, palette) }
        }

        /** The keys of [keys] but the A of the day, which a held key tells by its drop rather than by its face. */
        @JvmStatic
        fun keysButTheAOfTheDay(): List<Arguments> = keys().filterNot { key ->
            key.get()[1] == ControlButton.A && key.get()[2] == BloxPalettes.ColorfulLight
        }

        @JvmStatic
        fun allKeysHeldOrNot(): List<Arguments> = allKeys().flatMap { key ->
            listOf(true, false).map { Arguments.of(*key.get(), it) }
        }

        /** Each theme, a key held or not. */
        @JvmStatic
        fun palettesHeldOrNot(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            listOf(true, false).map { Arguments.of(theme, palette, it) }
        }

        /** Every key, in each theme. */
        @JvmStatic
        fun allKeys(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            ControlButton.entries.map { Arguments.of(theme, it, palette) }
        }
    }
}
