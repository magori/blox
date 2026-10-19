package ch.magori.blox.feature.composer.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class EditorStateTest {
    @Test
    fun `touching a cell places a note in the selected bar of the selected track`() {
        // Given the bass, on its third bar
        val editor = EditorState.new().copy(track = Track.BASS, bar = 2)

        // When
        val touched = editor.touch(pitch("E3"), eighth = 1)

        // Then
        assertThat(touched.draft.bass.line("E3", bar = 2)).isEqualTo("..O=............")
        assertThat(touched.draft.melody.notes).isEmpty()
    }

    @Test
    fun `in Lengthen mode, touching a cell lengthens the note on its left`() {
        // Given an eighth note, in Lengthen mode
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0).copy(mode = EditMode.LENGTHEN)

        // When
        val touched = editor.touch(pitch("E4"), eighth = 2)

        // Then
        assertThat(touched.draft.melody.line("E4")).isEqualTo("O=====..........")
    }

    @Test
    fun `sliding along a line places a long note in the selected bar of the selected track`() {
        // Given the bass, on its second bar
        val editor = EditorState.new().copy(track = Track.BASS, bar = 1)

        // When
        val slid = editor.slide(pitch("G2"), fromEighth = 4, toEighth = 7)

        // Then
        assertThat(slid.draft.bass.line("G2", bar = 1)).isEqualTo("........O=======")
    }

    @Test
    fun `copying the bar copies it over the next one, on the open track only, and shows the next bar`() {
        // Given a note in the first bar of each track
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)
            .copy(track = Track.BASS).touch(pitch("C3"), eighth = 0)

        // When
        val copied = editor.copyBar()

        // Then
        assertThat(copied.bar).isEqualTo(1)
        assertThat(copied.draft.bass.line("C3", bar = 1)).isEqualTo("O=..............")
        assertThat(copied.draft.melody.line("E4", bar = 1)).isEqualTo("................")
    }

    @Test
    fun `Copy the bar is disabled on the eighth bar, which has no bar after it`() {
        assertThat(EditorState.new().copy(bar = 6).canCopyBar).isTrue()
        assertThat(EditorState.new().copy(bar = 7).canCopyBar).isFalse()
    }

    @Test
    fun `copying the eighth bar is refused`() {
        assertThatIllegalStateException().isThrownBy { EditorState.new().copy(bar = 7).copyBar() }
    }

    @Test
    fun `clearing the bar silences it on the open track only`() {
        // Given a note in the first bar of each track, the bass open
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)
            .copy(track = Track.BASS).touch(pitch("C3"), eighth = 0)

        // When
        val cleared = editor.clearBar()

        // Then
        assertThat(cleared.draft.bass.notes).isEmpty()
        assertThat(cleared.draft.melody.line("E4")).isEqualTo("O=..............")
    }

    @Test
    fun `Clear the bar is disabled on a bar of the open track where no note sounds`() {
        // Given a note in the first bar of the melody, the bass open
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0)

        // Then
        assertThat(editor.canClearBar).isTrue()
        assertThat(editor.copy(track = Track.BASS).canClearBar).isFalse()
        assertThat(editor.copy(bar = 1).canClearBar).isFalse()
    }

    @Test
    fun `the tempo buttons speed the music up or slow it down, and take the undo away`() {
        // Given a cleared bar that could be undone, at 120 beats per minute
        val editor = EditorState.new().touch(pitch("E4"), eighth = 0).clearBar()

        // When
        val faster = editor.faster()
        val slower = editor.slower()

        // Then
        assertThat(faster.draft.tempo).isEqualTo(Tempo(130))
        assertThat(slower.draft.tempo).isEqualTo(Tempo(110))
        assertThat(faster.canUndo).isFalse()
        assertThat(slower.canUndo).isFalse()
    }

    @Test
    fun `the editor shows one of the eight bars`() {
        assertThatIllegalArgumentException().isThrownBy { EditorState.new().copy(bar = 8) }
    }

    @Test
    fun `clearing a bar where no note sounds is refused`() {
        assertThatIllegalStateException().isThrownBy { EditorState.new().clearBar() }
    }
}
