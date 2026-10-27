package ch.magori.blox.core.designsystem.frame

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LoopPlayHeadTest {
    @Test
    fun `the play head starts on the first eighth of the loop`() {
        assertThat(LoopPlayHead.start(eighthsPerLoop = 64).eighth).isZero()
    }

    @Test
    fun `the play head moves on an eighth each eighth note, 15 ticks at 120 beats per minute`() {
        // Given
        val start = LoopPlayHead.start(eighthsPerLoop = 64)

        // When
        val early = start.advance(ticks = 14, beatsPerMinute = 120)
        val second = early.advance(ticks = 1, beatsPerMinute = 120)
        val tenth = second.advance(ticks = 8 * 15, beatsPerMinute = 120)

        // Then
        assertThat(early.eighth).isZero()
        assertThat(second.eighth).isEqualTo(1)
        assertThat(tenth.eighth).isEqualTo(9)
    }

    @ParameterizedTest(name = "eighths per loop: {0}")
    @ValueSource(ints = [64, 16])
    fun `the play head comes back to the first eighth after the last one, as the music loops`(eighthsPerLoop: Int) {
        // Given the last eighth of the loop, at 60 beats per minute: 30 ticks an eighth
        val last = LoopPlayHead.start(eighthsPerLoop).advance(ticks = (eighthsPerLoop - 1) * 30, beatsPerMinute = 60)

        // When
        val looped = last.advance(ticks = 30, beatsPerMinute = 60)

        // Then
        assertThat(last.eighth).isEqualTo(eighthsPerLoop - 1)
        assertThat(looped.eighth).isZero()
    }

    @Test
    fun `after a change of tempo, the play head moves on at the new tempo from where it stood`() {
        // Given half an eighth at 60 beats per minute, 15 ticks of 30
        val half = LoopPlayHead.start(eighthsPerLoop = 64).advance(ticks = 15, beatsPerMinute = 60)

        // When the other half at 120 beats per minute, 7 ticks of 15, then one more tick
        val almost = half.advance(ticks = 7, beatsPerMinute = 120)
        val next = almost.advance(ticks = 1, beatsPerMinute = 120)

        // Then
        assertThat(almost.eighth).isZero()
        assertThat(next.eighth).isEqualTo(1)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a loop of zero eighths or less is refused`(eighthsPerLoop: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { LoopPlayHead.start(eighthsPerLoop) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `a tempo of zero beats per minute or less is refused`(beatsPerMinute: Int) {
        // Given
        val start = LoopPlayHead.start(eighthsPerLoop = 64)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { start.advance(ticks = 1, beatsPerMinute) }
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, -15])
    fun `a negative number of ticks is refused`(ticks: Int) {
        // Given
        val start = LoopPlayHead.start(eighthsPerLoop = 64)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { start.advance(ticks, beatsPerMinute = 120) }
    }
}
