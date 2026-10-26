package ch.magori.blox.core.music

/** What a numeral does to the root of its chord: [FLAT] lowers it a semitone from the degree, [SHARP] raises it. */
public enum class Accidental(internal val symbol: String, internal val semitones: Int) {
    FLAT("♭", -1),
    NATURAL("", 0),
    SHARP("#", 1),
}

/** The numerals of the seven degrees, in capitals. */
private val NUMERALS = listOf("I", "II", "III", "IV", "V", "VI", "VII")

/**
 * A chord named after the [degree] of its root in a seven-note scale, from 1 to 7, moved a semitone by its
 * [accidental], and its [quality]. Names are read with [parse] and written by [name]: an optional `♭` or `#`,
 * then `"I"` to `"VII"` in capitals for a major chord, `"i"` to `"vii"` for a minor one, followed by `°` for a
 * diminished one (`"vii°"`, `"♭VII"`).
 */
public data class RomanNumeral(
    public val degree: Int,
    public val quality: ChordQuality,
    public val accidental: Accidental = Accidental.NATURAL,
) {
    init {
        require(degree in 1..NUMERALS.size) { "A numeral names a degree from 1 to ${NUMERALS.size}, was $degree" }
    }

    /** The name of the numeral, the one [parse] reads it from: `"I"`, `"vi"`, `"vii°"`, `"♭VII"`. */
    public val name: String get() {
        val numeral = NUMERALS[degree - 1]
        return accidental.symbol + when (quality) {
            ChordQuality.MAJOR -> numeral
            ChordQuality.MINOR -> numeral.lowercase()
            ChordQuality.DIMINISHED -> numeral.lowercase() + "°"
        }
    }

    public companion object {
        // Built by RomanNumeral itself, whose init and name read NUMERALS: a top-level val, ready before any of them.
        private val BY_NAME: Map<String, RomanNumeral> = Accidental.entries.flatMap { accidental ->
            (1..NUMERALS.size).flatMap { degree ->
                ChordQuality.entries.map { quality -> RomanNumeral(degree, quality, accidental) }
            }
        }.associateBy { it.name }

        /** Reads a name such as `"V"`, `"vi"`, `"vii°"` or `"♭VII"`. */
        public fun parse(name: String): RomanNumeral =
            requireNotNull(BY_NAME[name]) { "\"$name\" is not a numeral from I to VII such as V, vi, vii° or ♭VII" }
    }
}
