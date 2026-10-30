package ch.magori.blox.core.game.tempo

import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class MeterTest {
    @Test
    fun `an eighth lasts a whole number of ticks`() {
        // Given a meter of 15 ticks per eighth
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then an eighth starts every 15 ticks, and only then
        assertThat(listOf(0, 15, 30, 45)).allMatch { meter.startsAnEighth(it) }
        assertThat(listOf(1, 14, 16, 29)).noneMatch { meter.startsAnEighth(it) }
    }

    @Test
    fun `tick 0 is beat 1 of bar 1`() {
        // Given a meter of 15 ticks per eighth
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then
        assertThat(meter.beatOf(0)).isEqualTo(1)
        assertThat(meter.barOf(0)).isEqualTo(1)
    }

    @Test
    fun `a quarter note lasts 2 eighths`() {
        // Given a meter of 15 ticks per eighth
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then a beat lasts 30 ticks, and the 4 beats of the bar follow each other
        assertThat(meter.ticksPerQuarter).isEqualTo(Ticks(30))
        assertThat(listOf(0, 29, 30, 59, 60, 90, 119).map(meter::beatOf)).containsExactly(1, 1, 2, 2, 3, 4, 4)
    }

    @Test
    fun `a bar lasts 8 eighths and a phrase 4 bars`() {
        // Given a meter of 15 ticks per eighth
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then a bar lasts 120 ticks, a phrase 480, and the phrase starts again at bar 1, beat 1
        assertThat(meter.ticksPerBar).isEqualTo(Ticks(120))
        assertThat(meter.ticksPerPhrase).isEqualTo(Ticks(480))
        assertThat(listOf(119, 120, 240, 479, 480, 600).map(meter::barOf)).containsExactly(1, 2, 3, 4, 1, 2)
        assertThat(listOf(120, 479, 480).map(meter::beatOf)).containsExactly(1, 4, 1)
    }

    @Test
    fun `the bar of the nearest beat is the one of the nearest quarter note`() {
        // Given a meter of 15 ticks per eighth: the phrase starts again at tick 480
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then 5 ticks before beat 1 of bar 1 is in bar 1, 5 ticks after the last beat of bar 4 in bar 4, and
        // the off-beat, halfway between two quarter notes, goes with the later one
        assertThat(listOf(475, 485, 455, 239, 225, 224).map(meter::barOfNearestBeat))
            .containsExactly(1, 1, 4, 3, 3, 2)
    }

    @Test
    fun `a tick within N ticks of a quarter note is on the beat`() {
        // Given a meter of 15 ticks per eighth, with a window of 5 ticks
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then the ticks up to 5 before or after a quarter note are on the beat, the others are not
        assertThat(listOf(0, 5, 25, 30, 35, 475, 480)).allMatch { meter.isOnBeat(it) }
        assertThat(listOf(6, 15, 24, 36, 474)).noneMatch { meter.isOnBeat(it) }
    }

    @Test
    fun `a wider window takes in more ticks`() {
        // Given a meter of 15 ticks per eighth, with a window of 6 ticks
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 6)

        // When / Then
        assertThat(listOf(6, 24, 36)).allMatch { meter.isOnBeat(it) }
        assertThat(listOf(7, 23)).noneMatch { meter.isOnBeat(it) }
    }

    @Test
    fun `the tempo is 1800 over the ticks per eighth`() {
        // Given meters of 15 and 16 ticks per eighth
        val at15 = meter(ticksPerEighth = 15, onBeatWindow = 5)
        val at16 = meter(ticksPerEighth = 16, onBeatWindow = 5)

        // When / Then in quarter notes per minute
        assertThat(at15.beatsPerMinute).isEqualTo(120.0)
        assertThat(at16.beatsPerMinute).isEqualTo(112.5)
    }

    @Test
    fun `the window on the beat is 5 ticks until it is measured`() {
        // Given a meter given no window
        val meter = Meter(ticksPerEighth = Ticks(15))

        // When / Then the provisional window of the rules (to be measured on a phone, P11.2)
        assertThat(meter.onBeatWindow).isEqualTo(Meter.PROVISIONAL_ON_BEAT_WINDOW).isEqualTo(Ticks(5))
    }

    @Test
    fun `the divisions follow the ticks per eighth`() {
        // Given a meter of 10 ticks per eighth, with a window of 5 ticks
        val meter = meter(ticksPerEighth = 10, onBeatWindow = 5)

        // When / Then
        assertThat(listOf(meter.ticksPerQuarter, meter.ticksPerBar, meter.ticksPerPhrase))
            .containsExactly(Ticks(20), Ticks(80), Ticks(320))
        assertThat(meter.beatOf(20)).isEqualTo(2)
        assertThat(meter.barOf(80)).isEqualTo(2)
        assertThat(meter.barOf(320)).isEqualTo(1)
        assertThat(meter.isOnBeat(15)).isTrue()
        assertThat(meter.isOnBeat(14)).isFalse()
    }

    @Test
    fun `a window reaching the off-beat is refused`() {
        // Given a window as long as an eighth, which would reach the off-beat between two quarter notes
        val ticksPerEighth = Ticks(6)
        val onBeatWindow = Ticks(6)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Meter(ticksPerEighth, onBeatWindow) }
    }

    @Test
    fun `a negative tick count is refused`() {
        // Given a meter
        val meter = meter(ticksPerEighth = 15, onBeatWindow = 5)

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { meter.beatOf(-1) }
        assertThatIllegalArgumentException().isThrownBy { meter.barOf(-1) }
        assertThatIllegalArgumentException().isThrownBy { meter.isOnBeat(-1) }
        assertThatIllegalArgumentException().isThrownBy { meter.startsAnEighth(-1) }
    }

    private fun meter(
        ticksPerEighth: Int,
        onBeatWindow: Int,
    ) = Meter(Ticks(ticksPerEighth), Ticks(onBeatWindow))
}
