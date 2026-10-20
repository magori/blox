package ch.magori.blox.core.designsystem.theme

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FewestLinesSizeTest {
    @Test
    fun `a text that fits on one line keeps one line even at a smaller size than on several`() {
        val size = fewestLinesSize(
            smallest = 8f,
            largest = 14f,
            step = 0.25f,
            fitsOnOneLine = { it <= 10f },
            fitsOnLines = { it <= 14f },
        )

        assertThat(size).isEqualTo(10f)
    }

    @Test
    fun `a text that does not fit on one line even at the smallest size wraps on several lines`() {
        val size = fewestLinesSize(
            smallest = 8f,
            largest = 14f,
            step = 0.25f,
            fitsOnOneLine = { false },
            fitsOnLines = { it <= 12f },
        )

        assertThat(size).isEqualTo(12f)
    }

    @Test
    fun `a text that fits nowhere takes the smallest size`() {
        val size = fewestLinesSize(
            smallest = 8f,
            largest = 14f,
            step = 0.25f,
            fitsOnOneLine = { false },
            fitsOnLines = { false },
        )

        assertThat(size).isEqualTo(8f)
    }

    @Test
    fun `the lines are never tried when one line is enough`() {
        var linesTried = 0

        fewestLinesSize(
            smallest = 8f,
            largest = 14f,
            step = 0.25f,
            fitsOnOneLine = { it <= 10f },
            fitsOnLines = {
                linesTried++
                true
            },
        )

        assertThat(linesTried).isZero()
    }
}
