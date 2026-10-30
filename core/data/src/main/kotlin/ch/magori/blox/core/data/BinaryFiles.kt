package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A DataStore of a binary format of BloX ([serializer]) living in [scope], on the file [produceFile] gives; one instance
 * per file. The storage is Okio's, as for the preferences. A corrupted file is copied aside first ([keepAside]), then
 * replaced by what [replaceCorrupted] gives, an empty content.
 */
internal fun <T> binaryDataStore(
    scope: CoroutineScope,
    serializer: OkioSerializer<T>,
    produceFile: () -> Path,
    replaceCorrupted: () -> T,
): DataStore<T> = DataStoreFactory.create(
    storage = OkioStorage(FileSystem.SYSTEM, serializer, producePath = produceFile),
    corruptionHandler = ReplaceFileCorruptionHandler {
        keepAside(produceFile())
        replaceCorrupted()
    },
    scope = scope,
)

/**
 * Copies the corrupted file at [path] to `<path>.corrupt`, replacing an older copy, before the DataStore replaces it: what
 * the player made (musics, a garden) may still be found in it, after a bug of a later version, say. A copy that fails (a
 * full disk) is given up, and the content still starts again empty.
 */
private fun keepAside(path: Path) {
    try {
        FileSystem.SYSTEM.copy(path, checkNotNull(path.parent) / "${path.name}$CORRUPTED_SUFFIX")
    } catch (_: IOException) {
        // The copy is a help for later, not a condition of reading the file again.
    }
}

/** What the copy of a corrupted file adds to its name. */
private const val CORRUPTED_SUFFIX = ".corrupt"

/**
 * A count of elements or of bytes, refused with an [IllegalArgumentException] if it is negative or beyond [most]: a
 * corrupted count must not have a list of billions of elements allocated before the end of the file is found.
 */
internal fun BufferedSource.readCount(most: Int): Int =
    readInt().also { require(it in 0..most) { "A count goes from 0 to $most, was $it" } }

/** A text written by [writeText], of [most] bytes at the most ([readCount]). */
internal fun BufferedSource.readText(most: Int): String = readUtf8(readCount(most).toLong())

/** A text as the binary files of BloX write it: its length in bytes, then its bytes in UTF-8. */
internal fun BufferedSink.writeText(text: String) {
    val bytes = text.encodeToByteArray()
    writeInt(bytes.size)
    write(bytes)
}

/** This time to the millisecond, as the binary files of BloX keep it (the musics, the kept gardens). */
internal fun Instant.toFileTime(): Instant = truncatedTo(ChronoUnit.MILLIS)

/** The length of this text in bytes, as [writeText] writes it, in UTF-8. */
internal fun String.utf8Size(): Int = encodeToByteArray().size
