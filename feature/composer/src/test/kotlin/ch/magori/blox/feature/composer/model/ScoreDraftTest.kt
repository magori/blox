package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScoreDraftTest {
    @Test
    fun `an empty draft plays as eight bars of silence on the melody and bass sounds of the scores, at its tempo`() {
        // Given
        val draft = ScoreDraft.new().copy(tempo = Tempo(96))

        // When
        val score = draft.toScore(ScoreId.KOROBEINIKI)

        // Then
        val silence = List(8) { Note(null, 4.0) }
        assertThat(score.bpm).isEqualTo(96)
        assertThat(score.beatsPerBar).isEqualTo(4)
        assertThat(score.voices).containsExactly(
            Voice(Waveform.SQUARE, 0.07, silence),
            Voice(Waveform.TRIANGLE, 0.16, silence),
        )
    }

    @Test
    fun `the notes of a track play with their length, the silences between them as rests that stop at the bar lines`() {
        // Given an eighth note on E4, then a half note on G4 after an eighth of silence
        val new = ScoreDraft.new()
        val draft = new.copy(
            melody = new.melody.place(pitch("E4"), Cell(bar = 0, eighth = 0))
                .slide(pitch("G4"), from = Cell(bar = 0, eighth = 2), to = Cell(bar = 0, eighth = 5)),
        )

        // When
        val melody = draft.toScore(ScoreId.KOROBEINIKI).voices.first().notes

        // Then
        assertThat(melody).containsExactlyElementsOf(
            listOf(Note(pitch("E4"), 0.5), Note(null, 0.5), Note(pitch("G4"), 2.0), Note(null, 1.0)) +
                List(7) { Note(null, 4.0) },
        )
    }

    @Test
    fun `a draft is empty while neither track has a note`() {
        // Given
        val new = ScoreDraft.new()

        // Then
        assertThat(new.isEmpty).isTrue()
        assertThat(new.with(Track.BASS, new.bass.place(pitch("C3"), Cell(0, 0))).isEmpty).isFalse()
        assertThat(new.with(Track.MELODY, new.melody.place(pitch("C4"), Cell(0, 0))).isEmpty).isFalse()
    }
}
