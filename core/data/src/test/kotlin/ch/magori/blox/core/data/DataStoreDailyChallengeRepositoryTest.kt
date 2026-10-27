package ch.magori.blox.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.LocalDate

/** The contract of the results of the day, on a DataStore file, and what is proper to a file: it is kept, and its errors. */
class DataStoreDailyChallengeRepositoryTest :
    DailyChallengeRepositoryContract(),
    DataStoreFileContract<DailyChallengeRepository> {
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

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("daysOutOfRange")
    fun `a date out of the range of LocalDate in the file gives no result of the day, and the next try is the first`(
        day: Long,
    ) = runTest {
        // Given a file whose date is no date, with its tries and its best
        val repository = repositoryOnFileOf(day)

        // When / Then it reads as an unreadable file, and the next try starts the date over
        assertThat(repository.results.first()).isNull()
        repository.startTry(OCTOBER_26)
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = OCTOBER_26, tries = 1))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("boundDays")
    fun `a date at a bound of the range of LocalDate in the file is read as it is`(
        day: Long,
        date: LocalDate,
    ) = runTest {
        // Given a file whose date is the first or the last day a LocalDate holds
        val repository = repositoryOnFileOf(day)

        // When / Then
        assertThat(repository.results.first()).isEqualTo(DailyResults(date = date, tries = 3, best = 5000))
    }

    @Test
    fun `a try that ends on a file whose date is out of range counts as a whole first try`() = runTest {
        // Given a file whose date is past LocalDate.MAX, with its tries and its best
        val repository = repositoryOnFileOf(Long.MAX_VALUE)

        // When a try of October 26 ends, without its start
        val result = repository.endTry(OCTOBER_26, points = 9800)

        // Then it is the first try of a new date: no best before it, and it is the first try and the best
        assertThat(result).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
        assertThat(repository.results.first())
            .isEqualTo(DailyResults(date = OCTOBER_26, tries = 1, firstTry = 9800, best = 9800, firstTryOver = true))
    }

    override fun emptyRepository(scope: CoroutineScope): DailyChallengeRepository = repositoryIn(scope)

    /** The file of the results holding a first try of October 26 under way, read as it is, on a full disk. */
    override suspend fun unwritableRepository(scope: CoroutineScope): DailyChallengeRepository {
        val dataStore = dataStoreIn(scope)
        DataStoreDailyChallengeRepository(dataStore).startTry(OCTOBER_26)
        return DataStoreDailyChallengeRepository(UnwritableDataStore(dataStore))
    }

    /** The file of the results of the day, in [directory]. */
    override val file: File get() = File(directory, DAILY_CHALLENGE_FILE)

    override fun repositoryIn(scope: CoroutineScope): DailyChallengeRepository =
        DataStoreDailyChallengeRepository(dataStoreIn(scope))

    override fun repositoryFailingWith(failure: Exception): DailyChallengeRepository =
        DataStoreDailyChallengeRepository(FailingDataStore(failure))

    override fun readsOf(repository: DailyChallengeRepository): List<Flow<Any?>> = listOf(repository.results)

    /** No result of the day. */
    override val emptyReads: List<Any?> = listOf(null)

    /** A try of October 26, which is the first. */
    override suspend fun write(repository: DailyChallengeRepository) = repository.startTry(OCTOBER_26)

    override val readsAfterWrite: List<Any?> = listOf(DailyResults(date = OCTOBER_26, tries = 1))

    /** The DataStore of the file of [directory], living in [scope]; one per file. */
    private fun dataStoreIn(scope: CoroutineScope) = preferencesDataStore(scope) { file.absolutePath.toPath() }

    /** A repository on the file of [directory] holding [day] as its date, 3 tries and a best of 5000. */
    private suspend fun TestScope.repositoryOnFileOf(day: Long): DailyChallengeRepository {
        val dataStore = dataStoreIn(backgroundScope)
        dataStore.edit { preferences ->
            preferences[longPreferencesKey("date")] = day
            preferences[intPreferencesKey("tries")] = 3
            preferences[intPreferencesKey("best")] = 5000
        }
        return DataStoreDailyChallengeRepository(dataStore)
    }

    private companion object {
        /** Epoch days no LocalDate holds: the ends of a Long, and the days just past the bounds of a LocalDate. */
        @JvmStatic
        fun daysOutOfRange(): List<Arguments> = listOf(
            argumentSet("Long.MIN_VALUE", Long.MIN_VALUE),
            argumentSet("LocalDate.MIN - 1", LocalDate.MIN.toEpochDay() - 1),
            argumentSet("LocalDate.MAX + 1", LocalDate.MAX.toEpochDay() + 1),
            argumentSet("Long.MAX_VALUE", Long.MAX_VALUE),
        )

        /** The first and the last epoch days a LocalDate holds, with their date. */
        @JvmStatic
        fun boundDays(): List<Arguments> = listOf(
            argumentSet("LocalDate.MIN", LocalDate.MIN.toEpochDay(), LocalDate.MIN),
            argumentSet("LocalDate.MAX", LocalDate.MAX.toEpochDay(), LocalDate.MAX),
        )
    }
}
