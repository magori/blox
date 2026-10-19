package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EditorUndoTest {
    private val withANote = EditorState.new().touch(pitch("E4"), eighth = 0)

    @Test
    fun `clearing the bar can be undone, once`() {
        // Given
        val cleared = withANote.clearBar()

        // When
        val undone = cleared.undo()

        // Then
        assertThat(cleared.canUndo).isTrue()
        assertThat(undone.draft).isEqualTo(withANote.draft)
        assertThat(undone.canUndo).isFalse()
    }

    @Test
    fun `copying the bar can be undone`() {
        // Given a note in the second bar too, which the copy covers
        val editor = withANote.copy(bar = 1).touch(pitch("G4"), eighth = 3).copy(bar = 0)

        // When
        val undone = editor.copyBar().undo()

        // Then
        assertThat(undone.draft).isEqualTo(editor.draft)
    }

    @Test
    fun `touching or sliding a note takes the undo away, as touching the cell again undoes it`() {
        // Given
        val cleared = withANote.clearBar()

        // Then
        assertThat(cleared.touch(pitch("G4"), eighth = 2).canUndo).isFalse()
        assertThat(cleared.slide(pitch("G4"), fromEighth = 2, toEighth = 3).canUndo).isFalse()
    }

    @Test
    fun `the dice replaces the music, shows its first bar, and can be undone`() {
        // Given a generated music, and the editor on the third bar
        val generated = ScoreDraft.new(tempo = Tempo(150))
            .let { it.with(Track.BASS, it.bass.place(pitch("C3"), Cell(0, 0))) }
        val editor = withANote.copy(bar = 2)

        // When
        val rolled = editor.rollDice(generated)

        // Then
        assertThat(rolled.draft).isEqualTo(generated)
        assertThat(rolled.bar).isZero()
        assertThat(rolled.undo().draft).isEqualTo(withANote.draft)
    }

    @Test
    fun `the dice keeps no undo when the grid was empty`() {
        // Given an empty music, whose tempo was changed
        val editor = EditorState(ScoreDraft.new(tempo = Tempo(80)))

        // When
        val rolled = editor.rollDice(ScoreDraft.new(tempo = Tempo(150)))

        // Then
        assertThat(rolled.canUndo).isFalse()
    }

    @Test
    fun `the dice on an emptied grid takes away the undo left by clearing it`() {
        // Given the only note cleared, which could be undone
        val emptied = withANote.clearBar()

        // When
        val rolled = emptied.rollDice(ScoreDraft.new(tempo = Tempo(150)))

        // Then
        assertThat(emptied.canUndo).isTrue()
        assertThat(rolled.canUndo).isFalse()
    }
}
