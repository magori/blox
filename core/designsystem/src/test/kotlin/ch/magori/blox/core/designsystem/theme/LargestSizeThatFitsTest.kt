package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.math.log2

class LargestSizeThatFitsTest {
    @Test
    fun `a text that fits at its largest size keeps it`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 14f, step = 0.25f) { true }

        assertThat(size).isEqualTo(14f)
    }

    @Test
    fun `a text too large shrinks to the largest step from the smallest size that fits`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 14f, step = 0.25f) { it <= 11.1f }

        assertThat(size).isEqualTo(11f)
    }

    @Test
    fun `a text that does not fit even at the smallest size has no size that fits`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 14f, step = 0.25f) { it < 8f }

        assertThat(size).isNull()
    }

    @Test
    fun `a largest size off the steps is kept when the text fits at it`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 14.1f, step = 0.25f) { true }

        assertThat(size).isEqualTo(14.1f)
    }

    @Test
    fun `below a largest size off the steps a text takes the largest step from the smallest size that fits`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 14.1f, step = 0.25f) { it < 14.1f }

        assertThat(size).isEqualTo(14f)
    }

    @Test
    fun `with the smallest size equal to the largest a text that fits takes it`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 8f, step = 0.25f) { it <= 8f }

        assertThat(size).isEqualTo(8f)
    }

    @Test
    fun `with the smallest size equal to the largest a text that does not fit has no size that fits`() {
        val size = largestSizeThatFits(smallest = 8f, largest = 8f, step = 0.25f) { it < 8f }

        assertThat(size).isNull()
    }

    @Test
    fun `a text that fits only at the smallest size takes exactly the smallest size`() {
        val size = largestSizeThatFits(smallest = 7.5f, largest = 14f, step = 0.25f) { it <= 7.5f }

        assertThat(size).isEqualTo(7.5f)
    }

    @Test
    fun `the sizes tried grow with the logarithm of the number of steps`() {
        val steps = 4_000
        var tries = 0

        largestSizeThatFits(smallest = 0f, largest = steps * 0.25f, step = 0.25f) {
            tries++
            it <= 333.3f
        }

        // The largest and the smallest sizes, then one halving of the steps left at a time.
        assertThat(tries).isLessThanOrEqualTo(2 + ceil(log2(steps + 1.0)).toInt())
    }

    @Test
    fun `when fitting is not monotonic, as a text that wraps may be, the size given was tried and fits`() {
        // Fits at the smallest size and between 11 and 12, not between: a later line break may free some width.
        val fitting = { size: Float -> size <= 8f || size in 11f..12f }
        val tried = mutableListOf<Float>()

        val size = largestSizeThatFits(smallest = 8f, largest = 14f, step = 0.25f) {
            tried += it
            fitting(it)
        }

        assertThat(size).isNotNull()
        assertThat(tried).contains(size)
        assertThat(fitting(checkNotNull(size))).isTrue()
    }
}
