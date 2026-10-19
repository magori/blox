package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The contract of the records, on a DataStore file, and what is proper to a file: it is kept, and its errors. */
class DataStoreHighScoreRepositoryTest : HighScoreRepositoryContract() {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the record survives a new repository on the same file`() = runTest {
        val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        repositoryIn(firstScope).submit(5000)
        firstScope.coroutineContext.job.cancelAndJoin()

        val reopened = repositoryIn(backgroundScope)

        assertThat(reopened.highScore.first()).isEqualTo(5000)
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

    @Test
    fun `a failure other than an IOException while the records are read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val repository = DataStoreHighScoreRepository(FailingDataStore(IllegalStateException("a bug")))

        // When
        val failure = runCatching { repository.highScore.first() }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
    }

    override fun emptyRepository(scope: CoroutineScope): HighScoreRepository = repositoryIn(scope)

    /** The records file of the test, in [directory]. */
    private val recordsFile: File get() = File(directory, RECORDS_FILE)

    /** A repository on the records file of [directory], its DataStore living in [scope], as in production. */
    private fun repositoryIn(scope: CoroutineScope): HighScoreRepository =
        DataStoreHighScoreRepository(preferencesDataStore(scope) { recordsFile.absolutePath.toPath() })
}
