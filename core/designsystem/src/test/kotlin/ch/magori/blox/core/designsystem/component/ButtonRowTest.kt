package ch.magori.blox.core.designsystem.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ButtonRowTest {
    @Test
    fun `a row shares its width less its gaps in proportion to the weights, in whole pixels`() {
        assertThat(rowShares(width = 104, gap = 4, weights = listOf(1f, 1f))).containsExactly(50, 50)
    }

    @Test
    fun `the pixel a rounding takes too many is given back by the first buttons`() {
        // Given 2.5 px a unit of weight: 3, 3.5 and 3.5 round to 3, 4 and 4, a pixel more than the 10 of the row
        assertThat(rowShares(width = 10, gap = 0, weights = listOf(1.2f, 1.4f, 1.4f))).containsExactly(2, 4, 4)
    }

    @Test
    fun `the pixel a rounding leaves out is given to the first buttons`() {
        // Given 3.33 px a unit of weight: three times 3, a pixel less than the 10 of the row
        assertThat(rowShares(width = 10, gap = 0, weights = listOf(1f, 1f, 1f))).containsExactly(4, 3, 3)
    }

    @Test
    fun `labels hold side by side when each holds in its share less the padding on each side`() {
        // Given shares of 50 px, 3 px of padding on each side: 44 px for a label
        val holds = holdSideBySide(width = 104, gap = 4, padding = 3, weights = listOf(1f, 1f), labels = listOf(44, 44))

        assertThat(holds).isTrue()
    }

    @Test
    fun `labels do not hold side by side when one is a pixel wider than its share less its padding`() {
        val holds = holdSideBySide(width = 104, gap = 4, padding = 3, weights = listOf(1f, 1f), labels = listOf(44, 45))

        assertThat(holds).isFalse()
    }
}
