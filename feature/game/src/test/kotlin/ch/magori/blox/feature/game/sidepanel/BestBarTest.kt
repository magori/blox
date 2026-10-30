package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import ch.magori.blox.core.game.model.Points
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class BestBarTest {
    @ParameterizedTest(name = "{0} points of a best of {1}: {2}")
    @CsvSource("0, 1000, 0", "250, 1000, 0.25", "999, 1000, 0.999", "1000, 1000, 1", "1001, 1000, 1", "5000000, 1, 1")
    fun `the best bar fills with the score up to the best`(
        score: Int,
        best: Int,
        fill: Float,
    ) {
        // When
        val bar = bestBarFill(Points(score), best)

        // Then full once the score reaches the best, never beyond
        assertThat(bar).isEqualTo(fill)
    }

    @ParameterizedTest(name = "a best of {0}")
    @NullSource
    @ValueSource(ints = [0])
    fun `without a best the best bar is hidden`(best: Int?) {
        // When
        val bar = bestBarFill(Points(1200), best)

        // Then
        assertThat(bar).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("palettes")
    fun `the filled part of the best bar stands out from its track by 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
    ) {
        // When
        val colors = bestBarColors(palette)

        // Then (WCAG 2.1, 1.4.11: the fill tells how near the best is)
        assertThat(contrast(colors.fill, colors.track)).isGreaterThanOrEqualTo(3.0)
    }

    companion object {
        @JvmStatic
        fun palettes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }
    }
}
