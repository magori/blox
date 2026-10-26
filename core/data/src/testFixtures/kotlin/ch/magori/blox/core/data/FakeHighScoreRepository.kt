package ch.magori.blox.core.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * A record kept in memory, [initial] at first, that keeps the contract of [HighScoreRepository]
 * (`HighScoreRepositoryContract`): a negative score is refused, a higher one becomes the record.
 *
 * For the tests of a view model: [submitted] holds every score accepted, [saved] every score whose submit
 * went to its end, [clears] counts the clears asked for. A submit or a clear waits for [gate] when there is one,
 * then fails with [failure] when there is one, the record unchanged, as a file that cannot be written.
 */
class FakeHighScoreRepository(initial: Int = 0) : HighScoreRepository {
    val submitted = mutableListOf<Int>()
    val saved = mutableListOf<Int>()
    var gate: CompletableDeferred<Unit>? = null
    var failure: Exception? = null

    /** How many clears were asked for, those still waiting for [gate] included. */
    var clears = 0
        private set

    /** The record kept; the test may change it, as another game would. */
    val record = MutableStateFlow(initial)

    /** What [highScore] gives: [record], unless the test sets another flow (reads that go wrong) before it is read. */
    var highScoreFlow: Flow<Int> = record

    override val highScore: Flow<Int> get() = highScoreFlow

    override suspend fun submit(score: Int): RecordResult {
        require(score >= 0) { "A score cannot be negative: $score" }
        submitted += score
        gate?.await()
        failure?.let { throw it }
        val previous = record.getAndUpdate { maxOf(it, score) }
        saved += score
        return RecordResult(previous = previous, isNewRecord = score > previous)
    }

    override suspend fun clear() {
        clears++
        gate?.await()
        failure?.let { throw it }
        record.value = 0
    }
}
