package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Tone
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

class ToneTest {
    @Test
    fun `a sine at 440 Hz crosses zero rising every 100 point 2 samples`() {
        // When: one second of a sine at 440 Hz
        val samples = Tone.SINE.samples(frequency = 440.0, count = SAMPLE_RATE)

        // Then
        assertThat(samples.toList()).allSatisfy { assertThat(it).isBetween(-1.0, 1.0) }
        val risingCrossings = (1 until samples.size).filter { samples[it - 1] < 0 && samples[it] >= 0 }
        assertThat(risingCrossings).hasSize(439)
        val period = (risingCrossings.last() - risingCrossings.first()).toDouble() / (risingCrossings.size - 1)
        assertThat(period).isCloseTo(SAMPLE_RATE / 440.0, within(0.01))
    }

    @Test
    fun `a pulse of a quarter is high for a quarter of its period`() {
        // When: one second of a pulse at 100 Hz, a period of 441 samples
        val samples = Tone.PULSE_QUARTER.samples(frequency = 100.0, count = SAMPLE_RATE)

        // Then: high on 25 % of the samples, low on the others, at its full level once in each period
        assertThat(samples.toList()).allSatisfy { assertThat(it).isBetween(-1.0, 1.0) }
        val high = samples.count { it > 0 }.toDouble() / samples.size
        assertThat(high).isCloseTo(0.25, within(0.005))
        assertThat(samples.copyOfRange(0, 441).max()).isCloseTo(1.0, within(0.01))
    }

    @Test
    fun `a band-limited saw stays within -1 and 1 and loses its aliasing above the Nyquist frequency`() {
        // Given one second of a saw at 1 000 Hz, whose harmonics above 22 050 Hz fold back at 100 or 900 Hz from a
        // multiple of 1 000 Hz, and the same saw without band limit
        val saw = Tone.SAW.samples(frequency = 1_000.0, count = SAMPLE_RATE)
        val naiveSaw = DoubleArray(SAMPLE_RATE) { index -> 2 * ((index * 1_000.0 / SAMPLE_RATE) % 1.0) - 1 }
        val aliases = (0 until 22).flatMap { listOf(it * 1_000 + 100, it * 1_000 + 900) }

        // When
        val aliasing = aliases.sumOf { amplitudeAt(saw, it).pow(2) }
        val naiveAliasing = aliases.sumOf { amplitudeAt(naiveSaw, it).pow(2) }

        // Then: the harmonics stay, the aliasing falls by more than 10 dB (about 15 dB for a PolyBLEP over two
        // samples, which leaves the aliases next to the Nyquist frequency, where they matter least)
        assertThat(saw.toList()).allSatisfy { assertThat(it).isBetween(-1.0, 1.0) }
        assertThat(amplitudeAt(saw, 1_000)).isCloseTo(2 / PI, within(0.01))
        assertThat(aliasing).isLessThan(naiveAliasing / 10)
    }

    @Test
    fun `a band-limited pulse of a quarter loses its aliasing above the Nyquist frequency`() {
        // Given one second of a pulse of a quarter at 1 000 Hz, whose harmonics above 22 050 Hz fold back at 100 or
        // 900 Hz from a multiple of 1 000 Hz, and the same pulse without band limit
        val pulse = Tone.PULSE_QUARTER.samples(frequency = 1_000.0, count = SAMPLE_RATE)
        val naivePulse = DoubleArray(SAMPLE_RATE) { index ->
            if ((index * 1_000.0 / SAMPLE_RATE) % 1.0 < 0.25) 1.0 else -1.0
        }
        val aliases = (0 until 22).flatMap { listOf(it * 1_000 + 100, it * 1_000 + 900) }

        // When
        val aliasing = aliases.sumOf { amplitudeAt(pulse, it).pow(2) }
        val naiveAliasing = aliases.sumOf { amplitudeAt(naivePulse, it).pow(2) }

        // Then: the fundamental stays, 4 sin(π / 4) / π, and the aliasing falls by more than 10 dB
        assertThat(amplitudeAt(pulse, 1_000)).isCloseTo(4 * sin(PI / 4) / PI, within(0.01))
        assertThat(aliasing).isLessThan(naiveAliasing / 10)
    }

    @Test
    fun `the square and the triangle render as before`() {
        // Given phases next to the edges of the square, for a wave whose phase moves by a hundredth a sample: a
        // band limit would round them
        val phaseStep = 0.01

        // When
        val square = listOf(0.0, 0.005, 0.4999, 0.5, 0.5001, 0.995).map { Tone.SQUARE.amplitudeAt(it, phaseStep) }
        val triangle = listOf(0.0, 0.125, 0.25, 0.5, 0.75, 0.875).map { Tone.TRIANGLE.amplitudeAt(it, phaseStep) }

        // Then: exactly the waves of before the instruments, so that the Arcade style keeps its samples
        assertThat(square).containsExactly(1.0, 1.0, 1.0, -1.0, -1.0, -1.0)
        assertThat(triangle).containsExactly(0.0, 0.5, 1.0, 0.0, -1.0, -0.5)
    }

    /** The amplitude of the component of [samples] at [frequency], a whole number of periods over the samples. */
    private fun amplitudeAt(
        samples: DoubleArray,
        frequency: Int,
    ): Double {
        val angle = 2 * PI * frequency / samples.size
        val real = samples.indices.sumOf { samples[it] * cos(angle * it) }
        val imaginary = samples.indices.sumOf { samples[it] * sin(angle * it) }
        return 2 * hypot(real, imaginary) / samples.size
    }

    /** [count] samples of this tone at [frequency], the phase of sample i being i × frequency / rate, as in a note. */
    private fun Tone.samples(
        frequency: Double,
        count: Int,
    ): DoubleArray {
        val phaseStep = frequency / SAMPLE_RATE
        return DoubleArray(count) { index -> amplitudeAt((index * frequency / SAMPLE_RATE) % 1.0, phaseStep) }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
