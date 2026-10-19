package ch.magori.blox.core.data

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * The contract of [HighScoreRepository], what every implementation does, the real one and the fake shared by
 * the tests of the features alike. A subclass gives an empty repository; what is proper to an implementation
 * (a file, its errors) is tested in the subclass.
 */
abstract class HighScoreRepositoryContract {
    /** A repository without any record, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): HighScoreRepository

    @Test
    fun `without a saved record the record is 0`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When / Then
        assertThat(repository.highScore.first()).isEqualTo(0)
    }

    @Test
    fun `a higher score becomes the record, and submit tells it is a new record and the previous one`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)
        repository.submit(1200)

        // When
        val result = repository.submit(5000)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 1200, isNewRecord = true))
        assertThat(repository.highScore.first()).isEqualTo(5000)
    }

    @ParameterizedTest(name = "record 5000, score {0}")
    @ValueSource(ints = [4999, 5000])
    fun `a lower or equal score leaves the record and is not a new record`(score: Int) = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)
        repository.submit(5000)

        // When
        val result = repository.submit(score)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 5000, isNewRecord = false))
        assertThat(repository.highScore.first()).isEqualTo(5000)
    }

    @Test
    fun `a score of 0 in the first game is not a new record`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val result = repository.submit(0)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = false))
    }

    @Test
    fun `a first positive score is a new record against 0`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val result = repository.submit(1)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
    }

    @Test
    fun `a negative score is refused and leaves the record at 0`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val failure = runCatching { repository.submit(-1) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.highScore.first()).isEqualTo(0)
    }

    @Test
    fun `the record flow emits the new record after a submit`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.highScore.test {
            assertThat(awaitItem()).isEqualTo(0)

            // When
            repository.submit(1200)

            // Then
            assertThat(awaitItem()).isEqualTo(1200)
        }
    }

    @Test
    fun `a lower score emits nothing on the record flow`() = runTest {
        // Given a record of 5000
        val repository = emptyRepository(backgroundScope)
        repository.submit(5000)

        repository.highScore.test {
            assertThat(awaitItem()).isEqualTo(5000)

            // When a lower score is submitted
            repository.submit(1200)
            testScheduler.advanceUntilIdle()

            // Then the record did not change: nothing is emitted
            expectNoEvents()
        }
    }

    @ParameterizedTest(name = "submitted in the order {0}, {1}")
    @CsvSource("3000, 5000", "5000, 3000")
    fun `two concurrent submits keep the highest score, the second one seeing the first`(
        first: Int,
        second: Int,
    ) = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val scores = listOf(first, second)
        val results = scores.map { score -> async { repository.submit(score) } }.awaitAll()

        // Then one submit compared with 0, the other with the score of that one
        assertThat(repository.highScore.first()).isEqualTo(5000)
        val (alone, after) = scores.zip(results).partition { (_, result) -> result.previous == 0 }
        assertThat(alone).hasSize(1)
        assertThat(after.single().second.previous).isEqualTo(alone.single().first)
    }
}
