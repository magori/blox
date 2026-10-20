package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DraftFormTest {
    @Test
    fun `a draft comes back from its saved form as it was, its scale, category, tempo, lines and notes kept`() {
        // Given a draft of a category, with sixteenth notes, a long note, and a line out of the scale whose note was
        // erased: the line stays in the grid
        val scale = Scale(pitch("D4"), ScaleType.DORIAN)
        val opened = ScoreDraft.fromScore(
            score(
                melody = thenSilence(notes("D5:0.25 E5:0.25 F#5:0.5 A4:3"), bars = 7),
                bass = thenSilence(notes("D3:2 A2:2"), bars = 7),
                bpm = 100,
            ),
            scale,
            Category.MYSTERIOUS,
        )!!
        val draft = EditorState.open(opened).touch(pitch("F#5"), eighth = 1).draft

        // When
        val back = DraftForm.read(DraftForm.write(draft))

        // Then
        assertThat(draft.melody.rows.map { it.pitch }).contains(pitch("F#5"))
        assertThat(back).isEqualTo(draft)
    }

    @Test
    fun `a new music, without category, comes back from its saved form as it was`() {
        // Given
        val draft = ScoreDraft.new()

        // Then
        assertThat(DraftForm.read(DraftForm.write(draft))).isEqualTo(draft)
    }

    @Test
    fun `the saved form of a full grid stays a few kilobytes, far below what a saved state may hold`() {
        // Given every sixteenth note of both tracks played
        val empty = ScoreDraft.new()
        fun full(track: Track): TrackGrid {
            val pitch = empty[track].rows.last().pitch
            return empty[track].copy(notes = (0 until SIXTEENTHS).map { GridNote(pitch, it, 1) })
        }
        val draft = empty.copy(melody = full(Track.MELODY), bass = full(Track.BASS))

        // Then
        assertThat(DraftForm.write(draft).length).isLessThan(4_000)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "garbage",
            // a later version
            "2;60;MAJOR;-;120;60/;48/",
            // a scale that does not exist
            "1;60;LYDIAN;-;120;60/;48/",
            // a tempo out of the tempos of the editor
            "1;60;MAJOR;-;30;60/;48/",
            // two notes at once on a track
            "1;60;MAJOR;-;120;60,62/60.0.4,62.2.2;48/",
            // a note on no line of its grid
            "1;60;MAJOR;-;120;60/62.0.2;48/",
            // a note of more than three parts
            "1;60;MAJOR;-;120;60/60.0.2.9;48/",
        ],
    )
    fun `a saved form that cannot be read gives no draft`(form: String) {
        assertThat(DraftForm.read(form)).isNull()
    }
}
