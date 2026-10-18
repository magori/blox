package ch.magori.blox.core.audio

/** Where the sequencer writes its samples, from −1 to 1. [AudioTrackSink] plays them. */
public interface AudioSink {
    /**
     * Writes [samples]; waits while the sink is full, which paces the sequencer. [samples] is only
     * valid during the call: the sequencer reuses the array for the next slice.
     */
    public suspend fun write(samples: FloatArray)

    /** Stops playing and frees the sink; it is not written to again. */
    public fun release()
}
