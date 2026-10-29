package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A progress of the padlocks in memory, that keeps the contract of [PadlockProgressRepository]
 * (`PadlockProgressRepositoryContract`): the rules are those of the real repository, [PadlockProgress], only the file
 * is left out. For the tests of a view model: each change then fails with [failure] when there is one, the progress
 * unchanged, as a file that cannot be written.
 */
class FakePadlockProgressRepository : PadlockProgressRepository {
    var failure: Exception? = null

    private val content = MutableStateFlow(PadlockProgress())

    override val progress: Flow<PadlockProgress> = content

    override suspend fun record(
        padlock: Int,
        opening: PadlockOpening,
    ): OpeningOutcome {
        failure?.let { throw it }
        var outcome: OpeningOutcome? = null
        content.update { progress -> progress.recording(padlock, opening).also { outcome = it.second }.first }
        return checkNotNull(outcome)
    }

    override suspend fun clear() {
        failure?.let { throw it }
        content.update { PadlockProgress() }
    }
}
