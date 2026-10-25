package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/** A DataStore read from [delegate] but whose every write fails with an [IOException], as a full disk. */
internal class UnwritableDataStore<T>(private val delegate: DataStore<T>) : DataStore<T> {
    override val data: Flow<T> = delegate.data

    override suspend fun updateData(transform: suspend (t: T) -> T): T = throw IOException("disk full")
}
