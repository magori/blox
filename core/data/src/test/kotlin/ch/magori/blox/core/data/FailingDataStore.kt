package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A DataStore whose every read and write fails with [failure]: a bug, which a file on disk cannot give. */
internal class FailingDataStore<T>(private val failure: Exception) : DataStore<T> {
    override val data: Flow<T> = flow { throw failure }

    override suspend fun updateData(transform: suspend (t: T) -> T): T = throw failure
}
