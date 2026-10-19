package ch.magori.blox.feature.composer

import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Tempo
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ComposerViewModelTest {
    private val viewModel = ComposerViewModel()

    @Test
    fun `the editor opens a new music, with the other track shown in watermark`() {
        // Then
        assertThat(viewModel.uiState.value).isEqualTo(ComposerUiState(EditorState.new(), showOtherTrack = true))
    }

    @Test
    fun `touching a cell or sliding along a line edits the open track, in the selected bar`() {
        // When
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onCellSlid(pitch("G4"), fromEighth = 2, toEighth = 5)

        // Then
        val expected = EditorState.new().touch(pitch("E4"), eighth = 0).slide(pitch("G4"), fromEighth = 2, toEighth = 5)
        assertThat(viewModel.uiState.value.editor).isEqualTo(expected)
    }

    @Test
    fun `the tabs, the bar buttons and the mode choose what the grid shows and what a touch does`() {
        // When
        viewModel.onTrackChosen(Track.BASS)
        viewModel.onBarChosen(5)
        viewModel.onModeChosen(EditMode.LENGTHEN)

        // Then
        val editor = viewModel.uiState.value.editor
        assertThat(editor.track).isEqualTo(Track.BASS)
        assertThat(editor.bar).isEqualTo(5)
        assertThat(editor.mode).isEqualTo(EditMode.LENGTHEN)
    }

    @Test
    fun `the toolbar copies the bar, clears it, and undoes the last of them`() {
        // Given a note in the first bar
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        val written = viewModel.uiState.value.editor

        // When
        viewModel.onCopyBar()
        val copied = viewModel.uiState.value.editor
        viewModel.onClearBar()
        val cleared = viewModel.uiState.value.editor
        viewModel.onUndo()

        // Then
        assertThat(copied).isEqualTo(written.copyBar())
        assertThat(cleared).isEqualTo(copied.clearBar())
        assertThat(viewModel.uiState.value.editor).isEqualTo(cleared.undo())
    }

    @Test
    fun `a toolbar action without effect, its button disabled, does nothing when it comes all the same`() {
        // Given an empty music on its last bar: nothing to clear, to undo, nor a bar after it to copy over
        viewModel.onBarChosen(7)
        val before = viewModel.uiState.value

        // When
        viewModel.onCopyBar()
        viewModel.onClearBar()
        viewModel.onUndo()

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(before)
    }

    @Test
    fun `the tempo buttons speed the music up and slow it down`() {
        // When
        viewModel.onFaster()
        viewModel.onFaster()
        viewModel.onSlower()

        // Then
        assertThat(viewModel.uiState.value.editor.draft.tempo).isEqualTo(Tempo(130))
    }

    @Test
    fun `the fastest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`() {
        // Given the fastest tempo, then a cleared bar that can be undone
        repeat(TEMPO_STEPS) { viewModel.onFaster() }
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()
        val fastest = viewModel.uiState.value

        // When
        viewModel.onFaster()

        // Then
        assertThat(fastest.editor.draft.tempo).isEqualTo(Tempo(Tempo.FASTEST))
        assertThat(viewModel.uiState.value).isEqualTo(fastest)
    }

    @Test
    fun `the slowest tempo button, disabled, does nothing when it comes all the same, and keeps the undo`() {
        // Given the slowest tempo, then a cleared bar that can be undone
        repeat(TEMPO_STEPS) { viewModel.onSlower() }
        viewModel.onCellTouched(pitch("E4"), eighth = 0)
        viewModel.onClearBar()
        val slowest = viewModel.uiState.value

        // When
        viewModel.onSlower()

        // Then
        assertThat(slowest.editor.draft.tempo).isEqualTo(Tempo(Tempo.SLOWEST))
        assertThat(viewModel.uiState.value).isEqualTo(slowest)
    }

    @Test
    fun `the menu hides the other track in watermark, and shows it again`() {
        // When
        viewModel.onOtherTrackShown(false)
        val hidden = viewModel.uiState.value.showOtherTrack
        viewModel.onOtherTrackShown(true)

        // Then
        assertThat(hidden).isFalse()
        assertThat(viewModel.uiState.value.showOtherTrack).isTrue()
    }

    @Test
    fun `the factory that app builds makes an editor on a new music`() {
        // When
        val made = ComposerViewModel.Factory().create(ComposerViewModel::class, CreationExtras.Empty)

        // Then
        assertThat(made.uiState.value.editor).isEqualTo(EditorState.new())
    }

    private companion object {
        /** Enough steps of 10 to reach either end of the tempos, from anywhere between them. */
        const val TEMPO_STEPS = (Tempo.FASTEST - Tempo.SLOWEST) / Tempo.STEP
    }
}
