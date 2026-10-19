package ch.magori.blox.feature.composer

import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Tempo

/**
 * What the editor shows: the [editor], whether the notes of the other track are shown in watermark, the cell
 * [played] while the music is listened to, `null` otherwise, and whether the grid follows it, showing its bar
 * (*Suivre la lecture*), and the [message] to show once, if any.
 */
internal data class ComposerUiState(
    val editor: EditorState,
    val showOtherTrack: Boolean,
    val played: Cell? = null,
    val followPlayback: Boolean = true,
    val message: ComposerMessage? = null,
) {
    /** Whether the tempo button « + » has an effect: not at the fastest tempo. */
    val canSpeedUp: Boolean get() = editor.draft.tempo.bpm < Tempo.FASTEST

    /** Whether the tempo button « − » has an effect: not at the slowest tempo. */
    val canSlowDown: Boolean get() = editor.draft.tempo.bpm > Tempo.SLOWEST

    /**
     * Whether *Écouter* has an effect: not while neither track has a note. A music listened to always has one: a change
     * of the notes stops it.
     */
    val canListen: Boolean get() = !editor.draft.isEmpty

    /** Whether the open track has no note: the grid then says how to place one. */
    val isTrackEmpty: Boolean get() = editor.draft[editor.track].notes.isEmpty()
}

/** A message the editor shows once, then forgets. */
internal enum class ComposerMessage {
    /** Choosing another bar than the one played stopped following the playback. */
    FOLLOW_STOPPED,
}
