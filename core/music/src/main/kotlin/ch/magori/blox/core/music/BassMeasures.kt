package ch.magori.blox.core.music

import kotlin.math.abs

/** The measures of [bass] over its [form], from which the quality of a music is noted, shares from 0 to 1. */
internal class BassMeasures(private val form: Form, private val bass: Bass) {
    /**
     * The share of the changes of chord that the bass makes by a fourth at most, from the last note under a chord to the
     * first under the next, from bar 8 back to bar 1 too as the music loops: a bass that leads smoothly from chord to
     * chord rather than leaping.
     */
    val smoothShare: Double
        get() {
            val struck = bass.struck
            val changes = struck.indices.map {
                struck[it] to struck[(it + 1) % struck.size]
            }.filter { (before, after) ->
                form.chordUnder(before) != form.chordUnder(after)
            }
            return changes.shareOf(ifNone = 1.0) { (before, after) ->
                abs(after.pitch.midi - before.pitch.midi) <= PERFECT_FOURTH
            }
        }
}
