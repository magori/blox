package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.audio.LoopListening
import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.SoundEffectPlayer
import ch.magori.blox.core.audio.playCues
import ch.magori.blox.core.data.CurrentGarden
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.data.cutName
import ch.magori.blox.core.data.nameProblem
import ch.magori.blox.core.data.proposedName
import ch.magori.blox.core.designsystem.frame.FramePlayHead
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenComposer
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
import ch.magori.blox.core.garden.toScore
import ch.magori.blox.feature.garden.sound.GARDEN_WARM_UP
import ch.magori.blox.feature.garden.sound.previewCuesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * The musical garden (V40, docs/jardin-musical.md, ADR 0027): the garden under way, which opens on the garden [kept],
 * and each garden adopted ([adopt]) kept in its place, without a button. Unreadable, the garden kept is the empty one
 * (the contract of [GardenRepository]).
 *
 * The garden is written in [appScope], which outlives the screen: a change made just before leaving is kept. The
 * writes, one at a time under [writes], keep the order of the changes, the last garden adopted being the one kept, as
 * long as [appScope] dispatches at once (`Dispatchers.Main.immediate`): each write then asks for [writes] in the order
 * of the changes. The garden kept is read under [writes] too: a visit opens on the garden of the previous one, once
 * kept.
 *
 * The sound (P10.12, docs/jardin-musical.md, Son): Écouter plays the score of the garden in a loop by the [music]
 * player of the application, as a preview of « Mes musiques » (one music at a time, the audio focus asked for); each
 * garden adopted meanwhile is switched to at the bar line; the score stops once the screen is no longer visible, or
 * left. The column played moves on with the frames the screen hands over ([onFrame], ADR 0012). The preview is heard by
 * the [soundEffects], its notes once in a rising arpeggio, when the [preferences] want the sound effects: the notes of
 * the garden, a finite set, are warmed up as it opens, never released (ADR 0025, ADR 0027).
 */
