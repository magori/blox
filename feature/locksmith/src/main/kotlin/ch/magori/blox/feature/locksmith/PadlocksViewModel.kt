package ch.magori.blox.feature.locksmith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.PadlockProgressRepository
import ch.magori.blox.core.locksmith.PadlockResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.reflect.KClass

/**
 * The screen « Serrures » (P7.35): the best opening of each open padlock, as the [progress] of the Serrurier keeps it,
 * followed while the screen is shown. A progress that cannot be read gives an empty one (the contract of the
 * repository): the screen then shows padlock 1 to open.
 *
 * Starting over from the beginning (P7.37) clears the [progress] in [appScope], the scope of the application, which
 * outlives the screen, and hands the clear over to padlock 1 by [cleared].
 */
class PadlocksViewModel internal constructor(
    private val progress: PadlockProgressRepository,
    private val cleared: ClearedProgressRelay,
    private val appScope: CoroutineScope,
) : ViewModel() {
    /** Where starting over from the beginning stands. */
    private val clearing = MutableStateFlow(ProgressClearing.NONE)

    /** The best openings the screen shows, kept as they were once the clear is under way. */
    private var shown: List<PadlockResult> = emptyList()

    init {
        // A clear handed over that no padlock took (the screen left before the navigation) is not told later, by a
        // padlock shown for another reason.
        cleared.forget()
    }

    /** What the screen shows; null until the progress is read, so that no empty screen flashes. */
    internal val uiState: StateFlow<PadlocksUiState?> =
        combine(progress.progress, clearing) { kept, clearing ->
            // Once the clear is under way, the screen keeps the padlocks it showed when it was asked: the window tells
            // how many it forgets, never 0, and the grid under it does not empty before the navigation.
            if (!clearing.isUnderWay) shown = kept.openings.map { it.toResult() }
            PadlocksUiState(shown, clearing)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    /** Asks whether to start over from the beginning: the confirmation opens. */
    internal fun onStartOver() = answer(ProgressClearing.ASKED)

    /** Keeps the progress: the confirmation closes, nothing changed. */
    internal fun onKeepProgress() = answer(ProgressClearing.NONE)

    /** Moves the confirmation [to], unless it waits: while the clear is written, and once done, until the screen goes. */
    private fun answer(to: ProgressClearing) {
        if (!clearing.value.isWaiting) clearing.value = to
    }

    /**
     * Clears the progress, as the player confirmed (asked, or after a failure; any other confirmation, a second tap, is
     * ignored), and hands the clear over to the padlock shown next, in the scope of the application: leaving the screen
     * at once does not lose it. While it is written, the confirmation stays open and answers nothing. A progress that
     * cannot be written (a full disk) is kept, and the confirmation stays open to say so, for the player to try again or
     * keep it.
     */
    internal fun onClearProgress() {
        if (clearing.value != ProgressClearing.ASKED && clearing.value != ProgressClearing.FAILED) return
        clearing.value = ProgressClearing.CLEARING
        appScope.launch {
            clearing.value = try {
                progress.clear()
                cleared.handOver()
                ProgressClearing.CLEARED
            } catch (_: IOException) {
                ProgressClearing.FAILED
            }
        }
    }

    /**
     * Builds the view model of the screen « Serrures » (ADR 0013), on the [progress] the application owns (ADR 0007), the
     * relay of a progress [cleared] to padlock 1, and [appScope], where the clear is written.
     */
    class Factory(
        private val progress: PadlockProgressRepository,
        private val cleared: ClearedProgressRelay,
        private val appScope: CoroutineScope,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return PadlocksViewModel(progress, cleared, appScope) as T
        }
    }

    private companion object {
        /** How long the progress is still followed once the screen is hidden: a rotation does not read it again. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
