package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * The musical garden (V40, docs/jardin-musical.md, ADR 0027): the garden under way, which opens empty and calm. The
 * garden kept (P10.11), the grid and the tray (P10.10) and the sound (P10.12) come later.
 */
class GardenViewModel : ViewModel() {
    private val state = MutableStateFlow(GardenUiState(Garden.EMPTY))

    /** The screen. */
    internal val uiState: StateFlow<GardenUiState> = state.asStateFlow()

    /** Builds the [GardenViewModel] of the screen, for `app` (ADR 0007, 0013). */
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return GardenViewModel() as T
        }
    }
}
