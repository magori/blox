package ch.magori.blox.feature.game.results

import ch.magori.blox.core.data.DailyChallengeRepository
import ch.magori.blox.core.data.RecordResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * The daily game of [date] (docs/graine-du-jour.md): its tries go to the results of the day, under the date of its
 * route, never to the records of the Classic. A try is counted as soon as it starts, and ends with its points, over or
 * left.
 *
 * Its writes take [writes], shared by every daily game: one at a time, in the order they are asked for (a fair lock),
 * so that the end of a try left is written before the start of the next one, which would otherwise freeze the first
 * try at 0. Asked for on the main thread, at once (`Dispatchers.Main.immediate`), they are asked for in the order of
 * the games.
 */
internal class DailyGameResults(
    private val daily: DailyChallengeRepository,
    private val date: LocalDate,
    private val writes: Mutex,
) : GameResults {
    /** The best of [date]: 0 while the results are those of another date, or before any try. */
    override val best: Flow<Int> get() = daily.results.map { results -> results?.takeIf { it.date == date }?.best ?: 0 }

    override suspend fun start() = writes.withLock { daily.startTry(date) }

    override suspend fun end(points: Int): RecordResult = writes.withLock { daily.endTry(date, points) }

    override suspend fun leave(points: Int) {
        end(points)
    }
}
