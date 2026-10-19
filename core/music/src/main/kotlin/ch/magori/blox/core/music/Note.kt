package ch.magori.blox.core.music

/** A note of [beats] beats (1 = quarter note, 0.5 = eighth note); a `null` [pitch] is a rest. */
public data class Note(public val pitch: Pitch?, public val beats: Double) {
    init {
        requirePositiveBeats(beats, "A note")
    }

    public val isRest: Boolean get() = pitch == null
}

/** Refuses with an `IllegalArgumentException` a duration of [beats] that is not positive and finite. */
internal fun requirePositiveBeats(
    beats: Double,
    what: String,
) {
    require(beats > 0 && beats.isFinite()) { "$what lasts a positive, finite number of beats, was $beats" }
}
