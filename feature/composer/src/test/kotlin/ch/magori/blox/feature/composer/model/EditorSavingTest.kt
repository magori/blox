package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EditorSavingTest {
    @Test
    fun `a new music is not saved`() {
        assertThat(EditorState.new().status).isEqualTo(SaveStatus.NOT_SAVED)
    }

    @Test
    fun `an opened music is saved until it is changed, then modified`() {
        // Given
        val opened = EditorState.open(ScoreDraft.new(tempo = Tempo(90)))

        // When
        val changed = opened.touch(pitch("E4"), eighth = 0)

        // Then
        assertThat(opened.status).isEqualTo(SaveStatus.SAVED)
        assertThat(changed.status).isEqualTo(SaveStatus.MODIFIED)
    }

    @Test
    fun `a music changed back to its saved version is saved again`() {
        // Given
        val opened = EditorState.open(ScoreDraft.new())

        // When the note placed is erased
        val back = opened.touch(pitch("E4"), eighth = 0).touch(pitch("E4"), eighth = 0)

        // Then
        assertThat(back.status).isEqualTo(SaveStatus.SAVED)
    }

    @Test
    fun `once saved, the music is saved, and changes are counted from there`() {
        // Given a new music with a note
        val written = EditorState.new().touch(pitch("E4"), eighth = 0)

        // When
        val saved = written.markedSaved()

        // Then
        assertThat(saved.status).isEqualTo(SaveStatus.SAVED)
        assertThat(saved.touch(pitch("G4"), eighth = 2).status).isEqualTo(SaveStatus.MODIFIED)
    }

    @Test
    fun `leaving a new music asks first, unless it was left empty`() {
        // Given a new music, whose tempo was changed but grids left empty
        val empty = EditorState.new().faster()

        // Then
        assertThat(empty.asksBeforeLeaving).isFalse()
        assertThat(empty.touch(pitch("E4"), eighth = 0).asksBeforeLeaving).isTrue()
    }

    @Test
    fun `leaving a saved music asks first only when it was changed, even if it was emptied`() {
        // Given a saved music with a note
        val opened = EditorState.open(ScoreDraft.new()).touch(pitch("E4"), eighth = 0).markedSaved()

        // Then
        assertThat(opened.asksBeforeLeaving).isFalse()
        assertThat(opened.clearBar().asksBeforeLeaving).isTrue()
    }

    @Test
    fun `an empty grid cannot be saved`() {
        assertThat(EditorState.new().saveProblem("Joyeux 1", emptyList())).isEqualTo(SaveProblem.EMPTY_GRID)
    }

    @Test
    fun `a music cannot be saved under the name of another music, and can under a free name`() {
        // Given a music with a note
        val written = EditorState.new().touch(pitch("E4"), eighth = 0)
        val names = listOf(savedMusic("Joyeux 1"))

        // Then
        assertThat(written.saveProblem("joyeux 1", names)).isEqualTo(SaveProblem.NAME_TAKEN)
        assertThat(written.saveProblem("Joyeux 2", names)).isNull()
    }

    @Test
    fun `a music cannot be saved without a name`() {
        // Given a music with a note
        val written = EditorState.new().touch(pitch("E4"), eighth = 0)

        // Then
        assertThat(written.saveProblem("  ", emptyList())).isEqualTo(SaveProblem.BLANK_NAME)
    }

    @Test
    fun `a music cannot be renamed without a name, nor with the name of another music, but can with its own`() {
        // Given
        val musics = listOf(savedMusic("Joyeux 1"), savedMusic("Calme 1"))
        val own = musics.first().id

        // Then
        assertThat(renameProblem(" ", musics, own)).isEqualTo(SaveProblem.BLANK_NAME)
        assertThat(renameProblem("calme 1", musics, own)).isEqualTo(SaveProblem.NAME_TAKEN)
        assertThat(renameProblem("JOYEUX 1", musics, own)).isNull()
        assertThat(renameProblem("Calme 1", musics, apartFrom = null)).isEqualTo(SaveProblem.NAME_TAKEN)
    }
}
