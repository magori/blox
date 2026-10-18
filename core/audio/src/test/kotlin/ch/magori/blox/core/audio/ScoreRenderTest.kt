package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ScoreRenderTest {
    private val synthesizer = Synthesizer(sampleRate = 44_100)

    private val melody = Voice(Waveform.SQUARE, volume = 0.07, notes = List(4) { Note(Pitch.parse("E5"), beats = 1.0) })
    private val bass = Voice(Waveform.TRIANGLE, volume = 0.16, notes = List(2) { Note(Pitch.parse("E2"), beats = 2.0) })
    private val score = Score("Test", bpm = 150, beatsPerBar = 4, voices = listOf(melody, bass))

    @Test
    fun `a score renders as the mix of its voices and lasts as long as the score`() {
        // When
        val samples = synthesizer.render(score)

        // Then: 4 beats at 150 bpm are 1.6 s, 70 560 samples
        assertThat(samples).hasSize(70_560)
        assertThat(
            samples,
        ).containsExactly(*mix(listOf(synthesizer.renderVoice(melody, 150.0), synthesizer.renderVoice(bass, 150.0))))
    }

    @Test
    fun `a tempo factor of 2 halves the duration`() {
        // When
        val samples = synthesizer.render(score, tempoFactor = 2.0)

        // Then: 4 beats at 300 bpm are 0.8 s
        assertThat(samples).hasSize(35_280)
    }

    @Test
    fun `the tempo may become fractional, 150 bpm times 1 point 25 is 187 point 5 bpm`() {
        // When
        val samples = synthesizer.render(score, tempoFactor = 1.25)

        // Then: 4 beats at 187.5 bpm are 1.28 s
        assertThat(samples).hasSize(56_448)
    }

    @ParameterizedTest(name = "a tempo factor of {0} is refused")
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `the tempo factor must be positive and finite`(tempoFactor: Double) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { synthesizer.render(score, tempoFactor) }
    }
}
