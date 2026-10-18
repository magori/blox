package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NoteTest {
    @ParameterizedTest(name = "{0} beats are refused")
    @ValueSource(doubles = [0.0, -0.5, Double.POSITIVE_INFINITY])
    fun `the duration of a note must be positive and finite`(beats: Double) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Note(Pitch.parse("A4"), beats) }
    }

    @Test
    fun `a note without pitch is a rest`() {
        // Given
        val note = Note(pitch = null, beats = 1.0)

        // When / Then
        assertThat(note.isRest).isTrue()
    }

    @Test
    fun `a note with a pitch is not a rest`() {
        // Given
        val note = Note(pitch = Pitch.parse("E5"), beats = 0.5)

        // When / Then
        assertThat(note.isRest).isFalse()
    }
}
