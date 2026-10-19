package ch.magori.blox.core.music

/**
 * A [scaleType] and the [progressions] that suit it, played at the pace of [harmonicRhythm]. A numeral without an
 * accidental builds its chord from the notes of the scale (of the major scale on the same tonic for a pentatonic one);
 * a numeral with one borrows a chord whose root is out of the scale, like the `♭VII` of a harmonic minor scale.
 */
public data class Harmony(
    public val scaleType: ScaleType,
    public val progressions: List<Progression>,
    public val harmonicRhythm: HarmonicRhythm = HarmonicRhythm.ONE_CHORD_PER_BAR,
) {
    init {
        require(progressions.isNotEmpty()) { "A harmony holds at least one progression on $scaleType" }
        progressions.forEach { progression ->
            val first = progression.numerals.first()
            require(first.degree == 1 && first.accidental == Accidental.NATURAL) {
                "The progression $progression on $scaleType starts on $first: it starts on the tonic, which the half " +
                    "cadence of the last bar calls back"
            }
        }
        progressions.forEach(::requireChordsChangeFromBarToBar)
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

    /**
     * Refuses [progression] if two bars running of a [Form] play the same chords, the last bar and the first one of the
     * loop included, so that no bar copies the one before it. Bars 4 and 8 are left out: the half cadence changes their
     * last chord, and their rhythm, which ends the phrase, sets them apart from the motif.
     */
    private fun requireChordsChangeFromBarToBar(progression: Progression) {
        val scale = Scale(PROBE_TONIC, scaleType)
        val chords = Form.BARS.map { progression.chordsOfBar(it, scale, harmonicRhythm) }
        val repeated = Form.BARS.filter { bar ->
            val next = (bar + 1) % chords.size
            bar !in Form.CADENCES && next !in Form.CADENCES && chords[bar] == chords[next]
        }
        require(repeated.isEmpty()) {
            val bars = repeated.map { it + 1 }
            "The progression $progression at $harmonicRhythm plays the same chords in bars $bars and the next ones: " +
                "a bar would copy the one before it"
        }
    }

    private companion object {
        /** Any tonic: whether a chord is in a scale depends on their intervals only. */
        val PROBE_TONIC = Pitch.parse("C4")
    }
}
