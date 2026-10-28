package ch.magori.blox.core.designsystem.frame

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FramePlayHeadTest {
    @Test
    fun `the first frame only sets the time, the next ones move the play head on at the tempo`() {
        // Given a loop of 16 eighths at 120 bpm: an eighth lasts a quarter of a second, 15 ticks
        val playHead = FramePlayHead(eighthsPerLoop = 16)

        // When
        val first = playHead.advance(frameNanos = 5 * SECOND, beatsPerMinute = 120).eighth
        val then = playHead.advance(frameNanos = 5 * SECOND + SECOND / 4, beatsPerMinute = 120).eighth

        // Then
        assertThat(first).isZero()
        assertThat(then).isEqualTo(1)
        assertThat(playHead.head.eighth).isEqualTo(1)
    }

    @Test
    fun `frames skipped are caught up, every tick of them`() {
        // Given
        val playHead = FramePlayHead(eighthsPerLoop = 16)
        playHead.advance(frameNanos = 0, beatsPerMinute = 120)

        // When a second goes by in one frame
        val eighth = playHead.advance(frameNanos = SECOND, beatsPerMinute = 120).eighth

        // Then 4 eighths are played
        assertThat(eighth).isEqualTo(4)
    }

    private companion object {
        const val SECOND = 1_000_000_000L
    }
}
