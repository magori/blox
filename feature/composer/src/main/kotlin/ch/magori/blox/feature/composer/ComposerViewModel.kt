package ch.magori.blox.feature.composer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.PlayHead
import ch.magori.blox.feature.composer.model.Tempo
import ch.magori.blox.feature.composer.model.Track
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** The music editor (V8, V11): a new music, edited on the grid of one bar of one track, and listened to by [music]. */
class ComposerViewModel(private val music: MusicPlayer) : ViewModel() {
    private val state = MutableStateFlow(ComposerUiState(EditorState.new(), showOtherTrack = true))

    /** What the editor shows. */
    internal val uiState: StateFlow<ComposerUiState> = state.asStateFlow()

    /** The music listened to, if any. */
    private var listening: Listening? = null

    /** Whether *Suivre la lecture* is checked in the menu: each listening starts following the playback, or not. */
    private var followChosen = true

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

    /**
     * The [bar], counted from 0, shown on the grid. Another bar than the one played, while the grid follows the
     * playback, stops following it, and a message says so.
     */
    internal fun onBarChosen(bar: Int) = state.update {
        val played = it.played
        val leaves = it.followPlayback && played != null && played.bar != bar
        val chosen = it.copy(editor = it.editor.copy(bar = bar))
        if (leaves) chosen.copy(followPlayback = false, message = ComposerMessage.FOLLOW_STOPPED) else chosen
    }

    /**
     * *Suivre la lecture*, checked or not, in the menu: checked, the grid shows the bar played at once. Kept for the
     * next listenings, unlike following stopped by choosing another bar, which comes back with the next listening.
     */
    internal fun onFollowPlayback(follow: Boolean) {
        followChosen = follow
        state.update { it.copy(followPlayback = follow) }
        listening?.let { show(it.head) }
    }

    /** The message of the editor was shown. */
    internal fun onMessageShown() = state.update { it.copy(message = null) }

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
    internal fun onFaster() = changeTempo { if (it.canSpeedUp) it.copy(editor = it.editor.faster()) else it }

    /** The tempo button « − ». */
    internal fun onSlower() = changeTempo { if (it.canSlowDown) it.copy(editor = it.editor.slower()) else it }

    /** Applies [change] to the tempo; the music listened to plays on, at the new tempo. */
    private fun changeTempo(change: (ComposerUiState) -> ComposerUiState) {
        state.update(change)
        val current = listening ?: return
        music.setTempoFactor(state.value.editor.draft.tempo.bpm.toDouble() / current.playedTempo.bpm)
    }

    /** *Montrer l'autre piste en filigrane*, checked or not, in the menu. */
    internal fun onOtherTrackShown(shown: Boolean) = state.update { it.copy(showOtherTrack = shown) }

    /** *Écouter*: plays the music being edited, from its start; *Arrêter*, while it plays, stops it. */
    internal fun onListen() {
        if (listening != null) return stopListening()
        if (!state.value.canListen) return
        val draft = state.value.editor.draft
        val playback = music.play(draft.toScore(EDITED_SCORE_ID))
        // The watch starts once the music is shown as listened to: a playback that stopped already (the audio focus
        // refused) stops the listening at once, rather than before it began.
        val watch = viewModelScope.launch(start = CoroutineStart.LAZY) { watch(playback) }
        listening = Listening(playback, draft.tempo, watch)
        state.update { it.copy(followPlayback = followChosen) }
        show(PlayHead.START)
        watch.start()
    }

    /**
     * A frame was drawn at [frameNanos], while the music is listened to: the play head moves on by the ticks due since
     * the frame before, at the tempo of the music. The screen hands frames only while the music plays.
     */
    internal fun onFrame(frameNanos: Long) {
        val current = listening ?: return
        val tempo = state.value.editor.draft.tempo
        current.head = current.head.advance(current.frames.advance(frameNanos), tempo)
        show(current.head)
    }

    /** Shows the cell of [head] as played, and its bar on the grid while the grid follows the playback. */
    private fun show(head: PlayHead) = state.update {
        val cell = head.cell
        val editor = if (it.followPlayback) it.editor.copy(bar = cell.bar) else it.editor
        it.copy(editor = editor, played = cell)
    }

    /**
     * The editor went through [event]: once it is no longer visible (`ON_STOP`: the application in the background),
     * the music listened to stops. A pause alone, behind a dialog or in a split screen, lets it play on.
     */
    internal fun onLifecycleEvent(event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) stopListening()
    }

    /** The editor leaves the stack: the music listened to stops with it. */
    override fun onCleared() = stopListening()

    /**
     * Waits for [playback] to stop by itself (the audio output fails, the audio focus is lost for good, the headphones
     * are unplugged; a score has no natural end, it loops), then offers to listen again: the screen shows « Écouter »
     * once more.
     */
    private suspend fun watch(playback: Playback) {
        playback.isPlaying.first { !it }
        stopListening()
    }

    /** Stops the music listened to, if any, and only it: a music started since keeps playing. */
    private fun stopListening() {
        val current = listening ?: return
        current.watch.cancel()
        current.playback.stop()
        listening = null
        state.update { it.copy(played = null) }
    }

    /**
     * Applies [change] to the editor. A change of the notes stops the music listened to: it never goes on playing a
     * version that is gone.
     */
    private fun edit(change: EditorState.() -> EditorState) {
        val before = state.value.editor.draft
        state.update { it.copy(editor = it.editor.change()) }
        if (state.value.editor.draft != before) stopListening()
    }

    /** Builds the [ComposerViewModel] with the music player of the application; `app` builds this factory (ADR 0013). */
    class Factory(private val music: MusicPlayer) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return ComposerViewModel(music) as T
        }
    }

    /**
     * The music listened to: its [playback], which stops it and never a music started since, the tempo it was played
     * at, [playedTempo], from which the tempo factor of a later change counts, the [watch] that waits for it to stop by
     * itself, the [frames] of the screen turned into ticks, and the [head] they moved on.
     */
    private class Listening(val playback: Playback, val playedTempo: Tempo, val watch: Job) {
        val frames = FrameTicks()
        var head = PlayHead.START
    }
}

/**
 * The identity of the music being edited, while it is not saved. Out of [ComposerViewModel], so that `ScoreId` is not
 * part of its API.
 */
internal val EDITED_SCORE_ID: ScoreId = ScoreId.Personal("editor")
