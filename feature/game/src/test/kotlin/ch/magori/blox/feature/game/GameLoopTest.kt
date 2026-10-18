package ch.magori.blox.feature.game

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GameLoopTest {
    @Test
    fun `the first frame gives no tick because it sets the time reference`() {
        // Given
        val loop = GameLoop()

        // When
        val ticks = loop.advance(frameNanos = 123_456_789L)

        // Then
        assertThat(ticks).isZero()
    }

    @Test
    fun `a frame one sixtieth of a second later gives one tick`() {
        // Given
        val loop = loopStartedAtZero()

        // When
        val ticks = loop.advance(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(ticks).isEqualTo(1)
    }

    @Test
    fun `three frames of 16 point 7 ms give three ticks in total`() {
        // Given
        val loop = loopStartedAtZero()

        // When
        val ticks = listOf(16_700_000L, 33_400_000L, 50_100_000L).sumOf { loop.advance(frameNanos = it) }

        // Then
        assertThat(ticks).isEqualTo(3)
    }

    @Test
    fun `the remainder is carried over so two frames of 10 ms give zero then one tick`() {
        // Given
        val loop = loopStartedAtZero()

        // When
        val first = loop.advance(frameNanos = 10_000_000L)
        val second = loop.advance(frameNanos = 20_000_000L)

        // Then
        assertThat(first).isZero()
        assertThat(second).isEqualTo(1)
    }

    @Test
    fun `a frame of 50 ms gives three ticks`() {
        // Given
        val loop = loopStartedAtZero()

        // When
        val ticks = loop.advance(frameNanos = 50_000_000L)

        // Then
        assertThat(ticks).isEqualTo(3)
    }

    @Test
    fun `there is no drift so sixty frames of one sixtieth of a second give exactly sixty ticks`() {
        // Given frame times as a real clock gives them: whole nanoseconds, one second after the start
        val loop = loopStartedAtZero()
        val frameTimes = (1..60).map { frame -> frame * 1_000_000_000L / 60 }

        // When
        val ticks = frameTimes.sumOf { loop.advance(frameNanos = it) }

        // Then
        assertThat(ticks).isEqualTo(60)
    }

    @Test
    fun `a frame of 510 ms gives at most four ticks, the surplus ticks are dropped and the fraction is kept`() {
        // Given
        val loop = loopStartedAtZero(maxTicksPerFrame = 4)

        // When: 510 ms are 30.6 ticks; 30 whole ticks are capped to 4, the 0.6 tick is kept
        val longFrame = loop.advance(frameNanos = 510_000_000L)
        // and 10 ms more are 0.6 tick, so 0.6 + 0.6 gives one tick
        val nextFrame = loop.advance(frameNanos = 520_000_000L)

        // Then
        assertThat(longFrame).isEqualTo(4)
        assertThat(nextFrame).isEqualTo(1)
    }

    @Test
    fun `the cap per frame is set by the constructor so a cap of 2 limits a 100 ms frame to two ticks`() {
        // Given
        val loop = loopStartedAtZero(maxTicksPerFrame = 2)

        // When
        val ticks = loop.advance(frameNanos = 100_000_000L)

        // Then
        assertThat(ticks).isEqualTo(2)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a cap per frame of zero or less is refused`(maxTicksPerFrame: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { GameLoop(maxTicksPerFrame) }
    }

    @Test
    fun `after a reset the next frame sets the time reference again and gives no tick`() {
        // Given a loop with a pending remainder
        val loop = loopStartedAtZero()
        loop.advance(frameNanos = 10_000_000L)

        // When
        loop.reset()
        val afterReset = loop.advance(frameNanos = 5_000_000_000L)
        val nextFrame = loop.advance(frameNanos = 5_010_000_000L)

        // Then
        assertThat(afterReset).isZero()
        assertThat(nextFrame).isZero()
    }

    @Test
    fun `an earlier frame gives no tick and leaves no debt`() {
        // Given
        val loop = loopStartedAtZero()
        loop.advance(frameNanos = 20_000_000L)

        // When
        val earlierFrame = loop.advance(frameNanos = 10_000_000L)
        val nextFrame = loop.advance(frameNanos = 10_000_000L + ONE_SIXTIETH_NANOS)

        // Then
        assertThat(earlierFrame).isZero()
        assertThat(nextFrame).isEqualTo(1)
    }

    /** A loop whose first frame, at time zero, has already set the time reference. */
    private fun loopStartedAtZero(maxTicksPerFrame: Int = 4): GameLoop =
        GameLoop(maxTicksPerFrame).apply { advance(frameNanos = 0L) }

    private companion object {
        /** 1/60 s rounded up to the next nanosecond: 16 666 666.67 ns. */
        const val ONE_SIXTIETH_NANOS = 16_666_667L
    }
}
