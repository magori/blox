package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GaugeBarsTest {
    @Test
    fun `each bar of the gauge is 8 dp wide and from 8 to 26 dp high, in accent up to the value`() {
        // Given the level 3 of 0 to 9, by day in Couleurs
        val palette = BloxPalettes.ColorfulLight

        // When
        val bars = gaugeBars(SteppedValue(3, 0..9), palette)

        // Then ten bars of 8 dp, 2 dp higher each, the first four in accent
        assertThat(bars.map { it.width }).containsOnly(8.dp).hasSize(10)
        assertThat(bars.map { it.height }).containsExactlyElementsOf((8..26 step 2).map { it.dp })
        assertThat(bars.map { it.color })
            .containsExactlyElementsOf(List(4) { palette.accent } + List(6) { palette.panelBorder })
    }

    @Test
    fun `in Original, whose panel border is its accent, the bars beyond the value are in button`() {
        // Given
        val palette = BloxPalettes.Original

        // When
        val bars = gaugeBars(SteppedValue(3, 0..9), palette)

        // Then the bars beyond the value stand apart from those up to it
        assertThat(bars.map { it.color })
            .containsExactlyElementsOf(List(4) { palette.accent } + List(6) { palette.button })
    }

    @Test
    fun `the bars up to the value are filled and those beyond it hollow, so the value does not read by colour alone`() {
        // When
        val bars = gaugeBars(SteppedValue(3, 0..9), BloxPalettes.ColorfulLight)

        // Then
        assertThat(bars.map { it.filled }).containsExactlyElementsOf(List(4) { true } + List(6) { false })
    }
}
