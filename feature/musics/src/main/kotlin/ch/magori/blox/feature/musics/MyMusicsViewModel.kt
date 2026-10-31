package ch.magori.blox.feature.musics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.audio.RingtoneExporter
import ch.magori.blox.core.audio.RingtoneKind
import ch.magori.blox.core.audio.ringtoneSeconds
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.data.MusicLibrary
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.data.freeName
import ch.magori.blox.core.data.keyName
import ch.magori.blox.core.data.lengthText
import ch.magori.blox.core.data.nameProblem
import ch.magori.blox.core.data.spokenLength
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToLong
import kotlin.reflect.KClass

/**
 * « Mes musiques » (V7, V10): the musics of the player saved in [library], and the one played during the games. Each
 * one may be listened to on the [music] player, the one the games play on, its card telling the bar it plays; chosen
 * for the games, renamed, or deleted after a confirmation, the deletion offered to be undone until the next action; a
 * deletion made in the editor, handed over by [deletedElsewhere], too. With a [ringtoneExporter] (Android 10 and
 * later), a music may become a ringtone or an alarm of the phone, in the sheet « Sonnerie ou réveil » (V42, ADR 0031),
 * written on [exportDispatcher], never on the main thread; its sheet tells the volume of the music at 0 %, read in
 * [preferences].
 */
