package ch.magori.blox.core.audio

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * A store of ringtones in memory, its entries numbered from 1. Its streams fail on their first write when
 * [failingWrites] is true.
 */
class FakeRingtoneStore(private val failingWrites: Boolean = false) : RingtoneStore<Int> {
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
        lastNumber += 1
        entries[lastNumber] = Entry(fileName, kind)
        return lastNumber
    }

    override fun output(entry: Int): OutputStream {
        val target = entries.getValue(entry)
        return object : OutputStream() {
            override fun write(byte: Int) {
                if (failingWrites) throw IOException("The storage is full")
                target.bytes.write(byte)
            }

            override fun write(
                bytes: ByteArray,
                offset: Int,
                length: Int,
            ) {
                if (failingWrites) throw IOException("The storage is full")
                target.bytes.write(bytes, offset, length)
            }

            override fun close() {
                target.closed = true
            }
        }
    }

    override fun publish(entry: Int) {
        val target = entries.getValue(entry)
        target.publishedBytes = target.bytes.toByteArray()
        target.closedWhenPublished = target.closed
    }

    override fun delete(entry: Int) {
        entries -= entry
    }
}
