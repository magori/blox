package ch.magori.blox.core.audio

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * A store of ringtones in memory, its entries numbered from 1. Each call throws the failure given for it, if any:
 * [insertFailure] on making an entry, [writeFailure] on the first write to a stream, [publishFailure] on publishing,
 * [deleteFailure] on removing (the entry then stays).
 */
class FakeRingtoneStore(
    private val insertFailure: Exception? = null,
    private val writeFailure: Exception? = null,
    private val publishFailure: Exception? = null,
    private val deleteFailure: Exception? = null,
) : RingtoneStore<Int> {
    /** An entry: the file asked for, its kind, the bytes written, and, once published, its bytes then. */
    class Entry(val fileName: String, val kind: RingtoneKind) {
        val bytes = ByteArrayOutputStream()
        var closed = false
        var publishedBytes: ByteArray? = null
        var closedWhenPublished = false
    }

    /** The entries still in the store, by their number. */
    val entries = mutableMapOf<Int, Entry>()

    private var lastNumber = 0

    override fun insert(
        fileName: String,
        kind: RingtoneKind,
    ): Int {
        insertFailure?.let { throw it }
        lastNumber += 1
        entries[lastNumber] = Entry(fileName, kind)
        return lastNumber
    }

    override fun output(entry: Int): OutputStream {
        val target = entries.getValue(entry)
        return object : OutputStream() {
            override fun write(byte: Int) {
                writeFailure?.let { throw it }
                target.bytes.write(byte)
            }

            override fun write(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ) {
                writeFailure?.let { throw it }
                target.bytes.write(bytes, offset, length)
            }

            override fun close() {
                target.closed = true
            }
        }
    }

    override fun publish(entry: Int) {
        publishFailure?.let { throw it }
        val target = entries.getValue(entry)
        target.publishedBytes = target.bytes.toByteArray()
        target.closedWhenPublished = target.closed
    }

    override fun delete(entry: Int) {
        deleteFailure?.let { throw it }
        entries -= entry
    }
}
