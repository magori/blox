package ch.magori.blox.core.audio

/** The output of one sound effect, its samples loaded once: [StaticAudioTrack] plays it on the device. */
public interface EffectTrack {
    /** Plays the effect from its start, cutting it if it still plays. */
    public fun play()

    /** Frees the output; it is not played again. */
    public fun release()
}
