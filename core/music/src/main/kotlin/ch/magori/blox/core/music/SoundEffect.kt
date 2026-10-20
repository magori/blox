package ch.magori.blox.core.music

/**
 * A short sound effect of the game, a chiptune beep: [blips] played one after the other on [waveform], at
 * [volume] (above 0, at most 1), each one shaped by [envelope], which fits in the shortest of them. Pure data,
 * timed in milliseconds rather than in beats: an effect has no tempo, it plays over the music whatever its tempo.
 */
public data class SoundEffect(
    public val waveform: Waveform,
    public val volume: Double,
    public val envelope: Envelope,
    public val blips: List<Blip>,
) {
    init {
        require(volume > 0 && volume <= 1) { "The volume of an effect is above 0 and at most 1, was $volume" }
        require(blips.isNotEmpty()) { "An effect has at least one blip" }
        val shortest = blips.minOf { it.millis }
        require(envelope.millis <= shortest) {
            "The envelope, ${envelope.millis} ms, fits in the shortest blip, $shortest ms"
        }
    }

    /** The length of the effect, in milliseconds. */
    public val millis: Int get() = blips.sumOf { it.millis }
}

/** One tone of a [SoundEffect]: [pitch], held a positive number of [millis] milliseconds. */
public data class Blip(public val pitch: Pitch, public val millis: Int) {
    init {
        require(millis > 0) { "A blip lasts a positive number of milliseconds, was $millis" }
    }
}

/**
 * The shape of each blip of a [SoundEffect]: its level rises linearly from silence to full over
 * [attackMillis], stays full, then falls linearly back to silence over its last [releaseMillis]. Neither is
 * negative; a rise and a fall of a few milliseconds keep the blip from clicking.
 */
public data class Envelope(public val attackMillis: Int, public val releaseMillis: Int) {
    init {
        require(attackMillis >= 0) { "The attack of an envelope cannot be negative, was $attackMillis ms" }
        require(releaseMillis >= 0) { "The release of an envelope cannot be negative, was $releaseMillis ms" }
    }

    /** The attack and the release together: the shortest blip the envelope fits in. */
    internal val millis: Int get() = attackMillis + releaseMillis
}
