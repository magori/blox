package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A DataStore whose every read and write fails with [failure]: a bug, which a file on disk cannot give. */
internal class FailingDataStore(private val failure: Exception) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw failure }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw failure
}
