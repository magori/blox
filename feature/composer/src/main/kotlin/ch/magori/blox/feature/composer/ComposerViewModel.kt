package ch.magori.blox.feature.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

/** The music editor (V8, V11): a new music, edited on the grid of one bar of one track. */
class ComposerViewModel : ViewModel() {
    private val state = MutableStateFlow(ComposerUiState(EditorState.new(), showOtherTrack = true))

    /** What the editor shows. */
    internal val uiState: StateFlow<ComposerUiState> = state.asStateFlow()

    /** The [eighth] cell of the selected bar touched on the line of [pitch]. */
    internal fun onCellTouched(
        pitch: Pitch,
        eighth: Int,
    ) = edit { touch(pitch, eighth) }

    /** The [fromEighth] cell of the selected bar touched on the line of [pitch], and the finger slid to [toEighth]. */
    internal fun onCellSlid(
        pitch: Pitch,
        fromEighth: Int,
        toEighth: Int,
    ) = edit { slide(pitch, fromEighth, toEighth) }

    /** The tab of [track] opened. */
    internal fun onTrackChosen(track: Track) = edit { copy(track = track) }

    /** The [bar], counted from 0, shown on the grid. */
    internal fun onBarChosen(bar: Int) = edit { copy(bar = bar) }

    /** What touching a cell does from now on. */
    internal fun onModeChosen(mode: EditMode) = edit { copy(mode = mode) }

    // A toolbar or tempo action comes only from its button, disabled when it has no effect; one that comes all the
    // same, a second tap before the button is disabled, does nothing, and keeps the undo.

    /** *Copier la mesure*. */
    internal fun onCopyBar() = edit { if (canCopyBar) copyBar() else this }

    /** *Effacer la mesure*. */
    internal fun onClearBar() = edit { if (canClearBar) clearBar() else this }

    /** *Annuler*. */
    internal fun onUndo() = edit { if (canUndo) undo() else this }

    /** The tempo button « + ». */
    internal fun onFaster() = state.update { if (it.canSpeedUp) it.copy(editor = it.editor.faster()) else it }

    /** The tempo button « − ». */
    internal fun onSlower() = state.update { if (it.canSlowDown) it.copy(editor = it.editor.slower()) else it }

    /** *Montrer l'autre piste en filigrane*, checked or not, in the menu. */
    internal fun onOtherTrackShown(shown: Boolean) = state.update { it.copy(showOtherTrack = shown) }

    private fun edit(change: EditorState.() -> EditorState) = state.update { it.copy(editor = it.editor.change()) }

    /** Builds the [ComposerViewModel]; `app` builds this factory (ADR 0013). */
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return ComposerViewModel() as T
        }
    }
}
