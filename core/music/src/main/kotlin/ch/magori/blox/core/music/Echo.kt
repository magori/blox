package ch.magori.blox.core.music

/**
 * The echo of a [SoundStyle], applied to the mix: the sound comes back after [delayBeats] beats, so that it follows
 * the tempo, each time at [feedback] of its level (from 0 to below 1), and the echo is added at [wet] of the mix
 * (from 0 to 1).
 */
public data class Echo(public val delayBeats: Double, public val feedback: Double, public val wet: Double) {
    init {
        require(delayBeats > 0) { "An echo comes back after some beats, was $delayBeats" }
        require(feedback in 0.0..<1.0) {
            "The feedback of an echo is from 0 to below 1, or it never dies out, was $feedback"
        }
        require(wet in 0.0..1.0) { "The part of an echo in the mix is from 0 to 1, was $wet" }
    }
}
