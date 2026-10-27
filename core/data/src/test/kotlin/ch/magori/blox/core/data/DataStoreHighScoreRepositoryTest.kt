package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
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
class DataStoreHighScoreRepositoryTest :
    HighScoreRepositoryContract(),
    DataStoreFileContract<HighScoreRepository> {
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

    override fun emptyRepository(scope: CoroutineScope): HighScoreRepository = repositoryIn(scope)

    /** The records file holding [record], read as it is, but on a disk that refuses every write. */
    override suspend fun unwritableRepository(
        scope: CoroutineScope,
        record: Int,
    ): HighScoreRepository {
        val dataStore = dataStoreIn(scope)
        DataStoreHighScoreRepository(dataStore).submit(record)
        return DataStoreHighScoreRepository(UnwritableDataStore(dataStore))
    }

    /** The records file of the test, in [directory]. */
    override val file: File get() = File(directory, RECORDS_FILE)

    override fun repositoryIn(scope: CoroutineScope): HighScoreRepository =
        DataStoreHighScoreRepository(dataStoreIn(scope))

    override fun repositoryFailingWith(failure: Exception): HighScoreRepository =
        DataStoreHighScoreRepository(FailingDataStore(failure))

    override fun readsOf(repository: HighScoreRepository): List<Flow<Any?>> = listOf(repository.highScore)

    /** A record of 0. */
    override val emptyReads: List<Any?> = listOf(0)

    /** A score of 1200, which becomes the record. */
    override suspend fun write(repository: HighScoreRepository) {
        assertThat(repository.submit(1200)).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
    }

    override val readsAfterWrite: List<Any?> = listOf(1200)

    /** The DataStore of the records file of [directory], living in [scope]; one per file. */
    private fun dataStoreIn(scope: CoroutineScope) = preferencesDataStore(scope) { file.absolutePath.toPath() }
}
