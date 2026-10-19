package ch.magori.blox.core.music

import kotlin.random.Random

/** A music the generator composes before choosing among several: its [form], its [melody] and its [bass]. */
internal data class Candidate(val form: Form, val melody: Melody, val bass: Bass) {
    /** How well the candidate follows the rules of writing, from which the best candidate is chosen. */
    val quality: Quality = Quality.of(form, melody, bass)

    companion object {
        /**
         * The candidate on [form], its melody then its bass drawn with [random], or `null` if no melody fits the form or
         * no bass fits the melody: the rule that rules out a candidate whatever its quality.
         */
        fun compose(
            form: Form,
            random: Random,
        ): Candidate? {
            val melody = Melody.compose(form, random) ?: return null
            val bass = Bass.compose(form, melody, random) ?: return null
            return Candidate(form, melody, bass)
        }
    }
}
