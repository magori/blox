package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.PadlockProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.reflect.KClass

/**
 * The screen « Serrures » (P7.35): the best opening of each open padlock, as the [progress] of the Serrurier keeps it,
 * followed while the screen is shown. A progress that cannot be read gives an empty one (the contract of the
 * repository): the screen then shows padlock 1 to open.
 */
class PadlocksViewModel internal constructor(progress: PadlockProgressRepository) : ViewModel() {
    /** What the screen shows; null until the progress is read, so that no empty screen flashes. */
    internal val uiState: StateFlow<PadlocksUiState?> =
        progress.progress
            .map { kept -> PadlocksUiState(kept.openings.map { it.toResult() }) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    /** Builds the view model of the screen « Serrures » (ADR 0013), on the [progress] the application owns (ADR 0007). */
    class Factory(private val progress: PadlockProgressRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return PadlocksViewModel(progress) as T
        }
    }

    private companion object {
        /** How long the progress is still followed once the screen is hidden: a rotation does not read it again. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
