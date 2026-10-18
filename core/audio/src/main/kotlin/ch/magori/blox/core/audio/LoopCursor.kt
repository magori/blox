package ch.magori.blox.core.audio

/**
 * A place in [buffer] played in a loop. Immutable: [fill] gives the cursor after the slice. The
 * loop is seamless: after the last sample comes the first one, and a slice longer than the buffer
 * goes round it as many times as needed.
 */
internal class LoopCursor(private val buffer: FloatArray, val position: Int = 0) {
    init {
        require(buffer.isNotEmpty()) { "A loop needs at least one sample" }
    }

    /** Copies the next [slice] size samples of the loop into [slice]; returns the cursor after them. */
    fun fill(slice: FloatArray): LoopCursor {
        var read = position
        var written = 0
        while (written < slice.size) {
            val count = minOf(buffer.size - read, slice.size - written)
            buffer.copyInto(slice, destinationOffset = written, startIndex = read, endIndex = read + count)
            written += count
            read = (read + count) % buffer.size
        }
        return LoopCursor(buffer, read)
    }
}
