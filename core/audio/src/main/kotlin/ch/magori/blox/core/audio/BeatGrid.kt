package ch.magori.blox.core.audio

/**
 * The beats of a buffer of [length] samples that holds [beats] beats: a beat lasts
 * `length / beats` samples, and starts on the nearest sample. The tempo changes on a beat, so that
 * the music keeps its time. Computed on [Long], so that `position × beats` never overflows.
 */
internal class BeatGrid(private val length: Int, private val beats: Int) {
    /**
     * The first beat that starts at [position] or after it; [beats] when it is the end of the
     * buffer, where the loop starts again.
     */
    fun nextBeat(position: Int): Int {
        val beat = (position.toLong() * beats / length).toInt()
        return if (startOf(beat) >= position) beat else beat + 1
    }

    /** The sample where [beat] starts, from 0 to [beats]; [beats] is the end of the buffer. */
    fun startOf(beat: Int): Int = ((2L * beat * length + beats) / (2L * beats)).toInt()
}
