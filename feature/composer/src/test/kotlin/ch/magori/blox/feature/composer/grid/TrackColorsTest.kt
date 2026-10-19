package ch.magori.blox.feature.composer.grid

import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.namedPalettes
import ch.magori.blox.feature.composer.model.Track
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class TrackColorsTest {
    @ParameterizedTest(name = "{0}, {2}")
    @MethodSource("themesAndTracks")
    fun `the attack and the held part of a note stand out from the cell by a contrast of 3 to 1 at least`(
        theme: String,
        palette: BloxPalette,
        track: Track,
    ) {
        // When
        val colors = trackColors(palette, track)

        // Then (WCAG 2.1, 1.4.11: graphical objects)
        assertThat(contrast(colors.note, palette.boardBackground)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(colors.held, palette.boardBackground)).isGreaterThanOrEqualTo(3.0)
    }

    @ParameterizedTest(name = "{0}, {2}")
    @MethodSource("themesAndTracks")
    fun `the held part of a note is paler than its attack, standing out less from the cell`(
        theme: String,
        palette: BloxPalette,
        track: Track,
    ) {
        // When
        val colors = trackColors(palette, track)

        // Then
        assertThat(contrast(colors.held, palette.boardBackground))
            .isLessThan(contrast(colors.note, palette.boardBackground))
    }

    @ParameterizedTest(name = "{0}, {2}")
    @MethodSource("themesAndTracks")
    fun `the selected tab of a track stands out from the unselected one, its label readable on it`(
        theme: String,
        palette: BloxPalette,
        track: Track,
    ) {
        // When
        val colors = trackColors(palette, track)

        // Then (WCAG 2.1, 1.4.11 and 1.4.1: the selected tab differs from a framed one in luminance; 1.4.3: its label)
        assertThat(contrast(colors.tab, palette.panel)).isGreaterThanOrEqualTo(3.0)
        assertThat(contrast(colors.tabInk, colors.tab)).isGreaterThanOrEqualTo(4.5)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("themes")
    fun `the name of a line out of the scale is dimmer than the others, and still readable by a contrast of 4,5 to 1`(
        theme: String,
        palette: BloxPalette,
    ) {
        // Then (WCAG 2.1, 1.4.3: text)
        assertThat(contrast(palette.outOfScale, palette.page)).isGreaterThanOrEqualTo(4.5)
        assertThat(contrast(palette.outOfScale, palette.page)).isLessThan(contrast(palette.label, palette.page))
    }

    companion object {
        @JvmStatic
        fun themes(): List<Arguments> = namedPalettes.map { (theme, palette) -> Arguments.of(theme, palette) }

        @JvmStatic
        fun themesAndTracks(): List<Arguments> = namedPalettes.flatMap { (theme, palette) ->
            Track.entries.map { Arguments.of(theme, palette, it) }
        }
    }
}
