package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A buffer of 12 samples holds 4 beats of 3 samples: beats 0, 1, 2 and 3 start at 0, 3, 6 and 9. */
class ResumePositionTest {
    @Test
    fun `a whole note resumes from the start of the loop`() {
        // Given a loop that may switch only at its start, stopped in the middle of its whole note
        val switchBeats = listOf(0, 4)

        // When
        val position = resumePosition(position = 8, length = 12, newLength = 12, switchBeats = switchBeats, beats = 4)

        // Then
        assertThat(position).isZero()
    }

    @Test
    fun `a place in a beat where a note goes on resumes from the last beat where every note starts`() {
        // Given a loop that may switch on beats 0 and 2, stopped in beat 3 (sample 10), where a note goes on
        val switchBeats = listOf(0, 2, 4)

        // When
        val position = resumePosition(position = 10, length = 12, newLength = 12, switchBeats = switchBeats, beats = 4)

        // Then the start of beat 2, not of beat 3
        assertThat(position).isEqualTo(6)
    }

    @Test
    fun `a place rounded onto the end of the new buffer resumes from its start`() {
        // Given sample 11 of 12, in a buffer of 6: 5.5, rounded to 6, the end of the buffer
        val switchBeats = listOf(0, 1, 2, 3, 4)

        // When
        val position = resumePosition(position = 11, length = 12, newLength = 6, switchBeats = switchBeats, beats = 4)

        // Then
        assertThat(position).isZero()
    }

    @Test
    fun `the start of the loop stays the start of the loop`() {
        // When
        val position =
            resumePosition(position = 0, length = 12, newLength = 6, switchBeats = listOf(0, 2, 4), beats = 4)

        // Then
        assertThat(position).isZero()
    }
}