class GardenViewModel internal constructor(
    private val kept: GardenRepository,
    private val library: GardenLibrary,
    private val appScope: CoroutineScope,
    private val writes: Mutex,
    private val music: MusicPlayer,
    private val soundEffects: SoundEffectPlayer,
    preferences: UserPreferencesRepository,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val state = MutableStateFlow<GardenUiState?>(null)

    /** The screen; null while the garden kept is being read (P10.10 decides what the screen shows meanwhile). */
    internal val uiState: StateFlow<GardenUiState?> = state.asStateFlow()

    /** The time « Annuler » is still offered, counted from the last Tout effacer or Au hasard. */
    private var undoTime: Job? = null

    /** The garden listened to, if it is. */
    private var listening: Listening? = null

    /** Whether the preview is heard, from the settings; false until they answer, as in the Locksmith. */
    private var soundEffectsOn = false

    /** The notes of the last preview heard, those still to come; cut by the next preview. */
    private var arpeggio: Job? = null

    /** What is known of « Mes jardins », kept here until the garden kept is read. */
    private var myGardens: MyGardens = MyGardens.Loading

    /** The time « « X » gardé. » still shows, counted from the last garden kept. */
    private var keptMessage: Job? = null

    init {
        // Read once: only this screen writes the garden kept. A garden adopted meanwhile is not replaced by it. Any
        // failure other than an unreadable garden (which the repository absorbs) is a bug, and is not swallowed here.
        viewModelScope.launch {
            val read = writes.withLock { kept.current.first() }
            // A garden adopted meanwhile stays; it was kept as coming from the same kept garden.
            state.update {
                it?.copy(keptFrom = read.keptFrom)
                    ?: GardenUiState(read.garden, keptFrom = read.keptFrom, myGardens = myGardens)
            }
        }
        // Followed while the screen lives: the dot of the disc and the sheet follow each change of the list. A flow that
        // ends gave the empty list of gardens that cannot be read (the contract of GardenLibrary).
        viewModelScope.launch {
            library.gardens
                .onCompletion { failure -> if (failure == null) showMyGardens(MyGardens.Unreadable) }
                .collect { showMyGardens(MyGardens.Listed(it)) }
        }
        // Read once, at the opening of the screen, as the Locksmith does. Unreadable settings give the defaults (the
        // contract of the repository).
        viewModelScope.launch {
            soundEffectsOn = preferences.preferences.first().soundEffects
            if (soundEffectsOn) soundEffects.warmUp(GARDEN_WARM_UP)
        }
    }

    /** Mes jardins: opens the sheet of the kept gardens. Nothing until the garden kept is read. */
    internal fun openMyGardens() {
        state.update { it?.copy(sheet = GardensSheet()) }
    }

    /** Closes the sheet « Mes jardins », by the system back or a touch on its scrim. */
    internal fun closeMyGardens() {
        state.update { it?.copy(sheet = null) }
    }

    /**
     * Garder ce jardin, Garder une copie: opens the window of its name on the one proposed, « Jardin » then the name of
     * its mood as the screen shows it, [moodName], in lower case, numbered if another garden has it. Nothing when the
     * garden cannot be kept ([GardenUiState.keepRefusal]).
     */
    internal fun startKeeping(moodName: String) {
        state.update { current ->
            val sheet = current?.sheet
            if (sheet == null || current.keepRefusal != null) return@update current
            current.copy(sheet = sheet.copy(naming = GardenNaming(current.keptGardens.proposedName(moodName))))
        }
    }

    /**
     * The name [typed] in the window, cut at the longest a name may be, and why it is refused, if it is; a failure told
     * before is no longer.
     */
    internal fun changeName(typed: String) {
        state.update { current ->
            val sheet = current?.sheet ?: return@update current
            val naming = sheet.naming ?: return@update current
            val name = cutName(typed)
            val problem = current.keptGardens.nameProblem(name)
            current.copy(sheet = sheet.copy(naming = GardenNaming(name = name, problem = problem)))
        }
    }

    /** Annuler: the window of the name closes, the sheet still open. */
    internal fun cancelNaming() {
        state.update { current -> current?.sheet?.let { current.copy(sheet = it.copy(naming = null)) } ?: current }
    }

    /**
     * Garder, in the window of the name: keeps the garden under way under the name typed, then keeps the garden under
     * way as coming from it, both in the scope of the application, one after the other under [writes]: the screen left
     * meanwhile, the garden is linked all the same. A name refused keeps nothing. A garden that cannot be kept leaves
     * the window open, saying so; kept, but whose link cannot be written, it is listed without being linked, and the
     * sheet says the operation failed. Once at a time: the sheet is greyed while it is written.
     */
    internal fun confirmKeeping(messageMillis: Long) {
        val current = state.value ?: return
        val sheet = current.sheet ?: return
        val naming = sheet.naming ?: return
        if (sheet.writing || current.keepRefusal != null || current.keptGardens.nameProblem(naming.name) != null) return
        val library = library
        val kept = kept
        writeGardens(messageMillis) {
            val keptGarden = try {
                library.keep(naming.name.trim(), current.garden)
            } catch (_: IOException) {
                return@writeGardens GardensWrite.Failed { it.copy(naming = it.naming?.copy(failed = true)) }
            }
            try {
                kept.keep(CurrentGarden(current.garden, keptFrom = keptGarden.id))
            } catch (_: IOException) {
                return@writeGardens GardensWrite.Failed { GardensSheet(failed = true) }
            }
            GardensWrite.Kept(keptGarden)
        }
    }

    /**
     * Enregistrer « X »: replaces X, the kept garden the garden under way comes from, with it, and tells it kept; only
     * once it changed since ([GardenUiState.canSaveInPlace]). The link stays as it is: X keeps its identity. Gardens
     * that cannot be written leave the sheet open, saying so.
     */
    internal fun saveInPlace(messageMillis: Long) {
        val current = state.value ?: return
        val linked = current.linked ?: return
        val sheet = current.sheet ?: return
        if (sheet.writing || !current.canSaveInPlace) return
        val library = library
        writeGardens(messageMillis) {
            try {
                GardensWrite.Kept(library.replace(linked.id, current.garden))
            } catch (_: IOException) {
                GardensWrite.Failed { it.copy(failed = true) }
            }
        }
    }

    /**
     * Writes a change of the kept gardens by [write], the sheet greyed meanwhile, in the scope of the application under
     * [writes], after the changes of the garden before it: it ends even when the screen is left. Then tells what it
     * gave: a garden kept ([toldKept]), or a sheet left open as it failed. Any failure other than an [IOException] is a
     * bug, and goes through to [appScope].
     */
    private fun writeGardens(
        messageMillis: Long,
        write: suspend () -> GardensWrite,
    ) {
        state.update { current ->
            current?.sheet?.let { current.copy(sheet = it.copy(writing = true, failed = false)) }
        }
        val writes = writes
        appScope.launch {
            when (val written = writes.withLock { write() }) {
                is GardensWrite.Kept -> toldKept(written.garden, messageMillis)

                is GardensWrite.Failed -> state.update { current ->
                    current?.sheet?.let { current.copy(sheet = written.sheet(it).copy(writing = false)) } ?: current
                }
            }
        }
    }

    /**
     * The garden under way comes from [keptGarden], just kept and written so: the sheet closes and « « X » gardé. »
     * shows for [messageMillis], in place of any offer of « Annuler ».
     */
    private fun toldKept(
        keptGarden: KeptGarden,
        messageMillis: Long,
    ) {
        undoTime?.cancel()
        state.update { it?.copy(keptFrom = keptGarden.id, sheet = null, keptName = keptGarden.name, undo = null) }
        keptMessage?.cancel()
        keptMessage = viewModelScope.launch {
            delay(messageMillis)
            state.update { it?.copy(keptName = null) }
        }
    }

    /** Shows [gardens] as what is known of « Mes jardins », now or once the garden kept is read. */
    private fun showMyGardens(gardens: MyGardens) {
        myGardens = gardens
        state.update { it?.copy(myGardens = gardens) }
    }

    /**
     * Adopts [garden] in place of the one under way: shown at once, and kept. The single way in of every change of the
     * garden (poser, reprendre, Tout effacer, Au hasard, Annuler, the mood: P10.10), shown as the [change] the screen
     * animates, if any, a move of its own (P10.15). While the garden is listened to,
     * the music goes on with its score at the next bar line, at the same rank, without a cut. A garden that cannot be
     * written (an [IOException], a full disk or a file of a later version) stays on screen, the last one kept stays on
     * the device; any other failure is a bug, and goes through to [appScope].
     */
    internal fun adopt(
        garden: Garden,
        change: GardenChange? = null,
    ) {
        state.update {
            it?.copy(garden = garden, undo = null, change = change, move = it.move + 1)
                ?: GardenUiState(garden, myGardens = myGardens)
        }
        undoTime?.cancel()
        listening?.let { playing ->
            music.switchTo(garden.toScore())
            playing.nextTempo = garden.mood.tempo
        }
        // Copies: the write, in the scope of the application, does not hold this view model once cleared.
        val kept = kept
        val writes = writes
        appScope.launch {
            writes.withLock {
                try {
                    kept.keep(garden)
                } catch (_: IOException) {
                    // Nothing kept: the garden stays on screen, the last one kept stays on the device.
                }
            }
        }
    }

    /**
     * A touch of the grid on [position]: on a preview that fits, places it; on a placed piece outside the preview, takes
     * it back as the preview, where it lay, in its type and orientation; elsewhere, a refused preview included, shows
     * the chosen piece there, its pivot on that cell: a touch never takes back the note a refused preview covers.
     */
    internal fun tap(position: Position): Unit = hearingThePreview {
        val current = state.value ?: return
        val onPreview = current.preview?.cells?.contains(position) == true
        val placed = current.garden.pieceAt(position)
        when {
            onPreview && current.refusal == null -> place()

            placed != null && !onPreview -> {
                state.value =
                    current.copy(chosen = placed.type, orientation = placed.orientation, aim = placed.pivot)
                adopt(current.garden.takeBack(placed))
            }

            else -> state.value = current.copy(aim = position)
        }
    }

    /** Chooses the piece of [type] in the tray, in its first orientation: the preview, if any, becomes it. */
    internal fun choose(type: PieceType): Unit = hearingThePreview {
        state.update { it?.copy(chosen = type, orientation = ClassicRotationSystem.spawnOrientation(type)) }
    }

    /** Turns the chosen piece a quarter turn by [rotation], round the pivot of the preview. */
    internal fun turn(rotation: Rotation): Unit = hearingThePreview {
        state.update { it?.copy(orientation = ClassicRotationSystem.rotate(it.chosen, it.orientation, rotation)) }
    }

    /** Chooses [mood]: the pieces stay where they are, heard in that mood; the mood already chosen changes nothing. */
    internal fun chooseMood(mood: GardenMood) {
        val current = state.value ?: return
        if (current.garden.mood == mood) return
        adopt(current.garden.withMood(mood), GardenChange.MoodChosen)
    }

    /** Tout effacer: nothing placed, in the same mood; « Annuler » gives the garden from before back. */
    internal fun clearAll(undoMillis: Long = UNDO_MILLIS) {
        val current = state.value ?: return
        adopt(current.garden.clear())
        offerUndo(current.garden, undoMillis)
    }

    /**
     * Au hasard: the garden the composer gives in the same mood, from a seed drawn for this roll, in place of the one
     * under way; « Annuler » gives it back, unless nothing was placed: nothing was cleared.
     */
    internal fun roll(undoMillis: Long = UNDO_MILLIS) {
        val current = state.value ?: return
        adopt(GardenComposer.compose(current.garden.mood, random.nextLong()), GardenChange.Rolled)
        if (current.garden.pieces.isNotEmpty()) offerUndo(current.garden, undoMillis)
    }

    /** Offers « Annuler » for [millis], to give [before] back. */
    private fun offerUndo(
        before: Garden,
        millis: Long,
    ) {
        state.update { it?.copy(undo = before) }
        undoTime?.cancel()
        undoTime = viewModelScope.launch {
            delay(millis)
            state.update { it?.copy(undo = null) }
        }
    }

    /** Annuler: the garden from before Tout effacer or Au hasard comes back. */
    internal fun undo() {
        adopt(state.value?.undo ?: return)
    }

    /** Retirer: the preview is gone, a piece taken back with it. */
    internal fun remove() {
        state.update { it?.copy(aim = null) }
    }

    /** Poser: places the preview, if there is one; it is gone once placed. */
    internal fun place() {
        val current = state.value ?: return
        val preview = current.preview ?: return
        val placement = current.garden.place(preview)
        if (placement is GardenPlacement.Placed) {
            state.value = current.copy(aim = null)
            adopt(placement.garden, GardenChange.Placed(preview))
        }
    }

    /**
     * Does [action], then, if it shows another preview (presented, moved, turned, another piece chosen, a piece taken
     * back), lets it be heard.
     */
    private inline fun hearingThePreview(action: () -> Unit) {
        val before = state.value?.preview
        action()
        val after = state.value ?: return
        val preview = after.preview
        if (preview != null && preview != before) hear(preview, after.garden.mood)
    }

    /**
     * Plays the notes of [preview] in [mood] once, as a rising arpeggio, when the settings want the sound effects; the
     * notes of the preview before, not yet heard, are cut. A cue still waiting dies with the screen left.
     */
    private fun hear(
        preview: Piece,
        mood: GardenMood,
    ) {
        if (!soundEffectsOn) return
        arpeggio?.cancel()
        val cues = previewCuesOf(preview, mood)
        arpeggio = viewModelScope.launch { soundEffects.playCues(cues) }
    }

    /**
     * Écouter: plays the score of the garden in a loop, from its first column, in place of any music playing; Arrêter,
     * while it plays, stops it. Nothing until the garden kept is read.
     */
    internal fun listen() {
        if (listening != null) return stopListening()
        val current = state.value ?: return
        val loop =
            LoopListening(music.play(current.garden.toScore()), viewModelScope, onStoppedByItself = ::stopListening)
        listening = Listening(loop, current.garden.mood.tempo)
        state.update { it?.copy(playedColumn = 0) }
        // Watched once the garden is shown as listened to: a score refused at once stops the listening at once.
        loop.watch()
    }

    /**
     * A frame was drawn at [frameNanos], while the garden is listened to: the column played moves on by the ticks due
     * since the frame before (ADR 0012), at the tempo of the score heard. A new mood is heard from the next bar line, as
     * [MusicPlayer.switchTo] plays it: the column moves on at its tempo from there.
     */
    internal fun onFrame(frameNanos: Long) {
        val playing = listening ?: return
        val bar = playing.playHead.head.eighth / EIGHTHS_PER_BAR
        val eighth = playing.playHead.advance(frameNanos, playing.tempo).eighth
        if (eighth / EIGHTHS_PER_BAR != bar) playing.tempo = playing.nextTempo
        state.update { it?.copy(playedColumn = eighth) }
    }

    /**
     * The garden is no longer visible (the application in the background): its score stops
     * ([ch.magori.blox.core.designsystem.lifecycle.ScreenHiddenEffect]).
     */
    internal fun onHidden() = stopListening()

    /** The garden leaves the stack: its score stops with it. */
    override fun onCleared() = stopListening()

    /**
     * Stops the score of the garden, if it is listened to, and only it: a music started since keeps playing. Also called
     * once it stopped by itself: the screen shows « Écouter » once more.
     */
    private fun stopListening() {
        val playing = listening ?: return
        playing.loop.stop()
        listening = null
        state.update { it?.copy(playedColumn = null) }
    }

    /**
     * Builds the [GardenViewModel] of the screen on the garden [kept], for `app` (ADR 0007, 0013). [appScope] lives as
     * long as the application, on `Dispatchers.Main.immediate`: each write is asked for there at once, in the order of
     * the changes, and one at a time, across the visits to the garden. The score of the garden plays on the [music]
     * player of the application, the preview on its [soundEffects], as its [preferences] want.
     */
    class Factory(
        private val kept: GardenRepository,
        private val library: GardenLibrary,
        private val appScope: CoroutineScope,
        private val music: MusicPlayer,
        private val soundEffects: SoundEffectPlayer,
        private val preferences: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        /** The writes of the garden, one at a time, for every view model of the garden. */
        private val writes = Mutex()

        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return GardenViewModel(kept, library, appScope, writes, music, soundEffects, preferences) as T
        }
    }

    /**
     * The garden listened to: its [loop], which stops it and never a music started since, its [playHead], moved on by
     * the frames of the screen, the [tempo] of the score heard and the one of the last score switched to, heard from the
     * next bar line, [nextTempo].
     */
    private class Listening(val loop: LoopListening, var tempo: Int) {
        val playHead = FramePlayHead(eighthsPerLoop = Garden.COLUMNS)
        var nextTempo = tempo
    }
}

/** The eighths of a bar of the score of the garden: its 16 columns make 2 bars of 4 beats. */
private const val EIGHTHS_PER_BAR = 8

/** How long « Annuler » is offered after Tout effacer or Au hasard (docs/jardin-musical.md, Écran). */
internal const val UNDO_MILLIS: Long = 4_000L

/** What a change of the kept gardens gave: a garden [Kept], or a sheet left open as it [Failed]. */
private sealed interface GardensWrite {
    /** The [garden] kept, the garden under way coming from it on the device. */
    class Kept(val garden: KeptGarden) : GardensWrite

    /** Nothing written, or not all: the sheet as it shows it, from the [sheet] open. */
    class Failed(val sheet: (GardensSheet) -> GardensSheet) : GardensWrite
}
