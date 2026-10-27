package ch.magori.blox.core.designsystem.frame

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

class FrameTicksTest {
    @ParameterizedTest(name = "cap per frame: {0}")
    @NullSource
    @ValueSource(ints = [4])
    fun `the first frame only sets the time reference`(maxTicksPerFrame: Int?) {
        // Given
        val ticks = FrameTicks(maxTicksPerFrame)

        // When
        val first = ticks.advance(frameNanos = 5_000_000_000)

        // Then
        assertThat(first).isZero()
    }

    @Test
    fun `a frame one sixtieth of a second later gives one tick`() {
        // Given
        val ticks = startedAtZero()

        // When
        val next = ticks.advance(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(next).isEqualTo(1)
    }

    @Test
    fun `three frames of 16 point 7 ms give three ticks in total`() {
        // Given
        val ticks = startedAtZero()

        // When
        val total = listOf(16_700_000L, 33_400_000L, 50_100_000L).sumOf { ticks.advance(frameNanos = it) }

        // Then
        assertThat(total).isEqualTo(3)
    }

    @Test
    fun `the remainder is carried over so two frames of 10 ms give zero then one tick`() {
        // Given
        val ticks = startedAtZero()

        // When
        val first = ticks.advance(frameNanos = 10_000_000L)
        val second = ticks.advance(frameNanos = 20_000_000L)

        // Then
        assertThat(first).isZero()
        assertThat(second).isEqualTo(1)
    }

    @Test
    fun `a frame of 50 ms gives three ticks`() {
        // Given
        val ticks = startedAtZero()

        // When
        val next = ticks.advance(frameNanos = 50_000_000L)

        // Then
        assertThat(next).isEqualTo(3)
    }

    @Test
    fun `there is no drift so sixty frames of one sixtieth of a second give exactly sixty ticks`() {
        // Given frame times as a real clock gives them: whole nanoseconds, one second after the start
        val ticks = startedAtZero()
        val frameTimes = (1..60).map { frame -> frame * 1_000_000_000L / 60 }

        // When
        val total = frameTimes.sumOf { ticks.advance(frameNanos = it) }

        // Then
        assertThat(total).isEqualTo(60)
    }

    @Test
    fun `each frame gives the whole ticks of 1 60th of a second since the frame before, the rest carried over`() {
        // Given no cap per frame, as for the play head of the editor
        val ticks = startedAtZero(maxTicksPerFrame = null)

        // When 25 ms, then 25 ms more, then a second late, as after a frame the system skipped
        val first = ticks.advance(frameNanos = 25_000_000)
        val second = ticks.advance(frameNanos = 50_000_000)
        val late = ticks.advance(frameNanos = 1_050_000_000)

        // Then 1.5 ticks, then 1.5 more, then all the 60 ticks of the second: none is dropped
        assertThat(listOf(first, second, late)).containsExactly(1, 2, 60)
    }

    @Test
    fun `a frame of 510 ms gives at most four ticks, the surplus ticks are dropped and the fraction is kept`() {
        // Given
        val ticks = startedAtZero(maxTicksPerFrame = 4)

        // When: 510 ms are 30.6 ticks; 30 whole ticks are capped to 4, the 0.6 tick is kept
        val longFrame = ticks.advance(frameNanos = 510_000_000L)
        // and 10 ms more are 0.6 tick, so 0.6 + 0.6 gives one tick
        val nextFrame = ticks.advance(frameNanos = 520_000_000L)

        // Then
        assertThat(longFrame).isEqualTo(4)
        assertThat(nextFrame).isEqualTo(1)
    }

    @Test
    fun `the cap per frame is set by the constructor so a cap of 2 limits a 100 ms frame to two ticks`() {
        // Given
        val ticks = startedAtZero(maxTicksPerFrame = 2)

        // When
        val next = ticks.advance(frameNanos = 100_000_000L)

        // Then
        assertThat(next).isEqualTo(2)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a cap per frame of zero or less is refused`(maxTicksPerFrame: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { FrameTicks(maxTicksPerFrame) }
    }

    @Test
    fun `after a reset the next frame sets the time reference again and gives no tick`() {
        // Given frame ticks with a pending remainder
        val ticks = startedAtZero()
        ticks.advance(frameNanos = 10_000_000L)

        // When
        ticks.reset()
        val afterReset = ticks.advance(frameNanos = 5_000_000_000L)
        val nextFrame = ticks.advance(frameNanos = 5_010_000_000L)

        // Then
        assertThat(afterReset).isZero()
        assertThat(nextFrame).isZero()
    }

    @ParameterizedTest(name = "cap per frame: {0}")
    @NullSource
    @ValueSource(ints = [4])
    fun `a frame time going back takes no tick away, and the ticks go on from it`(maxTicksPerFrame: Int?) {
        // Given
        val ticks = FrameTicks(maxTicksPerFrame)
        ticks.advance(frameNanos = 1_000_000_000)

        // When a frame half a second earlier, then a sixtieth of a second after it
        val back = ticks.advance(frameNanos = 500_000_000)
        val next = ticks.advance(frameNanos = 500_000_000 + ONE_SIXTIETH_NANOS)

        // Then no tick, and no debt left that would take the tick of the next frame
        assertThat(listOf(back, next)).containsExactly(0, 1)
    }

    /** Frame ticks whose first frame, at time zero, has already set the time reference. */
    private fun startedAtZero(maxTicksPerFrame: Int? = 4): FrameTicks =
        FrameTicks(maxTicksPerFrame).apply { advance(frameNanos = 0L) }

    private companion object {
        /** 1/60 s rounded up to the next nanosecond: 16 666 666.67 ns. */
        const val ONE_SIXTIETH_NANOS = 16_666_667L
    }

    @Test
    fun `ticks are turned into milliseconds for the animations of Compose, a fraction dropped`() {
        assertThat(millisOf(ticks = 12)).isEqualTo(200)
        assertThat(millisOf(ticks = 18)).isEqualTo(300)
        assertThat(millisOf(ticks = 8)).isEqualTo(133)
    }
}
