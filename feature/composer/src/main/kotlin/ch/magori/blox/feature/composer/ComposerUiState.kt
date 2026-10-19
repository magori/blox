package ch.magori.blox.feature.composer

import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Tempo

/** What the editor shows: the [editor] and whether the notes of the other track are shown in watermark. */
internal data class ComposerUiState(val editor: EditorState, val showOtherTrack: Boolean) {
    /** Whether the tempo button « + » has an effect: not at the fastest tempo. */
    val canSpeedUp: Boolean get() = editor.draft.tempo.bpm < Tempo.FASTEST

    /** Whether the tempo button « − » has an effect: not at the slowest tempo. */
    val canSlowDown: Boolean get() = editor.draft.tempo.bpm > Tempo.SLOWEST

    /** Whether the open track has no note: the grid then says how to place one. */
    val isTrackEmpty: Boolean get() = editor.draft[editor.track].notes.isEmpty()
}
