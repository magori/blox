package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Adsr
import ch.magori.blox.core.music.Instrument
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Tone
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.abs

class SynthesizerTest {
    private val synthesizer = Synthesizer(sampleRate = 44_100)

    @Test
    fun `a square wave at 440 Hz alternates plus and minus the volume with a period of about 100 point 2 samples`() {
        // When: the first second of an A4 of 1.25 s, before its envelope fades
        val samples = synthesizer.renderVoice(oneNote(Pitch.parse("A4"), Waveform.SQUARE, beats = 1.25), bpm = 60.0)
            .copyOfRange(0, 44_100)

        // Then
        assertThat(samples.toSet()).containsOnly(0.5f, -0.5f)
        val risingEdges = (1 until samples.size).filter { samples[it - 1] < 0 && samples[it] > 0 }
        assertThat(risingEdges).hasSize(439)
        val period = (risingEdges.last() - risingEdges.first()).toDouble() / (risingEdges.size - 1)
        assertThat(period).isCloseTo(44_100.0 / 440, within(0.01))
    }

    @Test
    fun `a triangle wave starts at 0, reaches the volume at a quarter period and varies linearly`() {
        // When: an A4 of one second, a period of 100.23 samples, far from its envelope
        val samples = synthesizer.renderVoice(oneNote(Pitch.parse("A4"), Waveform.TRIANGLE, beats = 1.0), bpm = 60.0)

        // Then: the phase of sample i is i × 440 / 44 100
        val expected = mapOf(0 to 0.0, 10 to 0.1995465, 25 to 0.4988662, 50 to 0.0022676, 75 to -0.4965986)
        expected.forEach { (index, value) ->
            assertThat(samples[index].toDouble()).describedAs("sample $index").isCloseTo(value, within(1e-6))
        }
    }

    @Test
    fun `a rest only produces zeros`() {
        // When
        val samples = synthesizer.renderVoice(
            oneNote(pitch = null, Waveform.SQUARE, beats = 1.0),
            bpm = ONE_THOUSAND_SAMPLES_A_BEAT,
        )

        // Then
        assertThat(samples.toList()).hasSize(1_000).containsOnly(0f)
    }

    @Test
    fun `the envelope keeps the volume up to 80 percent of the note, fades out up to 95 percent, then is silent`() {
        // Given a square wave, whose absolute value is the volume times the envelope
        val samples = synthesizer.renderVoice(
            oneNote(Pitch.parse("A4"), Waveform.SQUARE, beats = 1.0),
            bpm = ONE_THOUSAND_SAMPLES_A_BEAT,
        )

        // When
        val levels = samples.map { abs(it).toDouble() }

        // Then
        val expected = mapOf(0 to 0.5, 799 to 0.5, 800 to 0.5, 875 to 0.25, 920 to 0.1, 950 to 0.0, 999 to 0.0)
        expected.forEach { (index, level) ->
            assertThat(levels[index]).describedAs("level of sample $index").isCloseTo(level, within(1e-6))
        }
        assertThat(levels.subList(800, 950).zipWithNext { a, b -> a - b }).allSatisfy { step ->
            assertThat(step).isCloseTo(0.5 / 150, within(1e-6))
        }
    }

    @Test
    fun `an ADSR rises over its attack, falls to its sustain over its decay, and releases at the end of the note`() {
        // Given a square of 1 000 samples at 1 000 Hz, so that a millisecond is a sample: its absolute value is the
        // volume, 0.5, times the shape
        val shape = Adsr(attackMillis = 10, decayMillis = 20, sustain = 0.5, releaseMillis = 30)
        val instrument = Instrument(Tone.SQUARE, shape, lowPass = null, gain = 1.0)
        val voice = oneNote(Pitch.parse("A4"), Waveform.SQUARE, beats = 1.0)

        // When
        val levels = Synthesizer(sampleRate = 1_000).renderVoice(voice, bpm = 60.0, instrument).map { abs(it) }

        // Then: up over 10 samples, down to half over 20, held, then down to silence over the last 30
        val expected = mapOf(
            0 to 0.0,
            5 to 0.25,
            10 to 0.5,
            20 to 0.375,
            30 to 0.25,
            500 to 0.25,
            970 to 0.25,
            985 to 0.125,
            999 to 0.25 / 30,
        )
        expected.forEach { (index, level) ->
            assertThat(levels[index].toDouble()).describedAs("level of sample $index").isCloseTo(level, within(1e-6))
        }
    }

