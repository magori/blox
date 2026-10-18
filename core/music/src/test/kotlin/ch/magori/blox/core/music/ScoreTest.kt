package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ScoreTest {
    @Test
    fun `a well-formed score is built`() {
        // Given
        val melody = aVoice()

        // When
        val score = Score("Test", bpm = 150, beatsPerBar = 4, voices = listOf(melody))

        // Then
        assertThat(score.title).isEqualTo("Test")
        assertThat(score.bpm).isEqualTo(150)
        assertThat(score.beatsPerBar).isEqualTo(4)
        assertThat(score.voices).containsExactly(melody)
    }

    @ParameterizedTest(name = "{0} beats per minute are refused")
    @ValueSource(ints = [0, -1])
    fun `the tempo must be positive`(bpm: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            Score("Test", bpm, beatsPerBar = 4, voices = listOf(aVoice()))
        }
    }

    @Test
    fun `a score needs at least one voice`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            Score("Test", bpm = 150, beatsPerBar = 4, voices = emptyList())
        }
    }

    @Test
    fun `all voices of a score must have the same length`() {
        // Given
        val voices = listOf(aVoice(beats = 4.0), aVoice(beats = 8.0))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Score("Test", bpm = 150, beatsPerBar = 4, voices = voices) }
    }

    @ParameterizedTest(name = "{0} beats per bar are refused")
    @ValueSource(ints = [0, -1])
    fun `the number of beats per bar must be positive`(beatsPerBar: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy {
            Score("Test", bpm = 150, beatsPerBar = beatsPerBar, voices = listOf(aVoice(beats = 4.0)))
        }
    }

    @Test
    fun `every voice lasts a whole number of bars`() {
        // Given a voice of 5 beats in 4-4
        val voices = listOf(aVoice(beats = 5.0))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Score("Test", bpm = 150, beatsPerBar = 4, voices = voices) }
    }
}
