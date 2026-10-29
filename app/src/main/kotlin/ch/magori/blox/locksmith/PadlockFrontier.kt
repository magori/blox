package ch.magori.blox.locksmith

import ch.magori.blox.core.data.PadlockProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The frontier of the Serrurier, the first padlock not open (docs/regles-serrurier.md, Progression): the one its
 * cartridge opens, and names on its line « Serrure N ». `feature:cartridges` knows nothing of the progress: `app` hands
 * it [current], and awaits the frontier ([await]) when the cartridge is tapped.
 *
 * The [progress] is read once and kept warm in [scope], for the life of the process: the cartridges coming back know
 * the frontier from their first frame, and a tap finds it at once, up to date with the last padlock opened. A progress
 * that cannot be read is an empty one (`PadlockProgressRepository`): the frontier is then padlock 1.
 */
class PadlockFrontier(progress: Flow<PadlockProgress>, scope: CoroutineScope) {
    /**
     * The frontier, at each reading of the progress; null before the first, so that no line shows that may have to
     * change.
     */
    val current: StateFlow<Int?> = progress.map { it.frontier }.stateIn(scope, SharingStarted.Eagerly, null)

    /** The frontier, once the progress is read. */
    suspend fun await(): Int = current.filterNotNull().first()
}
