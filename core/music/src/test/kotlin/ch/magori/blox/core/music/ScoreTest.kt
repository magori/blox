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
        val score = Score("Test", bpm = 150, voices = listOf(melody))

        // Then
        assertThat(score.title).isEqualTo("Test")
        assertThat(score.bpm).isEqualTo(150)
        assertThat(score.voices).containsExactly(melody)
    }

    @ParameterizedTest(name = "{0} beats per minute are refused")
    @ValueSource(ints = [0, -1])
    fun `the tempo must be positive`(bpm: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Score("Test", bpm, listOf(aVoice())) }
    }

    @Test
    fun `a score needs at least one voice`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Score("Test", bpm = 150, voices = emptyList()) }
    }
}