class MyMusicsViewModel(
    private val library: MusicLibrary,
    private val music: MusicPlayer,
    private val deletedElsewhere: DeletedMusicRelay,
    preferences: UserPreferencesRepository,
    private val ringtoneExporter: RingtoneExporter?,
    private val exportDispatcher: CoroutineDispatcher,
) : ViewModel() {
    /** The saved musics and the one played during the games, the only reading of [library]; null until read. */
    private val content: StateFlow<Content?> =
        combine(library.musics, library.gameMusic, ::Content)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** The music being listened to, and the bar it plays; null when none is. */
    private val listened = MutableStateFlow<Listened?>(null)

    /** The playback of the music being listened to: stopping it never stops a music started since. */
    private var playback: Playback? = null

    /** Follows the [playback]: the bar it plays, and its stop by itself. */
    private var listenJob: Job? = null

    /** The dialog open, if any. */
    private val dialog = MutableStateFlow<MyMusicsDialog?>(null)

    /** The music deleted last, which « Annuler » puts back, until the next action; null when there is none. */
    private val deleted = MutableStateFlow<DeletedMusic?>(null)

    /** The actions of the player so far, which end the chance to undo a deletion: one made while it is written, too. */
    private var actionCount = 0

    /** The sheet « Sonnerie ou réveil » open, if any. */
    private val ringtone = MutableStateFlow<RingtoneSheetState?>(null)

    /** The playback of the music listened to in the sheet: stopping it never stops a music started since. */
    private var ringtonePlayback: Playback? = null

    /** Follows the [ringtonePlayback]: its stop by itself, or by another music played in its place. */
    private var ringtoneListenJob: Job? = null

    /** Whether the volume of the music is at 0 %, as the settings say; not until they are read. */
    private val musicMuted: StateFlow<Boolean> =
        preferences.preferences.map { it.musicVolume == Volume.SILENT }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    /** The sheet « Sonnerie ou réveil » as it is shown, telling the volume of the music at 0 %. */
    private val ringtoneShown = combine(ringtone, musicMuted) { sheet, muted -> sheet?.copy(musicMuted = muted) }

    /** What the screen shows; null while the musics are not read yet. */
    internal val uiState: StateFlow<MyMusicsUiState?> =
        combine(content.filterNotNull(), listened, dialog, deleted, ringtoneShown, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    init {
        // A music deleted in the editor, which closes on the list: its deletion may be undone as one made here.
        viewModelScope.launch {
            deletedElsewhere.handedOver.collect { handed ->
                // Not an action of the player on the list: actionCount, which only those raise, stays as it is.
                if (deletedElsewhere.take(handed)) deleted.value = handed
            }
        }
    }

    /** *Écouter* the music of [id], in place of the one being listened to; or *Arrêter* it, if it is listened to. */
    internal fun onListen(id: ScoreId.Personal) {
        endUndo()
        val again = listened.value?.id == id
        stopListening()
        if (again) return
        val score = savedMusic(id)?.score ?: return
        val started = music.play(score)
        playback = started
        listenJob = viewModelScope.launch {
            launch { followBars(id, score) }
            started.isPlaying.first { !it }
            stopListening()
        }
    }

    /**
     * *Utiliser* on the music of [id]: it is played during the games from now on; or *En jeu*, on the music played
     * during the games already: the games play the music chosen on the home screen again.
     */
    internal fun onUse(id: ScoreId.Personal) {
        endUndo()
        val current = content.value ?: return
        change { setGameMusic(id.takeUnless { it == current.gameMusic?.id }) }
    }

    /** *Renommer*, in the menu of the music of [id]: the dialog opens on its name. */
    internal fun onRename(id: ScoreId.Personal) {
        endUndo()
        val saved = savedMusic(id) ?: return
        dialog.value = MyMusicsDialog.Rename(id, saved.name)
    }

    /**
     * Why the music being renamed cannot be called [name]: blank, too long, or the name of another music, the case and
     * the spaces around it ignored (`nameProblem`, the rule of the library); null when it can.
     */
    internal fun renameProblem(name: String): NameProblem? =
        content.value?.musics.orEmpty().nameProblem(name, apartFrom = dialog.value?.id)

    /** The dialog *Renommer* confirmed with [name]: the music takes it, without the spaces around it, if it may. */
    internal fun onRenamed(name: String) {
        val renaming = dialog.value as? MyMusicsDialog.Rename ?: return
        if (renameProblem(name) != null) return
        change(onFailure = { failed(renaming) }) {
            rename(renaming.id, name.trim())
            closeDialog(renaming)
        }
    }

    /**
     * *Sonnerie ou réveil*, in the menu of the music of [id]: its sheet opens, Sonnerie chosen; never without an
     * exporter, the menu offering none then.
     */
    internal fun onRingtone(id: ScoreId.Personal) {
        if (ringtoneExporter == null) return
        endUndo()
        val saved = savedMusic(id) ?: return
        ringtone.value = RingtoneSheetState(
            id = id,
            name = saved.name,
            seconds = ringtoneSeconds(saved.score),
            kind = RingtoneKind.RINGTONE,
            phase = RingtonePhase.Choosing,
        )
    }

    /** *Écouter*, in the sheet « Sonnerie ou réveil »: its music plays, as the file will sound. */
    internal fun onRingtoneListen() {
        val sheet = ringtone.value ?: return
        if (sheet.listening) return stopRingtoneListening()
        val score = savedMusic(sheet.id)?.score ?: return
        val started = music.play(score)
        ringtonePlayback = started
        ringtone.value = sheet.copy(listening = true)
        ringtoneListenJob = viewModelScope.launch {
            started.isPlaying.first { !it }
            stopRingtoneListening()
        }
    }

    /** *Sonnerie* or *Réveil*, in the group « Pour » of the sheet: the use of the file, unless it is being written. */
    internal fun onRingtoneKindChosen(kind: RingtoneKind) {
        ringtone.update { sheet -> sheet?.takeIf { it.phase != RingtonePhase.Saving }?.copy(kind = kind) ?: sheet }
    }

    /**
     * *Enregistrer comme sonnerie* or *comme réveil*, or *Réessayer* after a failure: the music of the sheet is written
     * as a file of the use chosen. The sheet takes no choice until it is written; closed meanwhile, it does not cancel
     * the writing, which would leave a file whose URI is lost (RingtoneExporter).
     */
    internal fun onRingtoneSave() {
        val exporter = ringtoneExporter ?: return
        val sheet = ringtone.value?.takeIf { it.phase != RingtonePhase.Saving } ?: return
        val saved = savedMusic(sheet.id) ?: return
        ringtone.value = sheet.copy(phase = RingtonePhase.Saving)
        viewModelScope.launch {
            val phase = try {
                withContext(exportDispatcher) { exporter.export(saved.score, saved.name, sheet.kind) }
                RingtonePhase.Saved(sheet.kind)
            } catch (_: IOException) {
                // Nothing is left behind (RingtoneExporter): the player may try again.
                RingtonePhase.Failed
            }
            // The sheet may have been closed meanwhile, and another one opened: only the one waiting for this file ends.
            ringtone.update { shown ->
                if (shown?.id == sheet.id && shown.phase == RingtonePhase.Saving) shown.copy(phase = phase) else shown
            }
        }
    }

    /** The sheet « Sonnerie ou réveil » closed (*Terminé*, Back, swiped down): its music stops. */
    internal fun onRingtoneClosed() {
        stopRingtoneListening()
        ringtone.value = null
    }

    /** Stops the music listened to in the sheet, if any, and only it: a music started since keeps playing. */
    private fun stopRingtoneListening() {
        ringtoneListenJob?.cancel()
        ringtoneListenJob = null
        ringtonePlayback?.stop()
        ringtonePlayback = null
        ringtone.update { it?.copy(listening = false) }
    }

    /** *Supprimer*, in the menu of the music of [id]: a confirmation opens, telling whether the games play it. */
    internal fun onDelete(id: ScoreId.Personal) {
        endUndo()
        val saved = savedMusic(id) ?: return
        dialog.value = MyMusicsDialog.ConfirmDelete(id, saved.name, inGame = id == content.value?.gameMusic?.id)
    }

    /**
     * *Supprimer*, the deletion confirmed: once written, the music leaves the list, and stops if it was listened to;
     * « Annuler » may put it back until the next action. An action made while it is written ends that chance at once:
     * the message never shows.
     */
    internal fun onDeleteConfirmed() {
        val deleting = dialog.value as? MyMusicsDialog.ConfirmDelete ?: return
        val actions = actionCount
        change(onFailure = { failed(deleting) }) {
            val done = delete(deleting.id)
            closeDialog(deleting)
            if (listened.value?.id == deleting.id) stopListening()
            if (actionCount == actions) deleted.value = done
        }
    }

    /**
     * « Annuler », after a deletion: the music comes back at its place, played during the games again if it was; under a
     * free name (`freeName`, as the editor offers) if another music took its name since, from another screen. The
     * message leaves once the music is back; if it cannot be written, the message stays, to try again.
     */
    internal fun onUndo() {
        val restoring = deleted.value ?: return
        val musics = content.value?.musics.orEmpty()
        val name = musics.freeName(restoring.music.name, apartFrom = restoring.music.id)
        change {
            restore(restoring.copy(music = restoring.music.copy(name = name)))
            deleted.compareAndSet(restoring, null)
        }
    }

    /** *Fermer le message* of the deletion: it can no longer be undone. */
    internal fun onUndoDismissed() = endUndo()

    /** The dialog open closed, cancelled (*Annuler*, *Garder la musique*, Back): nothing changes. */
    internal fun onDialogDismissed() {
        dialog.value = null
    }

    /**
     * *Nouvelle* or *Au hasard*: the editor opens over the list, and plays its own music; the one listened to stops, and
     * the deletion can no longer be undone.
     */
    internal fun onEditorOpened() {
        endUndo()
        stopListening()
    }

    /**
     * The list is no longer visible (the application in the background): the music being listened to stops, on the
     * list or in the sheet, which stays open ([ch.magori.blox.core.designsystem.lifecycle.ScreenHiddenEffect]).
     */
    internal fun onHidden() = stopEveryListening()

    /** The screen leaves the stack: the music being listened to, on the list or in the sheet, stops with it. */
    override fun onCleared() = stopEveryListening()

    /** Stops the music listened to on the list and the one listened to in the sheet. */
    private fun stopEveryListening() {
        stopListening()
        stopRingtoneListening()
    }

    /**
     * Tells the bar of [score] being played, from the first, at its tempo, back to the first after the last: as the
     * player loops. Each bar starts at its own time from the start, rounded to the millisecond, so that the rounding
     * never adds up. The player does not tell where it is: the bar shown may run ahead of what is heard by the buffer
     * of the audio output, a few tenths of a second at most.
     */
    private suspend fun followBars(
        id: ScoreId.Personal,
        score: Score,
    ) {
        val bars = (score.voices.first().beats / score.beatsPerBar).toInt()
        val barMillis = score.beatsPerBar * MILLIS_PER_MINUTE / score.bpm
        var played = 0L
        while (true) {
            listened.value = Listened(id, Listening(bar = (played % bars).toInt() + 1, bars = bars))
            delay((barMillis * (played + 1)).roundToLong() - (barMillis * played).roundToLong())
            played++
        }
    }

    /** The saved music of [id], if the list is read and holds it. */
    private fun savedMusic(id: ScoreId.Personal): SavedMusic? = content.value?.musics?.firstOrNull { it.id == id }

    /** Stops the music being listened to, if any, and only it: a music started since keeps playing. */
    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        playback?.stop()
        playback = null
        listened.value = null
    }

    /**
     * An action of the player: the deletion can no longer be undone, its message leaves, and a deletion being written
     * will not offer it.
     */
    private fun endUndo() {
        actionCount++
        deleted.value = null
    }

    /** Closes [shown], if it is still the dialog open. */
    private fun closeDialog(shown: MyMusicsDialog) {
        dialog.compareAndSet(shown, null)
    }

    /** [shown], if it is still the dialog open, stays open and tells that its write failed. */
    private fun failed(shown: MyMusicsDialog) {
        val failed = when (shown) {
            is MyMusicsDialog.Rename -> shown.copy(failed = true)
            is MyMusicsDialog.ConfirmDelete -> shown.copy(failed = true)
        }
        dialog.compareAndSet(shown, failed)
    }

    /**
     * Changes the library by [write], which goes on after the change only once it is written. A write that fails (a full
     * disk) is dropped, and [onFailure] tells it: the list keeps showing the musics as they are saved, and the player may
     * try again.
     */
    private fun change(
        onFailure: () -> Unit = {},
        write: suspend MusicLibrary.() -> Unit,
    ) {
        viewModelScope.launch {
            try {
                library.write()
            } catch (_: IOException) {
                // Nothing written: the list shown is still the saved one.
                onFailure()
            }
        }
    }

    private fun toUiState(
        content: Content,
        listened: Listened?,
        dialog: MyMusicsDialog?,
        deleted: DeletedMusic?,
        ringtone: RingtoneSheetState?,
    ): MyMusicsUiState = MyMusicsUiState(
        musics = content.musics.map { saved ->
            MusicCard(
                id = saved.id,
                name = saved.name,
                category = saved.category,
                key = saved.scale.keyName(),
                length = lengthText(saved.score),
                spokenLength = spokenLength(saved.score),
                inGame = saved.id == content.gameMusic?.id,
                listening = listened?.takeIf { it.id == saved.id }?.listening,
            )
        },
        gameMusic = content.gameMusic?.name,
        dialog = dialog,
        deleted = deleted?.music?.name,
        ringtoneOffered = ringtoneExporter != null,
        ringtone = ringtone,
    )

    /**
     * Builds the [MyMusicsViewModel] with the musics of the player and the music player of the application, which
     * builds this factory (ADR 0013): the same player as the games and the home screen.
     */
    class Factory(
        private val library: MusicLibrary,
        private val music: MusicPlayer,
        private val deletedElsewhere: DeletedMusicRelay,
        private val preferences: UserPreferencesRepository,
        private val ringtoneExporter: RingtoneExporter?,
        private val exportDispatcher: CoroutineDispatcher,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST")
            return MyMusicsViewModel(
                library,
                music,
                deletedElsewhere,
                preferences,
                ringtoneExporter,
                exportDispatcher,
            ) as T
        }
    }

    /** The saved [musics], and the one played during the games, [gameMusic], null when there is none. */
    private data class Content(val musics: List<SavedMusic>, val gameMusic: SavedMusic?)

    /** The music of [id] being listened to, and where it is, [listening]. */
    private data class Listened(val id: ScoreId.Personal, val listening: Listening)

    private companion object {
        /** Keeps the list flowing across a configuration change, as the Android guidance advises. */
        const val STOP_TIMEOUT_MILLIS = 5_000L

        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
