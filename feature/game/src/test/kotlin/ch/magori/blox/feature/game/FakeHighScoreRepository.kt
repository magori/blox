package ch.magori.blox.feature.game

import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.RecordResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A record kept in memory, which answers every submit with [result]. [submitted] holds every score
 * submitted, [saved] every score whose submit went to its end. A submit waits for [gate] when there is
 * one, then fails with [failure] when there is one.
 */
internal class FakeHighScoreRepository(private val result: RecordResult) : HighScoreRepository {
    val submitted = mutableListOf<Int>()
    val saved = mutableListOf<Int>()
    var gate: CompletableDeferred<Unit>? = null
    var failure: Exception? = null

    override val highScore: Flow<Int> = MutableStateFlow(result.previous)

    override suspend fun submit(score: Int): RecordResult {
        submitted += score
        gate?.await()
        failure?.let { throw it }
        saved += score
        return result
    }
}
