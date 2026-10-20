package ch.magori.blox.feature.composer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.SavedMusic.Companion.MAX_NAME_LENGTH
import ch.magori.blox.core.data.freeName
import ch.magori.blox.core.data.proposedName
import ch.magori.blox.core.designsystem.frame.FrameTicks
import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.MusicGenerator
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.feature.composer.model.DraftForm
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.PlayHead
import ch.magori.blox.feature.composer.model.SaveProblem
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Tempo
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.renameProblem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * The music editor (V8, V11): the music of [musicKey] in [library], or a new one when it is null, edited on the grid of
 * one bar of one track, and listened to by [music]. The music the library does not know, or that does not open on the
 * grid, opens as a new music, under a new identity: the saved one is never overwritten. The dice draws its seeds from
 * [random] and composes on [generation], off the main thread; it rolls once the music opens if it is to [rollDice].
 */
class ComposerViewModel internal constructor(
    private val music: MusicPlayer,
    private val library: MusicLibrary,
    musicKey: String?,
    savedState: SavedStateHandle,
    private val random: Random,
    private val generation: CoroutineDispatcher,
    rollDice: Boolean,
) : ViewModel() {
    private val state = MutableStateFlow<ComposerUiState?>(null)

    /** What the editor shows; null until its music is open, which a saved music is once the library is read. */
    internal val uiState: StateFlow<ComposerUiState?> = state.asStateFlow()

    /** What the editor shows, once its music is open: no action comes before, the screen shows none. */
    private val current: ComposerUiState get() = checkNotNull(state.value) { "The music is not open yet" }

    /** The identity of the music: the one it is listened to and saved under. */
    private lateinit var id: ScoreId.Personal

    /** The saved musics, as the library last gave them: the names a music may not take. */
    private var musics: List<SavedMusic> = emptyList()

    /** The identity of the music played during the games, if any. */
    private var gameMusic: ScoreId.Personal? = null

    /** The change of the musics under way, if any: a second tap on its button meanwhile does nothing. */
    private var writing: Job? = null

    /** The roll of the dice under way, if any: a second roll meanwhile does nothing. */
    private var rolling: Job? = null

    /**
     * Whether a window was shown since the roll of the dice under way began: the music being saved, renamed or deleted
     * then, the roll leaves its music aside.
     */
    private var windowShown = false

    /** Whether the dice is to roll once the music opens: the editor opened by *Au hasard*. */
    private var rollOnOpen = false

    /** The music listened to, if any. */
    private var listening: Listening? = null

    /** Whether *Suivre la lecture* is checked in the menu: each listening starts following the playback, or not. */
    private var followChosen = true

    init {
        // The identity of the music, kept in the saved state once opened: after the death of the process, the editor
        // opens the same music, a new one included, with the draft and the name it kept.
        val ownKey = savedState.get<String>(ID_KEY)
        // Rolled once, when the music opens; an editor restored keeps the music it rolled.
        rollOnOpen = rollDice && ownKey == null
        val key = ownKey ?: musicKey
        val restored = Restored(
            draft = savedState.get<String>(DRAFT_KEY)?.let(DraftForm::read),
            name = savedState.get<String>(NAME_KEY),
        )
        if (key == null) open(library.newId(), saved = null, restored)
        viewModelScope.launch {
            library.musics.collect { saved ->
                musics = saved
                if (state.value == null) openSaved(checkNotNull(key), isOwn = ownKey != null, restored)
                update { it.copy(dialog = it.dialog?.checked()) }
            }
        }
        viewModelScope.launch {
            library.gameMusic.collect { played ->
                gameMusic = played?.id
                update { it.copy(isGameMusic = played?.id == id) }
            }
        }
        viewModelScope.launch {
            state.filterNotNull().map { it.editor.draft to it.name }.distinctUntilChanged().collect { (draft, name) ->
                savedState[ID_KEY] = id.key
                savedState[DRAFT_KEY] = DraftForm.write(draft)
                savedState[NAME_KEY] = name
            }
        }
    }

    /**
     * Opens the saved music of [key], as the [restored] draft shows it if any, or else, if it [isOwn], the identity of
     * the new music this editor opened before the death of the process, that music with its [restored] draft and name;
     * or a new music under a new identity.
     */
    private fun openSaved(
        key: String,
        isOwn: Boolean,
        restored: Restored,
    ) {
        val stored = musics.firstOrNull { it.id.key == key }
        val draft = stored?.let { ScoreDraft.fromScore(it.score, it.scale, it.category) }
        when {
            stored != null && draft != null -> open(stored.id, saved = draft, restored.copy(name = stored.name))
            isOwn -> open(ScoreId.Personal(key), saved = null, restored)
            else -> open(library.newId(), saved = null, Restored())
        }
    }

    /**
     * Opens the music of [id], as it was [saved] (null for a new music), as the [restored] draft shows it, and under
     * its name, if any.
     */
    private fun open(
        id: ScoreId.Personal,
        saved: ScoreDraft?,
        restored: Restored,
    ) {
        this.id = id
        val editor = EditorState(draft = restored.draft ?: saved ?: ScoreDraft.new(), saved = saved)
        state.value =
            ComposerUiState(editor, showOtherTrack = true, name = restored.name, isGameMusic = gameMusic == id)
        if (rollOnOpen) {
            rollOnOpen = false
            onDice()
        }
    }

    /** What the saved state kept of the music: its [draft] and its [name], if any. */
    private data class Restored(val draft: ScoreDraft? = null, val name: String? = null)

    /**
     * Applies [change] to what the editor shows, once its music is open. A window shown meanwhile is noted for the roll
     * of the dice under way, which then leaves its music aside.
     */
    private fun update(change: (ComposerUiState) -> ComposerUiState) {
        state.update { it?.let(change) }
        if (state.value?.dialog != null) windowShown = true
    }

    /**
     * *Enregistrer*: opens the save window on the name of the music, or for a music never named on the name proposed
     * after [base], the name of its category (« Joyeux 1 »), and on whether it is played during the games.
     */
    internal fun onSaveAsked(base: String) = askToSave(base, thenLeave = false)

    private fun askToSave(
        base: String,
        thenLeave: Boolean,
    ) = update {
        val name = it.name ?: musics.proposedName(base, apartFrom = id)
        it.copy(dialog = ComposerDialog.Save(name, forGames = it.isGameMusic, thenLeave = thenLeave).checked())
    }

    /** The name typed in the window, cut at the longest a saved name may be. */
    internal fun onNameChanged(name: String) = update {
        val cut = name.take(name.offsetByCodePoints(0, minOf(name.codePointCount(0, name.length), MAX_NAME_LENGTH)))
        val dialog = it.dialog
        if (dialog is ComposerDialog.Naming) it.copy(dialog = dialog.named(cut)) else it
    }

    /** *Prendre « X 2 »*: the free name offered in place of a name taken. */
    internal fun onFreeNameTaken() = update {
        val dialog = it.dialog
        val free = (dialog as? ComposerDialog.Naming)?.freeName
        if (free != null) it.copy(dialog = dialog.named(free)) else it
    }

    /**
     * *Renommer*, in the menu or on the title: opens the window on the name of the music, or for a music never named on
     * the name proposed after [base], as the save window does.
     */
    internal fun onRenameAsked(base: String) = update {
        val name = it.name ?: musics.proposedName(base, apartFrom = id)
        it.copy(dialog = ComposerDialog.Rename(name).checked())
    }

    /**
     * *Renommer* in the window: a saved music is renamed in the musics at once, its changes left as they are; a music
     * never saved takes the name, which the save window then proposes. A write that fails renames nothing; the window
     * stays open, saying so.
     */
    internal fun onRenameConfirmed() {
        val dialog = current.dialog as? ComposerDialog.Rename ?: return
        if (dialog.problem != null) return
        val name = dialog.name.trim()
        val stored = current.editor.saved != null
        write {
            if (stored) library.rename(id, name)
            update { it.copy(name = name, dialog = null) }
        }
    }

    /** *Supprimer*, in the menu, for a saved music only: asks first. */
    internal fun onDeleteAsked() = update {
        if (it.editor.saved == null) it else it.copy(dialog = ComposerDialog.Delete(isGameMusic = it.isGameMusic))
    }

    /**
     * *Supprimer* in the question: deletes the music, and leaves the editor with what was deleted, which the screen
     * before may one day restore (P4.19e). A write that fails deletes nothing; the question stays open, saying so.
     */
    internal fun onDeleteConfirmed() {
        if (current.dialog !is ComposerDialog.Delete) return
        write {
            val deleted = library.delete(id)
            val exit = if (deleted == null) ComposerExit.Leave else ComposerExit.Deleted(deleted)
            update { it.copy(dialog = null, exit = exit) }
        }
    }

    /** *Jouer cette musique pendant les parties*, checked or not, in the save window. */
    internal fun onForGamesChanged(forGames: Boolean) = update {
        val dialog = it.dialog
        if (dialog is ComposerDialog.Save) it.copy(dialog = dialog.copy(forGames = forGames)) else it
    }

    /** The window closed without an answer, or *Continuer*: nothing changes. */
    internal fun onDialogDismissed() = update { it.copy(dialog = null) }

    /**
     * *Enregistrer* in the window: saves the music under its identity and the name typed, plays it during the games or
     * no longer as the box says, and counts the changes from there. A window that shows a problem saves nothing. A write
     * that fails leaves the window open, saying so. Once saved, the music is saved even if choosing it for the games
     * then fails.
     */
    internal fun onSaveConfirmed() {
        val dialog = current.dialog as? ComposerDialog.Save ?: return
        if (dialog.problem != null) return
        val draft = current.editor.draft
        val name = dialog.name.trim()
        write {
            library.save(SavedMusic(id, name, draft.category, draft.scale, draft.toScore(id)))
            // Saved from here, whatever comes next: a failure to choose it for the games leaves it saved, deletable.
            update { it.copy(editor = it.editor.copy(saved = draft), name = name) }
            if (dialog.forGames != (gameMusic == id)) library.setGameMusic(id.takeIf { dialog.forGames })
            update { it.copy(dialog = null, exit = if (dialog.thenLeave) ComposerExit.Leave else null) }
        }
    }

    /**
     * Retour, the button or the gesture of the system: leaves the editor at once when nothing would be lost, else asks
     * first (*Enregistrer*, *Quitter sans enregistrer*, *Continuer*).
     */
    internal fun onBackAsked() = update {
        if (it.editor.asksBeforeLeaving) it.copy(dialog = ComposerDialog.Leave) else it.copy(exit = ComposerExit.Leave)
    }

    /** *Quitter sans enregistrer*: leaves the editor, the changes lost. */
    internal fun onLeaveWithoutSaving() = update { it.copy(dialog = null, exit = ComposerExit.Leave) }

    /** *Enregistrer* in the question before leaving: the save window, which leaves the editor once the music is saved. */
    internal fun onSaveBeforeLeaving(base: String) = askToSave(base, thenLeave = true)

    /** The screen has left the editor as [ComposerUiState.exit] said: it leaves once. */
    internal fun onExitDone() = update { it.copy(exit = null) }

    /**
     * Changes the musics by [change], once at a time: a second tap meanwhile does nothing. A failure to write them leaves
     * the window open, saying so, its name and its box as they were; without a window, a message says so.
     */
    private fun write(change: suspend () -> Unit) {
        if (writing?.isActive == true) return
        writing = viewModelScope.launch {
            try {
                change()
            } catch (_: IOException) {
                update {
                    val dialog = it.dialog
                    if (dialog ==
                        null
                    ) {
                        it.copy(message = ComposerMessage.WRITE_FAILED)
                    } else {
                        it.copy(dialog = dialog.failed())
                    }
                }
            }
        }
    }

    /**
     * The window with the problem of its name, given the saved musics and the music, and the free name offered when its
     * name is taken; a window without a name as it is.
     */
    private fun ComposerDialog.checked(): ComposerDialog = when (this) {
        is ComposerDialog.Save -> {
            val problem = current.editor.saveProblem(name, musics, apartFrom = id)
            copy(problem = problem, freeName = freeNameIf(problem, name))
        }

        is ComposerDialog.Rename -> {
            val problem = renameProblem(name, musics, apartFrom = id)
            copy(problem = problem, freeName = freeNameIf(problem, name))
        }

        ComposerDialog.Leave, is ComposerDialog.Delete -> this
    }

    /** The free name offered in place of [name] when the [problem] is that it is taken; null otherwise. */
    private fun freeNameIf(
        problem: SaveProblem?,
        name: String,
    ): String? = if (problem == SaveProblem.NAME_TAKEN) musics.freeName(name, apartFrom = id) else null

    /** The window, saying that writing the musics failed; a window that writes nothing as it is. */
    private fun ComposerDialog.failed(): ComposerDialog = when (this) {
        is ComposerDialog.Save -> copy(failed = true)
        is ComposerDialog.Rename -> copy(failed = true)
        is ComposerDialog.Delete -> copy(failed = true)
        ComposerDialog.Leave -> this
    }

    /** The window with the [name] typed, checked. */
    private fun ComposerDialog.Naming.named(name: String): ComposerDialog = when (this) {
        is ComposerDialog.Save -> copy(name = name)
        is ComposerDialog.Rename -> copy(name = name)
    }.checked()

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
    internal fun onBarChosen(bar: Int) = update {
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
        update { it.copy(followPlayback = follow) }
        listening?.let { show(it.head) }
    }

    /** A category, or Surprise, chosen for the dice. */
    internal fun onCategoryChosen(choice: CategoryChoice) = update { it.copy(choice = choice) }

    /**
     * *Au hasard*: the music the generator composes in the category chosen, from a seed drawn for this roll, in place of
     * the music being edited. The generator takes some tens of milliseconds: it composes off the main thread, and a second
     * roll meanwhile does nothing. It does not roll behind a window, and leaves its music aside once a window was shown
     * while it composed (a save, a rename, a deletion); a note touched meanwhile is replaced, *Annuler* brings it back.
     */
    internal fun onDice() {
        if (rolling?.isActive == true || current.dialog != null) return
        windowShown = false
        val choice = current.choice
        val seed = random.nextLong()
        rolling = viewModelScope.launch {
            val music = withContext(generation) { MusicGenerator.generate(choice, seed, id) }
            // Every music of the generator opens on the grid (ScoreDraftRoundTripTest).
            val draft = checkNotNull(ScoreDraft.fromScore(music.score, music.scale, music.category)) {
                "A generated music does not open on the grid"
            }
            if (!windowShown) editUndoable(ComposerMessage.DICE_ROLLED) { rollDice(draft) }
        }
    }

    /** The message of the editor was shown. */
    internal fun onMessageShown() = update { it.copy(message = null) }

    /** What touching a cell does from now on. */
    internal fun onModeChosen(mode: EditMode) = edit { copy(mode = mode) }

    // A toolbar or tempo action comes only from its button, disabled when it has no effect; one that comes all the
    // same, a second tap before the button is disabled, does nothing, and keeps the undo.

    /** *Copier la mesure*. */
    internal fun onCopyBar() = editUndoable(ComposerMessage.BAR_COPIED) { if (canCopyBar) copyBar() else this }

    /** *Effacer la mesure*. */
    internal fun onClearBar() = editUndoable(ComposerMessage.BAR_CLEARED) { if (canClearBar) clearBar() else this }

    /** *Annuler*. */
    internal fun onUndo() = edit { if (canUndo) undo() else this }

    /** The tempo button « + ». */
    internal fun onFaster() = changeTempo { if (it.canSpeedUp) it.edited(it.editor.faster()) else it }

    /** The tempo button « − ». */
    internal fun onSlower() = changeTempo { if (it.canSlowDown) it.edited(it.editor.slower()) else it }

    /** Applies [change] to the tempo; the music listened to plays on, at the new tempo. */
    private fun changeTempo(change: (ComposerUiState) -> ComposerUiState) {
        update(change)
        val playing = listening ?: return
        music.setTempoFactor(current.editor.draft.tempo.bpm.toDouble() / playing.playedTempo.bpm)
    }

    /** *Montrer l'autre piste en filigrane*, checked or not, in the menu. */
    internal fun onOtherTrackShown(shown: Boolean) = update { it.copy(showOtherTrack = shown) }

    /** *Écouter*: plays the music being edited, from its start; *Arrêter*, while it plays, stops it. */
    internal fun onListen() {
        if (listening != null) return stopListening()
        if (!current.canListen) return
        val draft = current.editor.draft
        val playback = music.play(draft.toScore(id))
        // The watch starts once the music is shown as listened to: a playback that stopped already (the audio focus
        // refused) stops the listening at once, rather than before it began.
        val watch = viewModelScope.launch(start = CoroutineStart.LAZY) { watch(playback) }
        listening = Listening(playback, draft.tempo, watch)
        update { it.copy(followPlayback = followChosen) }
        show(PlayHead.START)
        watch.start()
    }

    /**
     * A frame was drawn at [frameNanos], while the music is listened to: the play head moves on by the ticks due since
     * the frame before, at the tempo of the music. The screen hands frames only while the music plays.
     */
    internal fun onFrame(frameNanos: Long) {
        val playing = listening ?: return
        val tempo = current.editor.draft.tempo
        playing.head = playing.head.advance(playing.frames.advance(frameNanos), tempo)
        show(playing.head)
    }

    /** Shows the cell of [head] as played, and its bar on the grid while the grid follows the playback. */
    private fun show(head: PlayHead) = update {
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
        val playing = listening ?: return
        playing.watch.cancel()
        playing.playback.stop()
        listening = null
        update { it.copy(played = null) }
    }

    /**
     * Applies [change] to the editor. A change of the notes stops the music listened to: it never goes on playing a
     * version that is gone. Once the undo is gone, the message that told of it goes with it.
     */
    private fun edit(change: EditorState.() -> EditorState) {
        val before = current.editor.draft
        update { it.edited(it.editor.change()) }
        if (current.editor.draft != before) stopListening()
    }

    /**
     * Applies [change], an action that can be undone, to the editor; once done, with an undo kept, [message] says it can
     * be undone.
     */
    private fun editUndoable(
        message: ComposerMessage,
        change: EditorState.() -> EditorState,
    ) {
        val before = current.editor
        edit(change)
        val after = current.editor
        if (after != before && after.canUndo) update { it.copy(message = message) }
    }

    /**
     * Builds the [ComposerViewModel] with the music player and the musics of the application; `app` builds this factory
     * (ADR 0013). The identity of the music to open comes from the navigation entry, in the creation extras under
     * [MusicKey] (absent for a new music), whether to roll the dice once it opens under [RollDice], with the saved state
     * of the entry, which keeps the draft. The dice draws its seeds from [random] and composes on [generation].
     */
    class Factory(
        private val music: MusicPlayer,
        private val library: MusicLibrary,
        private val random: Random = Random.Default,
        private val generation: CoroutineDispatcher = Dispatchers.Default,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return ComposerViewModel(
                music,
                library,
                extras[MusicKey],
                extras.createSavedStateHandle(),
                random,
                generation,
                extras[RollDice] == true,
            ) as T
        }
    }

    companion object {
        /** Where the navigation entry puts the key of the music to open, `ScoreId.Personal.key`, for [Factory]. */
        val MusicKey: CreationExtras.Key<String> = object : CreationExtras.Key<String> {}

        /** Where the navigation entry tells [Factory] to roll the dice once the music opens: *Au hasard*. */
        val RollDice: CreationExtras.Key<Boolean> = object : CreationExtras.Key<Boolean> {}

        /** The keys, in the saved state of the entry, of the identity, the draft and the name of the music. */
        private const val ID_KEY = "ch.magori.blox.feature.composer.id"
        private const val DRAFT_KEY = "ch.magori.blox.feature.composer.draft"
        private const val NAME_KEY = "ch.magori.blox.feature.composer.name"
    }

    /**
     * The music listened to: its [playback], which stops it and never a music started since, the tempo it was played
     * at, [playedTempo], from which the tempo factor of a later change counts, the [watch] that waits for it to stop by
     * itself, the [frames] of the screen turned into ticks, and the [head] they moved on.
     */
    private class Listening(val playback: Playback, val playedTempo: Tempo, val watch: Job) {
        // No cap per frame, unlike a game: the music goes on in real time while frames are skipped, and the play head
        // catches up every tick to follow it.
        val frames = FrameTicks()
        var head = PlayHead.START
    }
}
