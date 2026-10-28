package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenComposer
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 * (the contract of [GardenRepository]). The grid and the tray (P10.10) and the sound (P10.12) come later.
 *
 * The garden is written in [appScope], which outlives the screen: a change made just before leaving is kept. The
 * writes, one at a time under [writes], keep the order of the changes, the last garden adopted being the one kept, as
 * long as [appScope] dispatches at once (`Dispatchers.Main.immediate`): each write then asks for [writes] in the order
 * of the changes. The garden kept is read under [writes] too: a visit opens on the garden of the previous one, once
 * kept.
 */
class GardenViewModel internal constructor(
    private val kept: GardenRepository,
    private val appScope: CoroutineScope,
    private val writes: Mutex,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val state = MutableStateFlow<GardenUiState?>(null)

    /** The screen; null while the garden kept is being read (P10.10 decides what the screen shows meanwhile). */
    internal val uiState: StateFlow<GardenUiState?> = state.asStateFlow()

    /** The time « Annuler » is still offered, counted from the last Tout effacer or Au hasard. */
    private var undoTime: Job? = null

    init {
        // Read once: only this screen writes the garden kept. A garden adopted meanwhile is not replaced by it. Any
        // failure other than an unreadable garden (which the repository absorbs) is a bug, and is not swallowed here.
        viewModelScope.launch {
            val read = writes.withLock { kept.garden.first() }
            state.update { it ?: GardenUiState(read) }
        }
    }

    /**
     * Adopts [garden] in place of the one under way: shown at once, and kept. The single way in of every change of the
     * garden (poser, reprendre, Tout effacer, Au hasard, the mood: P10.10). A garden that cannot be written (an
     * [IOException], a full disk or a file of a later version) stays on screen, the last one kept stays on the device;
     * any other failure is a bug, and goes through to [appScope].
     */
    internal fun adopt(garden: Garden) {
        state.update { it?.copy(garden = garden, undo = null) ?: GardenUiState(garden) }
        undoTime?.cancel()
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
    internal fun tap(position: Position) {
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
    internal fun choose(type: PieceType) {
        state.update { it?.copy(chosen = type, orientation = ClassicRotationSystem.spawnOrientation(type)) }
    }

    /** Turns the chosen piece a quarter turn by [rotation], round the pivot of the preview. */
    internal fun turn(rotation: Rotation) {
        state.update { it?.copy(orientation = ClassicRotationSystem.rotate(it.chosen, it.orientation, rotation)) }
    }

    /** Chooses [mood]: the pieces stay where they are, heard in that mood. */
    internal fun chooseMood(mood: GardenMood) {
        val current = state.value ?: return
        adopt(current.garden.withMood(mood))
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
        adopt(GardenComposer.compose(current.garden.mood, random.nextLong()))
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
            adopt(placement.garden)
        }
    }

    /**
     * Builds the [GardenViewModel] of the screen on the garden [kept], for `app` (ADR 0007, 0013). [appScope] lives as
     * long as the application, on `Dispatchers.Main.immediate`: each write is asked for there at once, in the order of
     * the changes, and one at a time, across the visits to the garden.
     */
    class Factory(private val kept: GardenRepository, private val appScope: CoroutineScope) :
        ViewModelProvider.Factory {
        /** The writes of the garden, one at a time, for every view model of the garden. */
        private val writes = Mutex()

        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return GardenViewModel(kept, appScope, writes) as T
        }
    }
}

/** How long « Annuler » is offered after Tout effacer or Au hasard (docs/jardin-musical.md, Écran). */
internal const val UNDO_MILLIS: Long = 4_000L
