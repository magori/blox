package ch.magori.blox.feature.game.results

import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.FakeHighScoreRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** What the results of a game tell of its end: whether it beat the best, whether they keep it, and the best before. */
class GameResultsTest {
    @Test
    fun `a classic game over tells the record before it, beaten or not`() = runTest {
        // Given a record of 100 points
        val results = ClassicGameResults(FakeHighScoreRepository(initial = 100))

        // When a game ends above it, then another below the new record
        val beaten = results.end(150)
        val notBeaten = results.end(120)

        // Then each tells the record it had to beat
        assertThat(beaten).isEqualTo(GameEnd(newBest = true, kept = true, previousBest = 100))
        assertThat(notBeaten).isEqualTo(GameEnd(newBest = false, kept = true, previousBest = 150))
    }

    @Test
    fun `a daily game over tells the best of its day before it, 0 at the first try`() = runTest {
        // Given no result yet
        val results = DailyGameResults(FakeDailyChallengeRepository(), OCTOBER_26, Mutex())

        // When a first try ends, then a second one above it
        val first = results.end(100)
        val second = results.end(150)

        // Then the first had no best to beat, the second the one of the first
        assertThat(first).isEqualTo(GameEnd(newBest = true, kept = true, previousBest = 0))
        assertThat(second).isEqualTo(GameEnd(newBest = true, kept = true, previousBest = 100))
    }

    @Test
    fun `a daily try ignored, its date already passed, tells no best before it`() = runTest {
        // Given results already on the 27th, whose best is 300
        val daily = FakeDailyChallengeRepository(
            DailyResults(OCTOBER_26.plusDays(1), tries = 1, firstTry = 300, best = 300, firstTryOver = true),
        )
        val results = DailyGameResults(daily, OCTOBER_26, Mutex())

        // When a try of the 26th ends
        val end = results.end(100)

        // Then it is not kept, and the best of the 27th is not the one it had to beat
        assertThat(end).isEqualTo(GameEnd(newBest = false, kept = false, previousBest = 0))
    }

    private companion object {
        val OCTOBER_26: LocalDate = LocalDate.of(2026, 10, 26)
    }
}
