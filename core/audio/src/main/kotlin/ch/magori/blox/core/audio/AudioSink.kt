package ch.magori.blox.core.audio

/** Where the sequencer writes its samples, from −1 to 1. [AudioTrackSink] plays them. */
public interface AudioSink {
    /**
     * Writes [samples]; waits while the sink is full, which paces the sequencer. [samples] is only
     * valid during the call: the sequencer reuses the array for the next slice. Throws
     * [DeadSinkException] when the output died, so that the sequencer opens another sink.
     */
    public suspend fun write(samples: FloatArray)

    /**
     * Plays the samples at [gain], from 0 (silent) to 1 (as written, the gain of a new sink), the samples written
     * already included, as far as the output allows: the gain of the volume of the music ([VolumeControl]).
     */
    public fun setGain(gain: Float)

    /** Stops playing and frees the sink; it is not written to again. */
    public fun release()
}
