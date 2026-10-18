package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SwitchBeatsTest {
    @Test
    fun `a whole note lets the tempo switch only on beat 0`() {
        // Given
        val score = scoreOf(voiceOf(4.0))

        // When / Then
        assertThat(switchBeats(score)).containsExactly(0)
    }

    @Test
    fun `quarter notes let the tempo switch on every beat`() {
        // Given
        val score = scoreOf(voiceOf(1.0, 1.0, 1.0, 1.0))

        // When / Then
        assertThat(switchBeats(score)).containsExactly(0, 1, 2, 3)
    }

    @Test
    fun `two voices with offset notes let the tempo switch only on the beats they share`() {
        // Given a voice in half notes (beats 0, 2) and one offset by a dotted half note (beats 0, 3)
        val score = scoreOf(voiceOf(2.0, 2.0), voiceOf(3.0, 1.0))

        // When / Then: a note of one voice or the other goes across beats 2 and 3
        assertThat(switchBeats(score)).containsExactly(0)
    }

    @Test
    fun `eighth notes do not make beats of their own`() {
        // Given a dotted quarter note and an eighth note, then a half note: notes start on beats 0, 1.5, 2
        val score = scoreOf(voiceOf(1.5, 0.5, 2.0))

        // When / Then: beat 1 falls inside the dotted quarter note
        assertThat(switchBeats(score)).containsExactly(0, 2)
    }

    // A synthetic score: its identifier plays no part here.
    private fun scoreOf(vararg voices: Voice): Score =
        Score(id = ScoreId.KOROBEINIKI, bpm = 150, beatsPerBar = 4, voices = voices.toList())

    private fun voiceOf(vararg beats: Double): Voice =
        Voice(Waveform.SQUARE, volume = 0.07, notes = beats.map { Note(Pitch.parse("E5"), it) })
}