    @Test
    fun `a release never runs into the next note of the voice`() {
        // Given two squares of 100 samples at 1 000 Hz, at volume 0.5, on a release of 300 ms, three times the note
        val shape = Adsr(attackMillis = 0, decayMillis = 0, sustain = 1.0, releaseMillis = 300)
        val instrument = Instrument(Tone.SQUARE, shape, lowPass = null, gain = 1.0)
        val a4 = Pitch.parse("A4")
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = listOf(Note(a4, beats = 1.0), Note(a4, beats = 1.0)))

        // When
        val levels = Synthesizer(sampleRate = 1_000).renderVoice(voice, bpm = 600.0, instrument).map { abs(it) }

        // Then: the release, shortened to the note, starts from its full level and ends on its last sample, and the
        // next note starts on its own full level
        assertThat(levels).hasSize(200)
        assertThat(levels[0].toDouble()).isCloseTo(0.5, within(1e-6))
        assertThat(levels[50].toDouble()).isCloseTo(0.25, within(1e-6))
        assertThat(levels[99].toDouble()).isCloseTo(0.005, within(1e-6))
        assertThat(levels[100].toDouble()).isCloseTo(0.5, within(1e-6))
    }

    @Test
    fun `a note lasts its beats times 60 over the tempo times the sample rate`() {
        // Given one beat at 150 bpm: 0.4 s
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = listOf(Note(Pitch.parse("A4"), beats = 1.0)))

        // When
        val samples = synthesizer.renderVoice(voice, bpm = 150.0)

        // Then
        assertThat(samples).hasSize(17_640)
    }

    @Test
    fun `a whole voice lasts exactly its total beats, without drift from rounding each note`() {
        // Given 18 eighth notes at 132 bpm: an eighth note is 10 022.7 samples, 9 beats are 180 409.1
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = List(18) { Note(Pitch.parse("A4"), beats = 0.5) })

        // When
        val samples = synthesizer.renderVoice(voice, bpm = 132.0)

        // Then: rounding each note on its own would give 18 × 10 023 = 180 414
        assertThat(samples).hasSize(180_409)
    }

    @Test
    fun `each note starts at the sample of its cumulated beats`() {
        // Given a note, a rest and a note of one beat each at 150 bpm: 17 640 samples per beat
        val a4 = Pitch.parse("A4")
        val notes = listOf(Note(a4, beats = 1.0), Note(pitch = null, beats = 1.0), Note(a4, beats = 1.0))
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = notes)

        // When
        val samples = synthesizer.renderVoice(voice, bpm = 150.0)

        // Then
        assertThat(samples[0]).isEqualTo(0.5f)
        assertThat(samples.copyOfRange(17_640, 35_280).toList()).containsOnly(0f)
        assertThat(samples[35_280]).isEqualTo(0.5f)
    }

    @ParameterizedTest(name = "a sample rate of {0} is refused")
    @ValueSource(ints = [0, -1])
    fun `the sample rate must be positive`(sampleRate: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Synthesizer(sampleRate) }
    }

    @ParameterizedTest(name = "a tempo of {0} bpm is refused")
    @ValueSource(ints = [0, -1])
    fun `rendering a voice needs a positive tempo`(bpm: Int) {
        // Given
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = listOf(Note(Pitch.parse("A4"), beats = 1.0)))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { synthesizer.renderVoice(voice, bpm.toDouble()) }
    }

    /** A voice of a single note, at volume 0.5. */
    private fun oneNote(
        pitch: Pitch?,
        waveform: Waveform,
        beats: Double,
    ): Voice = Voice(waveform, volume = 0.5, notes = listOf(Note(pitch, beats)))

    private companion object {
        /** At this tempo, a beat lasts 1 000 samples at 44 100 Hz: 60 × 44 100 / 1 000. */
        const val ONE_THOUSAND_SAMPLES_A_BEAT = 2_646.0
    }
}
