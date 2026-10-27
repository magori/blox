package ch.magori.blox.core.data

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.time.LocalDate

/**
 * The contract of [DailyChallengeRepository], what every implementation does, the real one and the fake shared by
 * the tests of the features alike. A subclass gives an empty repository; what is proper to an implementation
 * (a file, its errors) is tested in the subclass.
 */
abstract class DailyChallengeRepositoryContract {
    /** A repository without any try, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): DailyChallengeRepository

    /**
     * A repository holding a first try of October 26 under way, as a full disk: the results are read as kept, but
     * every write (a start, an end) fails with an IOException.
     */
    protected abstract suspend fun unwritableRepository(scope: CoroutineScope): DailyChallengeRepository

    @Test
    fun `a start that cannot be written fails with an IOException and keeps the results`() = runTest {
        // Given
        val repository = unwritableRepository(backgroundScope)

        // When
        val failure = runCatching { repository.startTry(OCTOBER_26) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1))
    }

    @Test
    fun `an end that cannot be written fails with an IOException and keeps the results`() = runTest {
        // Given
        val repository = unwritableRepository(backgroundScope)

        // When
        val failure = runCatching { repository.endTry(OCTOBER_26, points = 9800) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1))
    }

    @Test
    fun `without a try there is no result of the day`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When / Then
        assertThat(repository.results.first()).isNull()
    }

    @Test
    fun `a try is counted as soon as it starts`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.startTry(OCTOBER_26)

        // Then
        val results = repository.results.first()
        assertThat(results?.date).isEqualTo(OCTOBER_26)
        assertThat(results?.tries).isEqualTo(1)
    }

    @Test
    fun `the first try is worth 0 until it ends`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.startTry(OCTOBER_26)

        // Then
        val results = repository.results.first()
        assertThat(results?.firstTry).isEqualTo(0)
        assertThat(results?.best).isEqualTo(0)
    }

    @Test
    fun `the first try is the first and the best`() = runTest {
        // Given a first try under way
        val repository = emptyRepository(backgroundScope)
        repository.startTry(OCTOBER_26)

        // When it ends
        repository.endTry(OCTOBER_26, points = 9800)

        // Then
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_26, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true))
    }

    @Test
    fun `a better try replaces the best, never the first`() = runTest {
        // Given a first try of 9800
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_26, points = 9800)

        // When a better try ends, then a worse one
        repository.tryOf(OCTOBER_26, points = 18200)
        repository.tryOf(OCTOBER_26, points = 4000)

        // Then
        val results = repository.results.first()
        assertThat(results?.firstTry).isEqualTo(9800)
        assertThat(results?.best).isEqualTo(18200)
    }

    @Test
    fun `every try is counted`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When two tries end, and a third one is under way
        repository.tryOf(OCTOBER_26, points = 9800)
        repository.tryOf(OCTOBER_26, points = 0)
        repository.startTry(OCTOBER_26)

        // Then
        assertThat(repository.results.first()?.tries).isEqualTo(3)
    }

    @Test
    fun `a try of a newer date starts that date over`() = runTest {
        // Given two tries on October 26
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_26, points = 9800)
        repository.tryOf(OCTOBER_26, points = 18200)

        // When a try of October 27 starts
        repository.startTry(OCTOBER_27)

        // Then
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_27, tries = 1, firstTry = 0, best = 0))
    }

    @Test
    fun `a try of an older date is ignored`() = runTest {
        // Given a try of October 27
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_27, points = 9800)

        // When a try of October 26 starts and ends, a game of yesterday replayed after midnight
        repository.tryOf(OCTOBER_26, points = 18200)

        // Then
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_27, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true))
    }

    @ParameterizedTest(name = "results of an older date: {0}")
    @ValueSource(booleans = [false, true])
    fun `a try that ends on a newer date without its start counts as a whole first try`(withOlderResults: Boolean) =
        runTest {
            // Given no results, or those of October 26
            val repository = emptyRepository(backgroundScope)
            if (withOlderResults) repository.tryOf(OCTOBER_26, points = 18200)

            // When a try of October 27 ends whose start was not kept
            repository.endTry(OCTOBER_27, points = 9800)

            // Then
            assertThat(repository.results.first())
                .isEqualTo(
                    DailyResults(date = OCTOBER_27, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true),
                )
        }

    @Test
    fun `a first try that never ends stays worth 0`() = runTest {
        // Given a first try that never ended, its process killed and its game not restored
        val repository = emptyRepository(backgroundScope)
        repository.startTry(OCTOBER_26)

        // When a second try ends
        repository.tryOf(OCTOBER_26, points = 9800)

        // Then
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_26, tries = 2, firstTry = 0, best = 9800, firstTryOver = true))
    }

    @Test
    fun `the first try ends only once`() = runTest {
        // Given a first try that ended with 9800
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_26, points = 9800)

        // When it ends again, its game restored after the process died and played to its end once more
        repository.endTry(OCTOBER_26, points = 3000)

        // Then
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_26, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true))
    }

    @Test
    fun `negative points are refused and leave the results`() = runTest {
        // Given a first try under way
        val repository = emptyRepository(backgroundScope)
        repository.startTry(OCTOBER_26)

        // When
        val failure = runCatching { repository.endTry(OCTOBER_26, points = -1) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1))
    }

    @Test
    fun `the results flow emits each change, and nothing for a try that is ignored`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.results.test {
            assertThat(awaitItem()).isNull()

            // When a try of October 27 starts
            repository.startTry(OCTOBER_27)

            // Then
            assertThat(awaitItem()).isEqualTo(DailyResults(date = OCTOBER_27, tries = 1))

            // When it ends
            repository.endTry(OCTOBER_27, points = 9800)

            // Then
            assertThat(awaitItem())
                .isEqualTo(
                    DailyResults(date = OCTOBER_27, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true),
                )

            // When a try of October 26 starts and ends, ignored
            repository.tryOf(OCTOBER_26, points = 9800)
            testScheduler.advanceUntilIdle()

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `the end of a first try beats the best of the day`() = runTest {
        // Given a first try under way
        val repository = emptyRepository(backgroundScope)
        repository.startTry(OCTOBER_26)

        // When
        val result = repository.endTry(OCTOBER_26, points = 9800)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
    }

    @ParameterizedTest(name = "best 9800, points {0}")
    @ValueSource(ints = [4000, 9800])
    fun `an end below the best does not beat it`(points: Int) = runTest {
        // Given a best of 9800, and a second try under way
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_26, points = 9800)
        repository.startTry(OCTOBER_26)

        // When
        val result = repository.endTry(OCTOBER_26, points)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 9800, isNewRecord = false))
    }

    @Test
    fun `an ignored end beats nothing`() = runTest {
        // Given a best of 9800 on October 27
        val repository = emptyRepository(backgroundScope)
        repository.tryOf(OCTOBER_27, points = 9800)

        // When a try of October 26 ends, better than the best of October 27
        val result = repository.endTry(OCTOBER_26, points = 18200)

        // Then
        assertThat(result.isNewRecord).isFalse()
    }

    @Test
    fun `concurrent starts are all counted`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When tries start at once, on several threads
        (1..CONCURRENT_STARTS).map { async(Dispatchers.Default) { repository.startTry(OCTOBER_26) } }.awaitAll()

        // Then none is lost
        assertThat(repository.results.first()?.tries).isEqualTo(CONCURRENT_STARTS)
    }

    @Test
    fun `negative points are refused even when nothing can be written`() = runTest {
        // Given
        val repository = unwritableRepository(backgroundScope)

        // When
        val failure = runCatching { repository.endTry(OCTOBER_26, points = -1) }.exceptionOrNull()

        // Then the points are refused before any write
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    /** A whole try of [date], started then ended with [points]. */
    private suspend fun DailyChallengeRepository.tryOf(
        date: LocalDate,
        points: Int,
    ) {
        startTry(date)
        endTry(date, points)
    }

    protected companion object {
        val OCTOBER_26: LocalDate = LocalDate.of(2026, 10, 26)
        val OCTOBER_27: LocalDate = OCTOBER_26.plusDays(1)

        /** Enough starts on several threads for a lost update to show; the DataStore takes about 1 s for them. */
        const val CONCURRENT_STARTS = 200
    }
}
