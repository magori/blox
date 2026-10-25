package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test

/**
 * The roles that came with the Colorful theme v2 and that tokens json gives to the day and the night only: their
 * Original values are those of docs/themes.md (P5.56), to be confirmed on the mock-up (P5.85).
 */
class OriginalPaletteTest {
    @Test
    fun `the roles new in v2 have in the Original theme the values of docs themes md`() {
        // Given
        val palette = BloxPalettes.Original

        // Then, as docs themes md writes them
        assertSoftly { softly ->
            softly.assertThat(palette.emptyCell).`as`("emptyCell").isEqualTo(cssColor("#DCEBD5"))
            softly.assertThat(palette.panelInset).`as`("panelInset").isEqualTo(cssColor("#DCEBD5"))
            softly.assertThat(palette.scrim).`as`("scrim").isEqualTo(cssColor("rgba(207,227,200,0.85)"))
            softly.assertThat(palette.clearGlow).`as`("clearGlow").isEqualTo(cssColor("#EEF5EA"))
            softly.assertThat(palette.dangerSoft).`as`("dangerSoft").isEqualTo(cssColor("rgba(52,80,58,0.14)"))
            softly.assertThat(palette.cellDead).`as`("cellDead").isEqualTo(cssColor("#BFD6B6"))
            // The iris is replaced by the flat dark green, and the accent has no ring
            softly.assertThat(palette.iris).`as`("iris").isEqualTo(IrisColors(DARK, DARK, DARK))
            softly.assertThat(palette.accentRing).`as`("accentRing").isEqualTo(Color.Transparent)
        }
    }

    private companion object {
        val DARK = cssColor("#34503A")
    }
}
