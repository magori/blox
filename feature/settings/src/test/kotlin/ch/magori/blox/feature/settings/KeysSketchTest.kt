package ch.magori.blox.feature.settings

import androidx.compose.ui.geometry.Offset
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.contrast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

internal class KeysSketchTest {
    @Test
    fun `the sketch of the keys shows the arrows on the left, the down one lower, B lower and A higher on the right`() {
        // When
        val sketch = keysSketch(leftHanded = false)

        // Then, on its grid of 40 × 24 (docs/design-system/pictogrammes.md, KEYS_SKETCH), ▼ a third of an arrow lower
        assertThat(sketch.arrows).containsExactly(Offset(0f, 10f), Offset(9f, 13f), Offset(18f, 10f))
        assertThat(sketch.b).isEqualTo(Offset(33f, 17f))
        assertThat(sketch.a).isEqualTo(Offset(35f, 7f))
    }

    @Test
    fun `for the left hand the sketch turns over, as in a mirror`() {
        // When
        val sketch = keysSketch(leftHanded = true)

        // Then B and A on the left, A outside, the arrows on the right
        assertThat(sketch.arrows).containsExactly(Offset(14f, 10f), Offset(23f, 13f), Offset(32f, 10f))
        assertThat(sketch.b).isEqualTo(Offset(7f, 17f))
        assertThat(sketch.a).isEqualTo(Offset(5f, 7f))
    }

    @ParameterizedTest(name = "left handed: {0}")
    @ValueSource(booleans = [false, true])
    fun `B stands 2 units clear of the arrows, A clear of B, both within the sketch`(leftHanded: Boolean) {
        // When
        val sketch = keysSketch(leftHanded)

        // Then (squares of 8, discs of radius 5)
        val nearest = sketch.arrows.minOf { minOf(abs(it.x - sketch.b.x), abs(it.x + 8 - sketch.b.x)) }
        assertThat(nearest - 5).isGreaterThanOrEqualTo(2f)
        assertThat((sketch.a - sketch.b).getDistance()).isGreaterThan(10f)
        listOf(sketch.a, sketch.b).forEach { assertThat(it.x).isBetween(5f, 35f) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("colorful")
    fun `the A of the sketch is in the pressed accent, which stands out from its card by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val a = sketchAccent(palette)

        // Then (WCAG 2.1, 1.4.11: a graphic that tells the setting; the accent of the day stands out by 2,1 to 1 only)
        assertThat(a).isEqualTo(palette.accentPressed)
        assertThat(contrast(a, palette.panel)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        /** Couleurs, by day and by night. */
        @JvmStatic
        fun colorful(): List<Arguments> = listOf(
            Arguments.of("Couleurs, jour", BloxPalettes.ColorfulLight),
            Arguments.of("Couleurs, nuit", BloxPalettes.ColorfulDark),
        )
    }
}
