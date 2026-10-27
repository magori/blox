package ch.magori.blox.feature.home

import androidx.compose.ui.geometry.Offset
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class KeysSketchTest {
    @Test
    fun `the sketch of the keys shows the arrows on the left, B lower and A higher on the right`() {
        // When
        val sketch = keysSketch(leftHanded = false)

        // Then, on its grid of 40 × 24 (docs/design-system/pictogrammes.md, KEYS_SKETCH)
        assertThat(sketch.arrows).containsExactly(0f, 9f, 18f)
        assertThat(sketch.b).isEqualTo(Offset(31f, 17f))
        assertThat(sketch.a).isEqualTo(Offset(35f, 7f))
    }

    @Test
    fun `for the left hand the sketch turns over, as in a mirror`() {
        // When
        val sketch = keysSketch(leftHanded = true)

        // Then B and A on the left, A outside, the arrows on the right
        assertThat(sketch.arrows).containsExactly(14f, 23f, 32f)
        assertThat(sketch.b).isEqualTo(Offset(9f, 17f))
        assertThat(sketch.a).isEqualTo(Offset(5f, 7f))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("palettes")
    fun `the A of the sketch stands out from its card by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val a = sketchAccent(palette)

        // Then (WCAG 2.1, 1.4.11: a graphic that tells the setting)
        assertThat(contrast(a, palette.panel)).isGreaterThanOrEqualTo(3.0)
    }

    @Test
    fun `the A of the sketch is in the pressed accent by day and in the accent by night`() {
        // Then: the accent of the day would stand out from its card by 2,1 to 1 only
        assertThat(sketchAccent(BloxPalettes.ColorfulLight)).isEqualTo(BloxPalettes.ColorfulLight.accentPressed)
        assertThat(sketchAccent(BloxPalettes.ColorfulDark)).isEqualTo(BloxPalettes.ColorfulDark.accent)
    }

    companion object {
        @JvmStatic
        fun palettes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }
    }
}
