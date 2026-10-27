package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The file of the results of the daily challenge. Apart from the records: a daily game never enters them. It never
 * leaves the device (backup rules of `app`): restored on another phone, a first try would no longer mean anything.
 */
const val DAILY_CHALLENGE_FILE: String = "daily_challenge.preferences_pb"

/**
 * The results of the daily challenge kept in [dataStore], DataStore Preferences on [DAILY_CHALLENGE_FILE]. The
 * DataStore is created once, by the application container (ADR 0007): a file has a single DataStore instance. The
 * date is kept as its epoch day.
 */
class DataStoreDailyChallengeRepository(private val dataStore: DataStore<Preferences>) : DailyChallengeRepository {
    /**
     * The results; null, once, if the file cannot be read, and the flow then ends (an unreadable file is replaced by
     * an empty one, see [preferencesDataStore]). [distinctUntilChanged] is a defence, as for the records: DataStore
     * already leaves out a write that changes nothing.
     */
    override val results: Flow<DailyResults?> = dataStore.readOrEmpty()
        .map { it.results() }
        .distinctUntilChanged()

    override suspend fun startTry(date: LocalDate) {
        update { kept -> kept.afterStart(date) }
    }

    override suspend fun endTry(
        date: LocalDate,
        points: Int,
    ): RecordResult {
        require(points >= 0) { "Points cannot be negative: $points" }
        return update { kept -> kept.afterEnd(date, points) }.recordOf(date, points)
    }

    /**
     * Reads and replaces the results in one atomic update, and gives those read: what a result compares with is what
     * the update changed.
     */
    private suspend fun update(change: (DailyResults?) -> DailyResults): DailyResults? {
        var kept: DailyResults? = null
        dataStore.edit { preferences ->
            kept = preferences.results()
            preferences.write(change(kept))
        }
        return kept
    }

    /** The results kept, brought back within safe bounds: a date has at least one try, points are never negative. */
    private fun Preferences.results(): DailyResults? {
        val day = this[DATE] ?: return null
        return DailyResults(
            date = LocalDate.ofEpochDay(day),
            tries = (this[TRIES] ?: 0).coerceAtLeast(1),
            firstTry = (this[FIRST_TRY] ?: 0).coerceAtLeast(0),
            best = (this[BEST] ?: 0).coerceAtLeast(0),
            firstTryOver = this[FIRST_TRY_OVER] ?: false,
        )
    }

    private fun MutablePreferences.write(results: DailyResults) {
        this[DATE] = results.date.toEpochDay()
        this[TRIES] = results.tries
        this[FIRST_TRY] = results.firstTry
        this[BEST] = results.best
        this[FIRST_TRY_OVER] = results.firstTryOver
    }

    private companion object {
        val DATE = longPreferencesKey("date")
        val TRIES = intPreferencesKey("tries")
        val FIRST_TRY = intPreferencesKey("first_try")
        val BEST = intPreferencesKey("best")
        val FIRST_TRY_OVER = booleanPreferencesKey("first_try_over")
    }
}
