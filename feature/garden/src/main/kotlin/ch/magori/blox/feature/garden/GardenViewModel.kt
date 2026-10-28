package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
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
) : ViewModel() {
    private val state = MutableStateFlow<GardenUiState?>(null)

    /** The screen; null while the garden kept is being read (P10.10 decides what the screen shows meanwhile). */
    internal val uiState: StateFlow<GardenUiState?> = state.asStateFlow()

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
        state.value = GardenUiState(garden)
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
