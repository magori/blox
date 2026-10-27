package ch.magori.blox.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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

/** The contract of the results of the day, on a DataStore file, and what is proper to a file: it is kept, and its errors. */
class DataStoreDailyChallengeRepositoryTest : DailyChallengeRepositoryContract() {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the results survive a new repository on the same file`() = runTest {
        // Given two tries of October 26, the second one under way
        val firstScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        repositoryIn(firstScope).apply {
            startTry(OCTOBER_26)
            endTry(OCTOBER_26, points = 9800)
            startTry(OCTOBER_26)
        }
        firstScope.coroutineContext.job.cancelAndJoin()

        // When
        val reopened = repositoryIn(backgroundScope)

        // Then
        assertThat(reopened.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_26, tries = 2, firstTry = 9800, best = 9800, firstTryOver = true))
    }

    @Test
    fun `an unreadable file gives no result of the day, and the next try is the first`() = runTest {
        // Given a file that is not a preferences file
        dailyFile.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.results.first()).isNull()
        repository.startTry(OCTOBER_26)
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1))
    }

    @Test
    fun `a file that cannot be read gives no result of the day`() = runTest {
        // Given a directory where the file should be: reading it fails with an IOException
        dailyFile.mkdirs()
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.results.first()).isNull()
    }

    @Test
    fun `a file with missing or negative values reads as safe values`() = runTest {
        // Given a file holding a date, no tries, a negative first try and a negative best
        val dataStore = dataStoreIn(backgroundScope)
        dataStore.edit { preferences ->
            preferences[longPreferencesKey("date")] = OCTOBER_26.toEpochDay()
            preferences[intPreferencesKey("first_try")] = -9800
            preferences[intPreferencesKey("best")] = -1
        }

        // When
        val results = DataStoreDailyChallengeRepository(dataStore).results.first()

        // Then a date has at least its one try, and points are never negative
        assertThat(results).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1, firstTry = 0, best = 0))
    }

    @Test
    fun `a failure other than an IOException while the results are read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val repository = DataStoreDailyChallengeRepository(FailingDataStore(IllegalStateException("a bug")))

        // When
        val failure = runCatching { repository.results.first() }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
    }

    override fun emptyRepository(scope: CoroutineScope): DailyChallengeRepository = repositoryIn(scope)

    /** The file of the results holding a first try of October 26 under way, read as it is, on a full disk. */
    override suspend fun unwritableRepository(scope: CoroutineScope): DailyChallengeRepository {
        val dataStore = dataStoreIn(scope)
        DataStoreDailyChallengeRepository(dataStore).startTry(OCTOBER_26)
        return DataStoreDailyChallengeRepository(UnwritableDataStore(dataStore))
    }

    /** The file of the results of the day, in [directory]. */
    private val dailyFile: File get() = File(directory, DAILY_CHALLENGE_FILE)

    /** A repository on the file of [directory], its DataStore living in [scope], as in production. */
    private fun repositoryIn(scope: CoroutineScope): DailyChallengeRepository =
        DataStoreDailyChallengeRepository(dataStoreIn(scope))

    /** The DataStore of the file of [directory], living in [scope]; one per file. */
    private fun dataStoreIn(scope: CoroutineScope) = preferencesDataStore(scope) { dailyFile.absolutePath.toPath() }
}
