package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/**
 * The data of [this] DataStore, of any kind (preferences, saved musics). If the file cannot be read (an [IOException]):
 * [empty], once, and the flow then ends, the contract of the repositories. Any other failure is a bug and goes through.
 */
internal fun <T> DataStore<T>.readOr(empty: T): Flow<T> =
    data.catch { failure -> if (failure is IOException) emit(empty) else throw failure }
