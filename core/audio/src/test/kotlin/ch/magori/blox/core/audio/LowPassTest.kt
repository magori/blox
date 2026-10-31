package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Gate
import ch.magori.blox.core.music.Instrument
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Tone
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class LowPassTest {
    @Test
    fun `a constant signal passes the low-pass unchanged`() {
        // Given one second of a constant 0.5, at 44 100 Hz
        val constant = FloatArray(SAMPLE_RATE) { 0.5f }

        // When it passes a low-pass at 1 000 Hz
        val filtered = lowPass(constant, cutoff = 1_000.0, SAMPLE_RATE)

        // Then, once the filter has risen from silence (a few samples), it comes out unchanged
        assertThat(filtered).hasSize(SAMPLE_RATE)
        filtered.drop(SETTLED).forEach { assertThat(it.toDouble()).isCloseTo(0.5, within(1e-6)) }
    }

    @Test
    fun `a tone at the cutoff comes out about 3 dB lower`() {
        // Given one second of a sine at 1 000 Hz
        val tone = sine(frequency = 1_000.0)

        // When it passes a low-pass at 1 000 Hz
        val filtered = lowPass(tone, cutoff = 1_000.0, SAMPLE_RATE)

        // Then its level, once settled, is 3 dB lower: half the power
        assertThat(decibelsBetween(filtered, tone)).isCloseTo(-3.0, within(0.2))
    }

    @Test
    fun `a tone far above the cutoff is attenuated`() {
        // Given one second of a sine at 10 000 Hz, more than three octaves above the cutoff
        val tone = sine(frequency = 10_000.0)

        // When it passes a low-pass at 1 000 Hz
        val filtered = lowPass(tone, cutoff = 1_000.0, SAMPLE_RATE)

        // Then it comes out more than 15 dB lower
        assertThat(decibelsBetween(filtered, tone)).isLessThan(-15.0)
    }

    @Test
    fun `an instrument without filter is not changed`() {
        // Given an A4 of one second on a square, gated, without filter, and the same with a filter at 1 000 Hz
        val voice = Voice(Waveform.SQUARE, volume = 0.5, listOf(Note(Pitch.parse("A4"), beats = 1.0)))
        val unfiltered = Instrument(Tone.SQUARE, Gate, lowPass = null, gain = 1.0)
        val filtered = unfiltered.copy(lowPass = 1_000.0)
        val synthesizer = Synthesizer(SAMPLE_RATE)

        // When
        val plain = synthesizer.renderVoice(voice, bpm = 60.0, unfiltered)
        val passed = synthesizer.renderVoice(voice, bpm = 60.0, filtered)

        // Then the square without filter is untouched, plus and minus its volume while held, and the filter of the
        // other instrument passes it through the low-pass
        assertThat(plain.copyOfRange(0, SAMPLE_RATE * 8 / 10).toSet()).containsOnly(0.5f, -0.5f)
        assertThat(passed).containsExactly(*lowPass(plain, cutoff = 1_000.0, SAMPLE_RATE))
    }

    /** One second of a sine of [frequency] and of amplitude 0.5. */
    private fun sine(frequency: Double): FloatArray =
        FloatArray(SAMPLE_RATE) { (0.5 * sin(2 * PI * frequency * it / SAMPLE_RATE)).toFloat() }

    /** The level of [output] over the one of [input], in decibels, measured once the filter has settled. */
    private fun decibelsBetween(
        output: FloatArray,
        input: FloatArray,
    ): Double = 20 * log10(rms(output.drop(SETTLED)) / rms(input.drop(SETTLED)))

    private fun rms(samples: List<Float>): Double = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    private companion object {
        const val SAMPLE_RATE = 44_100

        /** The samples after which the filter, a time constant of 7.5 samples at 1 000 Hz, has settled. */
        const val SETTLED = 1_000
    }
}
