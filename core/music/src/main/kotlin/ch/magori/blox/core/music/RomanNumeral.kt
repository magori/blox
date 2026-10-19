package ch.magori.blox.core.music

/** What a numeral does to the root of its chord: [FLAT] lowers it a semitone from the degree, [SHARP] raises it. */
public enum class Accidental(internal val symbol: String, internal val semitones: Int) {
    FLAT("♭", -1),
    NATURAL("", 0),
    SHARP("#", 1),
}

/**
 * A chord named after the [degree] of its root in a seven-note scale, from 1 to 7, moved a semitone by its
 * [accidental], and its [quality]. Names are read with [parse]: an optional `♭` or `#`, then `"I"` to `"VII"` in
 * capitals for a major chord, `"i"` to `"vii"` for a minor one, followed by `°` for a diminished one
 * (`"vii°"`, `"♭VII"`).
 */
public data class RomanNumeral(
    public val degree: Int,
    public val quality: ChordQuality,
    public val accidental: Accidental = Accidental.NATURAL,
) {
    init {
        require(degree in 1..NUMERALS.size) { "A numeral names a degree from 1 to ${NUMERALS.size}, was $degree" }
    }

    public companion object {
        private val NUMERALS = listOf("I", "II", "III", "IV", "V", "VI", "VII")
        private val BY_NAME: Map<String, RomanNumeral> = Accidental.entries.flatMap { accidental ->
            NUMERALS.withIndex().flatMap { (index, numeral) ->
                val degree = index + 1
                listOf(
                    numeral to ChordQuality.MAJOR,
                    numeral.lowercase() to ChordQuality.MINOR,
                    numeral.lowercase() + "°" to ChordQuality.DIMINISHED,
                ).map { (name, quality) -> accidental.symbol + name to RomanNumeral(degree, quality, accidental) }
            }
        }.toMap()

        /** Reads a name such as `"V"`, `"vi"`, `"vii°"` or `"♭VII"`. */
        public fun parse(name: String): RomanNumeral =
            requireNotNull(BY_NAME[name]) { "\"$name\" is not a numeral from I to VII such as V, vi, vii° or ♭VII" }
    }
}
