package ch.magori.blox.feature.composer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrameTicksTest {
    @Test
    fun `the first frame only sets the time reference`() {
        assertThat(FrameTicks().advance(frameNanos = 5_000_000_000)).isZero()
    }

    @Test
    fun `each frame gives the whole ticks of 1 60th of a second since the frame before, the rest carried over`() {
        // Given
        val ticks = FrameTicks()
        ticks.advance(frameNanos = 0)

        // When 25 ms, then 25 ms more, then a second late, as after a frame the system skipped
        val first = ticks.advance(frameNanos = 25_000_000)
        val second = ticks.advance(frameNanos = 50_000_000)
        val late = ticks.advance(frameNanos = 1_050_000_000)

        // Then 1.5 ticks, then 1.5 more, then all the 60 ticks of the second: none is dropped
        assertThat(listOf(first, second, late)).containsExactly(1, 2, 60)
    }

    @Test
    fun `a frame time going back takes no tick away, and the ticks go on from it`() {
        // Given
        val ticks = FrameTicks()
        ticks.advance(frameNanos = 1_000_000_000)

        // When a frame half a second earlier, then a sixtieth of a second after it
        val back = ticks.advance(frameNanos = 500_000_000)
        val next = ticks.advance(frameNanos = 500_000_000 + 1_000_000_000L / 60 + 1)

        // Then
        assertThat(listOf(back, next)).containsExactly(0, 1)
    }
}
