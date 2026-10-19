package ch.magori.blox.feature.composer.grid

import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.model.notes
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GridRowsTest {
    @Test
    fun `the lines of the open track are shown from the highest down`() {
        // Given a new music, in C major
        val editor = EditorState.new()

        // When
        val rows = gridRows(editor, showOtherTrack = true)

        // Then
        assertThat(rows.map { it.pitch }).containsExactlyElementsOf(
            listOf("C6", "B5", "A5", "G5", "F5", "E5", "D5", "C5", "B4", "A4", "G4", "F4", "E4", "D4", "C4")
                .map(::pitch),
        )
        assertThat(rows.first().name).isEqualTo(RowName(NoteStep.DO, sharp = false, Register.VERY_HIGH))
        assertThat(rows).allMatch { it.inScale }
    }

    @Test
    fun `a space separates the octaves of the grid`() {
        // Given
        val editor = EditorState.new()

        // When
        val rows = gridRows(editor, showOtherTrack = true)

        // Then a space above the first line of each octave but the top one: B5 and B4
        assertThat(rows.filter { it.spaceAbove }.map { it.pitch }).containsExactly(pitch("B5"), pitch("B4"))
    }

    @Test
    fun `each line shows the eight cells of the selected bar of the open track`() {
        // Given a note of E3 on the second cell of the third bar of the bass, and a note of E4 on the melody
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)
            .copy(track = Track.BASS, bar = 2).touch(pitch("E3"), eighth = 1)

        // When
        val rows = gridRows(editor, showOtherTrack = false)

        // Then
        val empty = CellView(CellPart.SILENT, CellPart.SILENT)
        val note = CellView(CellPart.ATTACK, CellPart.HELD)
        assertThat(rows.single { it.pitch == pitch("E3") }.cells)
            .containsExactly(empty, note, empty, empty, empty, empty, empty, empty)
        assertThat(rows.filter { it.pitch != pitch("E3") }).allMatch { row -> row.cells == List(8) { empty } }
    }

    @Test
    fun `the notes of the other track mark the cells where they sound, on the line of the same name and octave`() {
        // Given a note of C3, the second octave of the bass, held over the third and fourth cells, on the melody tab
        val editor = EditorState.new().copy(track = Track.BASS).slide(pitch("C3"), fromEighth = 2, toEighth = 3)
            .copy(track = Track.MELODY)

        // When
        val shown = gridRows(editor, showOtherTrack = true)
        val hidden = gridRows(editor, showOtherTrack = false)

        // Then it marks the line of C5, the second octave of the melody
        assertThat(shown.single { it.pitch == pitch("C5") }.otherTrack)
            .containsExactly(false, false, true, true, false, false, false, false)
        assertThat(shown.filter { it.pitch != pitch("C5") }).allMatch { row -> row.otherTrack.none { it } }
        assertThat(hidden).allMatch { row -> row.otherTrack.none { it } }
    }

    @Test
    fun `a line out of the scale is shown as such`() {
        // Given a music opened with an F sharp, out of C major
        val opened = ScoreDraft.new().let {
            it.copy(melody = TrackGrid.playing(Track.MELODY, it.scale, notes("F#5:0.5")))
        }

        // When
        val rows = gridRows(EditorState.open(opened), showOtherTrack = true)

        // Then
        assertThat(rows.filterNot { it.inScale }.map { it.pitch }).containsExactly(pitch("F#5"))
    }

    @Test
    fun `a note of the other track whose line the open track does not have is not marked`() {
        // Given a bass opened with an F sharp 3, out of C major: the melody has no line of F sharp 5
        val opened = ScoreDraft.new().let {
            it.copy(bass = TrackGrid.playing(Track.BASS, it.scale, notes("F#3:0.5")))
        }

        // When
        val rows = gridRows(EditorState.open(opened), showOtherTrack = true)

        // Then
        assertThat(rows).allMatch { row -> row.otherTrack.none { it } }
    }

    @Test
    fun `each cell tells what touching it does, in the mode of the toolbar`() {
        // Given a note of E4 on the first cell
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)

        // When
        val placing = gridRows(editor, showOtherTrack = true).single { it.pitch == pitch("E4") }
        val lengthening = gridRows(editor.copy(mode = EditMode.LENGTHEN), showOtherTrack = true)
            .single { it.pitch == pitch("E4") }

        // Then
        assertThat(placing.actions.take(2)).containsExactly(CellAction.ERASE, CellAction.PLACE)
        assertThat(lengthening.actions.take(2)).containsExactly(CellAction.ONE_CELL, CellAction.LENGTHEN)
    }
}
