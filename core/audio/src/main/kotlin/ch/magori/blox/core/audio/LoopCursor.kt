package ch.magori.blox.core.audio

/**
 * A place in [buffer] played in a loop. Immutable: [fill] gives the cursor after the slice. The
 * loop is seamless: after the last sample comes the first one, and a slice longer than the buffer
 * goes round it as many times as needed.
 */
internal class LoopCursor(private val buffer: FloatArray, val position: Int = 0) {
    init {
        require(buffer.isNotEmpty()) { "A loop needs at least one sample" }
        require(position in buffer.indices) { "The position is in the buffer, 0 until ${buffer.size}, was $position" }
    }

    /**
     * Copies the next samples of the loop into [slice], from index [from] until index [until] (the
     * whole slice by default); the rest of [slice] is left as it was. Returns the cursor after them.
     */
    fun fill(
        slice: FloatArray,
        from: Int = 0,
        until: Int = slice.size,
    ): LoopCursor {
        var read = position
        var written = from
        while (written < until) {
            val count = minOf(buffer.size - read, until - written)
            buffer.copyInto(slice, destinationOffset = written, startIndex = read, endIndex = read + count)
            written += count
            read = (read + count) % buffer.size
        }
        return LoopCursor(buffer, read)
    }
}
