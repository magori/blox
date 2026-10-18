package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The record kept in [dataStore], DataStore Preferences. The DataStore is created once, by the
 * application container (ADR 0007): a file has a single DataStore instance.
 */
class DataStoreHighScoreRepository(private val dataStore: DataStore<Preferences>) : HighScoreRepository {
    /**
     * The record; 0 while the file cannot be read (an unreadable file is replaced by an empty one, see
     * [preferencesDataStore]). [distinctUntilChanged] is a defence: the records file holds nothing else.
     */
    override val highScore: Flow<Int> = dataStore.readOrEmpty()
        .map { it[HIGH_SCORE] ?: 0 }
        .distinctUntilChanged()

    /** Reads and replaces the record in one atomic update, so that concurrent submits keep the highest. */
    override suspend fun submit(score: Int): RecordResult {
        require(score >= 0) { "A score cannot be negative: $score" }
        var previous = 0
        dataStore.edit { preferences ->
            previous = preferences[HIGH_SCORE] ?: 0
            if (score > previous) preferences[HIGH_SCORE] = score
        }
        return RecordResult(previous = previous, isNewRecord = score > previous)
    }

    private companion object {
        val HIGH_SCORE = intPreferencesKey("high_score")
    }
}
