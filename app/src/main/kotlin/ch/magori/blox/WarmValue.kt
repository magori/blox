package ch.magori.blox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * A value of `app` read once from its [source] and kept warm in [scope], for the life of [scope]: a screen coming
 * back knows it from its first frame ([current]), without waiting for a collection, and a tap finds it at once
 * ([await]), up to date with the last reading. Before the first reading, it is unknown (null), so that nothing shows
 * that may have to change. A source whose value may itself be null wraps it, so that « read, none » and « not read »
 * stay apart (`NewCartridges`).
 */
internal class WarmValue<T : Any>(source: Flow<T>, scope: CoroutineScope) {
    /** The value, at each reading of the [source]; null before the first. */
    val current: StateFlow<T?> = source.stateIn(scope, SharingStarted.Eagerly, null)

    /** The last value read, then each new value that differs; nothing before the first reading. */
    val updates: Flow<T> = current.filterNotNull()

    /** The value, once the source is read. */
    suspend fun await(): T = updates.first()
}
