package ch.magori.blox.feature.composer

import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.Surprise
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.Tempo

/**
 * What the editor shows: the [editor], whether the notes of the other track are shown in watermark, the cell
 * [played] while the music is listened to, `null` otherwise, and whether the grid follows it, showing its bar
 * (*Suivre la lecture*), the [message] to show once, if any, the [name] of the music, null until it is named, whether
 * it is the music played during the games, [isGameMusic], the window over the editor, [dialog], if any,
 * and how the screen is to leave the editor, [exit], once it is to; the category, or Surprise, the dice
 * composes in, [choice].
 */
internal data class ComposerUiState(
    val editor: EditorState,
    val showOtherTrack: Boolean,
    val played: Cell? = null,
    val followPlayback: Boolean = true,
    val message: ComposerMessage? = null,
    val name: String? = null,
    val isGameMusic: Boolean = false,
    val dialog: ComposerDialog? = null,
    val exit: ComposerExit? = null,
    val choice: CategoryChoice = Surprise,
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

    /** The editor changed to [editor]; once its undo is gone, the message that told of it goes with it. */
    fun edited(editor: EditorState): ComposerUiState {
        val gone = message?.tellsUndo == true && !editor.canUndo
        return copy(editor = editor, message = if (gone) null else message)
    }
}

/**
 * A message the editor shows once, then forgets; one that [tellsUndo], that the action just done can be undone, goes away
 * once the undo is gone, used or taken away.
 */
internal enum class ComposerMessage(val tellsUndo: Boolean = false) {
    /** Choosing another bar than the one played stopped following the playback. */
    FOLLOW_STOPPED,

    /** The musics could not be written on the device: nothing was saved, renamed or deleted. */
    WRITE_FAILED,

    /** The dice replaced the music, which *Annuler* brings back. */
    DICE_ROLLED(tellsUndo = true),

    /** *Copier la mesure* copied the bar over the next one, which *Annuler* brings back. */
    BAR_COPIED(tellsUndo = true),

    /** *Effacer la mesure* cleared the bar, which *Annuler* brings back. */
    BAR_CLEARED(tellsUndo = true),
}
