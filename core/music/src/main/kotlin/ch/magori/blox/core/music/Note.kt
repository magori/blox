package ch.magori.blox.core.music

/** A note of [beats] beats (1 = quarter note, 0.5 = eighth note); a `null` [pitch] is a rest. */
public data class Note(public val pitch: Pitch?, public val beats: Double) {
    init {
        require(beats > 0 && beats.isFinite()) { "A note lasts a positive, finite number of beats, was $beats" }
    }

    public val isRest: Boolean get() = pitch == null
}
