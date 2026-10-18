package ch.magori.blox.core.data

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class DataStoreHighScoreRepositoryTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `without a saved record the record is 0`() = runTest {
        val repository = repositoryIn(backgroundScope)

        assertThat(repository.highScore.first()).isEqualTo(0)
    }

    @Test
    fun `a higher score becomes the record, and submit tells it is a new record and the previous one`() = runTest {
        val repository = repositoryIn(backgroundScope)
        repository.submit(1200)

        val result = repository.submit(5000)

        assertThat(result).isEqualTo(RecordResult(previous = 1200, isNewRecord = true))
        assertThat(repository.highScore.first()).isEqualTo(5000)
    }

    @ParameterizedTest(name = "record 5000, score {0}")
    @ValueSource(ints = [4999, 5000])
    fun `a lower or equal score leaves the record and is not a new record`(score: Int) = runTest {
        val repository = repositoryIn(backgroundScope)
        repository.submit(5000)

        val result = repository.submit(score)

        assertThat(result).isEqualTo(RecordResult(previous = 5000, isNewRecord = false))
        assertThat(repository.highScore.first()).isEqualTo(5000)
    }

    @Test
    fun `a score of 0 in the first game is not a new record`() = runTest {
        val repository = repositoryIn(backgroundScope)

        val result = repository.submit(0)

        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = false))
    }

    @Test
    fun `a first positive score is a new record against 0`() = runTest {
        // Given
        val repository = repositoryIn(backgroundScope)

        // When
        val result = repository.submit(1)

        // Then
        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
    }

    @Test
    fun `a negative score is refused and leaves the record at 0`() = runTest {
        val repository = repositoryIn(backgroundScope)

        val failure = runCatching { repository.submit(-1) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.highScore.first()).isEqualTo(0)
    }

    @Test
    fun `the record survives a new repository on the same file`() = runTest {
        val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        repositoryIn(firstScope).submit(5000)
        firstScope.coroutineContext.job.cancelAndJoin()

        val reopened = repositoryIn(backgroundScope)

        assertThat(reopened.highScore.first()).isEqualTo(5000)
    }

    @Test
    fun `the record flow emits the new record after a submit`() = runTest {
        val repository = repositoryIn(backgroundScope)

        repository.highScore.test {
            assertThat(awaitItem()).isEqualTo(0)

            repository.submit(1200)

            assertThat(awaitItem()).isEqualTo(1200)
        }
    }

    @ParameterizedTest(name = "submitted in the order {0}, {1}")
    @CsvSource("3000, 5000", "5000, 3000")
    fun `two concurrent submits keep the highest score, the second one seeing the first`(
        first: Int,
        second: Int,
    ) = runTest {
        // Given
        val repository = repositoryIn(backgroundScope)

        // When
        val scores = listOf(first, second)
        val results = scores.map { score -> async { repository.submit(score) } }.awaitAll()

        // Then one submit compared with 0, the other with the score of that one
        assertThat(repository.highScore.first()).isEqualTo(5000)
        val (alone, after) = scores.zip(results).partition { (_, result) -> result.previous == 0 }
        assertThat(alone).hasSize(1)
        assertThat(after.single().second.previous).isEqualTo(alone.single().first)
    }

    @Test
    fun `an unreadable records file gives a record of 0, and the next score becomes the record`() = runTest {
        // Given a records file that is not a preferences file
        recordsFile.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.highScore.first()).isEqualTo(0)
        assertThat(repository.submit(1200)).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
        assertThat(repository.highScore.first()).isEqualTo(1200)
    }

    @Test
    fun `a records file that cannot be read gives a record of 0`() = runTest {
        // Given a directory where the records file should be: reading it fails with an IOException
        recordsFile.mkdirs()
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.highScore.first()).isEqualTo(0)
    }

    /** The records file of the test, in [directory]. */
    private val recordsFile: File get() = File(directory, RECORDS_FILE)

    /** A repository on the records file of [directory], its DataStore living in [scope], as in production. */
    private fun repositoryIn(scope: CoroutineScope): HighScoreRepository =
        DataStoreHighScoreRepository(recordsDataStore(scope) { recordsFile.absolutePath.toPath() })
}
