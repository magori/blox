package ch.magori.blox.core.audio

import java.io.OutputStream

/**
 * Where ringtones are written (ADR 0031), each file an [Entry] of the store: the calls of `MediaStore` that
 * [RingtoneWriter] needs, so that a failure and what it leaves behind can be tested on the JVM. Each call may throw an
 * `IOException`.
 */
internal interface RingtoneStore<Entry> {
    /** A new entry, pending, hidden from the other applications, for a file asked to be named [fileName]. */
    fun insert(
        fileName: String,
        kind: RingtoneKind,
    ): Entry

    /** The stream to write the file of [entry] to, which the caller closes. */
    fun output(entry: Entry): OutputStream

    /** Makes [entry] seen by the other applications, its file whole. */
    fun publish(entry: Entry)

    /** Removes [entry] and its file. */
    fun delete(entry: Entry)
}
