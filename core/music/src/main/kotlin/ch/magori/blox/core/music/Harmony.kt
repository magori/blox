package ch.magori.blox.core.music

/**
 * A [scaleType] and the [progressions] that suit it. A numeral without an accidental builds its chord from the
 * notes of the scale (of the major scale on the same tonic for a pentatonic one); a numeral with one borrows a chord
 * whose root is out of the scale, like the `♭VII` of a harmonic minor scale.
 */
public data class Harmony(public val scaleType: ScaleType, public val progressions: List<Progression>) {
    init {
        require(progressions.isNotEmpty()) { "A harmony holds at least one progression on $scaleType" }
        val scale = Scale(PROBE_TONIC, scaleType.harmony)
        progressions.flatMap { it.numerals }.forEach { numeral ->
            val chord = scale.chord(numeral)
            if (numeral.accidental == Accidental.NATURAL) {
                require(chord.pitches.all { it in scale }) {
                    "The chord $numeral of a progression on $scaleType is out of the scale"
                }
            } else {
                require(chord.root !in scale) {
                    "The accidental of $numeral leaves its root in the $scaleType scale: write the degree without it"
                }
            }
        }
    }

    private companion object {
        /** Any tonic: whether a chord is in a scale depends on their intervals only. */
        val PROBE_TONIC = Pitch.parse("C4")
    }
}
