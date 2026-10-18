package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
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
        // When: one second of sound
        val samples = synthesizer.oscillate(Waveform.SQUARE, frequency = 440.0, volume = 0.5, sampleCount = 44_100)

        // Then
        assertThat(samples.toSet()).containsOnly(0.5f, -0.5f)
        val risingEdges = (1 until samples.size).filter { samples[it - 1] < 0 && samples[it] > 0 }
        assertThat(risingEdges).hasSize(439)
        val period = (risingEdges.last() - risingEdges.first()).toDouble() / (risingEdges.size - 1)
        assertThat(period).isCloseTo(44_100.0 / 440, within(0.01))
    }

    @Test
    fun `a triangle wave starts at 0, reaches the volume at a quarter period and varies linearly`() {
        // When: at 441 Hz, a period is exactly 100 samples
        val samples = synthesizer.oscillate(Waveform.TRIANGLE, frequency = 441.0, volume = 0.5, sampleCount = 100)

        // Then
        val expected = mapOf(0 to 0.0, 10 to 0.2, 25 to 0.5, 40 to 0.2, 50 to 0.0, 60 to -0.2, 75 to -0.5, 90 to -0.2)
        expected.forEach { (index, value) ->
            assertThat(samples[index].toDouble()).describedAs("sample $index").isCloseTo(value, within(1e-6))
        }
    }

    @Test
    fun `a rest only produces zeros`() {
        // When
        val samples = synthesizer.renderNote(pitch = null, Waveform.SQUARE, volume = 0.5, sampleCount = 1_000)

        // Then
        assertThat(samples.toList()).hasSize(1_000).containsOnly(0f)
    }

    @Test
    fun `the envelope keeps the volume up to 80 percent of the note, fades out up to 95 percent, then is silent`() {
        // Given a square wave, whose absolute value is the volume times the envelope
        val samples = synthesizer.renderNote(Pitch.parse("A4"), Waveform.SQUARE, volume = 0.5, sampleCount = 1_000)

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
    fun `a note lasts its beats times 60 over the tempo times the sample rate`() {
        // Given one beat at 150 bpm: 0.4 s
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = listOf(Note(Pitch.parse("A4"), beats = 1.0)))

        // When
        val samples = synthesizer.renderVoice(voice, bpm = 150)

        // Then
        assertThat(samples).hasSize(17_640)
    }

    @Test
    fun `a whole voice lasts exactly its total beats, without drift from rounding each note`() {
        // Given 18 eighth notes at 132 bpm: an eighth note is 10 022.7 samples, 9 beats are 180 409.1
        val voice = Voice(Waveform.SQUARE, volume = 0.5, notes = List(18) { Note(Pitch.parse("A4"), beats = 0.5) })

        // When
        val samples = synthesizer.renderVoice(voice, bpm = 132)

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
        val samples = synthesizer.renderVoice(voice, bpm = 150)

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
        assertThatIllegalArgumentException().isThrownBy { synthesizer.renderVoice(voice, bpm) }
    }
}
