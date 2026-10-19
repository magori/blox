package ch.magori.blox.core.music

/**
 * The way the melody of a category leans, as factors on the weights of the notes it draws, 1 leaving a weight as it
 * is: [fifths] on the leaps of a perfect fifth, [descents] on the steps down, the steps up being divided by it, so
 * that the melody goes down by step and up by leaps, and [arpeggios] on the moves between two notes of the chord in
 * place.
 */
internal data class MelodyTraits(val fifths: Double = 1.0, val descents: Double = 1.0, val arpeggios: Double = 1.0) {
    init {
        mapOf("fifths" to fifths, "descents" to descents, "arpeggios" to arpeggios).forEach { (name, factor) ->
            require(factor > 0 && factor.isFinite()) { "The factor on the $name is positive and finite, was $factor" }
        }
    }

    companion object {
        /** A melody that leans no way. */
        val NEUTRAL: MelodyTraits = MelodyTraits()
    }
}
