package ch.magori.blox.core.music

/**
 * A suite of chords, one per bar, written as [numerals] so that it fits any key. Names are read with [parse]:
 * numerals separated by hyphens or en dashes (`"I-V-vi-IV"`, `"I–V–vi–IV"`). The music loops, and so does the
 * progression: after its last chord it starts again.
 */
public data class Progression(public val numerals: List<RomanNumeral>) {
    init {
        require(numerals.isNotEmpty()) { "A progression holds at least one chord" }
    }

    /** The chord of [bar], counted from 0, in [scale]. */
    public fun chordAt(
        bar: Int,
        scale: Scale,
    ): Chord {
        require(bar >= 0) { "Bars are counted from 0, was $bar" }
        return scale.chord(numerals[bar % numerals.size])
    }

    /**
     * The chords of [bar], counted from 0, in [scale], at the pace of [harmonicRhythm]: the progression moves on to its
     * next chord every share of the bar, so that two chords a bar play beats 1 and 2, then beats 3 and 4.
     */
    internal fun chordsOfBar(
        bar: Int,
        scale: Scale,
        harmonicRhythm: HarmonicRhythm,
    ): List<Chord> {
        val count = harmonicRhythm.chordsPerBar
        return List(count) { chordAt(bar * count + it, scale) }
    }

    public companion object {
        private val SEPARATORS = arrayOf("-", "–")

        /** Reads numerals separated by hyphens or en dashes, such as `"I-V-vi-IV"` or `"i-VI-III-VII"`. */
        public fun parse(names: String): Progression = Progression(names.split(*SEPARATORS).map(RomanNumeral::parse))
    }
}
