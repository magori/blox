package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import java.time.LocalDate

/**
 * Results of the daily challenge kept in memory, [initial] at first (null: no try), that keep the contract of
 * [DailyChallengeRepository] (`DailyChallengeRepositoryContract`): they follow the same rules (afterStart,
 * afterEnd and recordOf, internal to core:data), each change atomic as in a DataStore.
 *
 * For the tests of a view model: a start or an end fails with [failure] when there is one, the results unchanged,
 * as a file that cannot be written.
 */
class FakeDailyChallengeRepository(initial: DailyResults? = null) : DailyChallengeRepository {
    var failure: Exception? = null

    /** The results kept; the test may change them, as another game would. */
    val state = MutableStateFlow(initial)

    override val results: Flow<DailyResults?> get() = state

    override suspend fun startTry(date: LocalDate) {
        write { kept -> kept.afterStart(date) }
    }

    override suspend fun endTry(
        date: LocalDate,
        points: Int,
    ): RecordResult {
        require(points >= 0) { "Points cannot be negative: $points" }
        return write { kept -> kept.afterEnd(date, points) }.recordOf(date, points)
    }

    /** Applies [change] to the results kept, in one atomic update, and gives those from before it. */
    private fun write(change: (DailyResults?) -> DailyResults): DailyResults? {
        failure?.let { throw it }
        return state.getAndUpdate(change)
    }
}
