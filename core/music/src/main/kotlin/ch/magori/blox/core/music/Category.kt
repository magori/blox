package ch.magori.blox.core.music

import kotlin.random.Random

/**
 * The character of a generated music, as a set of parameters rather than code of its own: one generator plays them
 * all, so adding a category leaves it unchanged. [harmonies] gives its scales, each with the progressions that suit
 * it, [tempo] the tempos it allows, [rhythms] the rhythm cells its melody draws its bars from, [bass] the way its
 * bass goes, and [allowsChromaticPassingNotes] whether its melody may pass through a note out of the scale, on a
 * weak beat.
 */
public enum class Category(
    public val harmonies: List<Harmony>,
    public val tempo: TempoRange,
    public val rhythms: List<RhythmCell>,
    public val bass: BassStyle,
    public val allowsChromaticPassingNotes: Boolean = false,
    ownPhraseEnds: List<RhythmCell> = emptyList(),
) : CategoryChoice {
    /** Eighth notes and dotted eighths that skip along. */
    JOYFUL(
        harmonies = listOf(on(ScaleType.MAJOR, "I–V–vi–IV", "I–IV–V–I")),
        tempo = TempoRange(120, 150),
        rhythms = cells("0.5 0.5 0.5 0.5 1 1", "0.75 0.25 0.75 0.25 1 1", "1 0.5 0.5 0.75 0.25 1", "0.5 1 0.5 1 1"),
        bass = BassStyle.BOUNCING_OCTAVES,
        ownPhraseEnds = cells("0.5 0.5 1 2", "0.75 0.25 1 2"),
    ),

    /** Half and quarter notes, with air between them. */
    CALM(
        harmonies = listOf(on(ScaleType.MAJOR_PENTATONIC, "I–vi–IV–V", "I–IV")),
        tempo = TempoRange(70, 95),
        rhythms = cells("2 2", "1 1 2", "2 1 r1", "1 r1 2"),
        bass = BassStyle.WHOLE_AND_HALF_NOTES,
    ),

    /** Strong quarter notes, struck on every beat. */
    EPIC(
        harmonies = listOf(on(ScaleType.NATURAL_MINOR, "i–VI–III–VII", "i–VII–VI–VII")),
        tempo = TempoRange(110, 140),
        rhythms = cells("1 1 1 1", "1 0.5 0.5 1 1", "0.5 0.5 1 1 1", "1 1 0.5 0.5 1"),
        bass = BassStyle.GALLOP,
        ownPhraseEnds = cells("1 1 2", "1 1 1 r1"),
    ),

    /** Long notes. */
    MELANCHOLIC(
        harmonies = listOf(on(ScaleType.NATURAL_MINOR, "i–VI–iv–v", "i–iv–VII–III")),
        tempo = TempoRange(80, 100),
        rhythms = cells("2 2", "3 1", "1 1 2", "1.5 0.5 2"),
        bass = BassStyle.ROOT_AND_FIFTH,
    ),

    /** Held notes, with room for a chromatic passing note. */
    MYSTERIOUS(
        harmonies = listOf(on(ScaleType.DORIAN, "i–IV"), on(ScaleType.HARMONIC_MINOR, "i–♭VII–VI–V")),
        tempo = TempoRange(90, 110),
        rhythms = cells("4", "3 1", "2 2", "3 0.5 0.5"),
        bass = BassStyle.HELD_NOTES,
        allowsChromaticPassingNotes = true,
    ),

    /**
     * Sixteenth notes for fast arpeggios. The `♭VII` of `I–♭VII–IV–I` is the seventh degree of the mixolydian scale
     * itself, written `VII`.
     */
    RETRO(
        harmonies = listOf(
            on(ScaleType.MAJOR, "I–V–vi–IV", harmonicRhythm = HarmonicRhythm.TWO_CHORDS_PER_BAR),
            on(ScaleType.MIXOLYDIAN, "I–VII–IV–I", harmonicRhythm = HarmonicRhythm.TWO_CHORDS_PER_BAR),
        ),
        tempo = TempoRange(140, 170),
        rhythms = cells(
            "0.25 0.25 0.25 0.25 0.25 0.25 0.25 0.25 0.5 0.5 1",
            "0.5 0.5 0.5 0.5 0.5 0.5 0.5 0.5",
            "0.25 0.25 0.25 0.25 0.5 0.5 0.25 0.25 0.25 0.25 1",
            "0.5 1 0.5 0.5 0.5 1",
        ),
        bass = BassStyle.FAST_OCTAVES,
        ownPhraseEnds = cells("0.5 0.5 0.5 0.5 2", "0.25 0.25 0.25 0.25 0.5 0.5 2"),
    ),

    /** Eighth notes, beats 1 and 3 marked. */
    DANCE(
        harmonies = listOf(on(ScaleType.HARMONIC_MINOR, "i–iv–V–i")),
        tempo = TempoRange(130, 160),
        rhythms = cells("1 0.5 0.5 1 0.5 0.5", "1 0.5 0.5 1 1", "0.5 0.5 0.5 0.5 0.5 0.5 0.5 0.5", "1 1 1 0.5 0.5"),
        bass = BassStyle.MARKED_ROOT_AND_FIFTH,
        ownPhraseEnds = cells("1 0.5 0.5 2", "1 0.5 0.5 1 r1"),
    ),
    ;

    /**
     * The cells that end a phrase, at bars 4 and 8, on a long note or a silence: the [rhythms] that breathe, plus the
     * phrase-end cells of the category's own.
     */
    internal val phraseEnds: List<RhythmCell> = rhythms.filter(RhythmCell::breathes) + ownPhraseEnds

    /** A category chosen by the player is played as it is. */
    override fun resolve(random: Random): Category = this

    public companion object {
        /** The tonics every category may be played on: C, D, F, G and A, in the fourth octave. */
        public val TONICS: List<Pitch> = listOf("C4", "D4", "F4", "G4", "A4").map(Pitch::parse)
    }
}

private fun on(
    scaleType: ScaleType,
    vararg progressions: String,
    harmonicRhythm: HarmonicRhythm = HarmonicRhythm.ONE_CHORD_PER_BAR,
): Harmony = Harmony(scaleType, progressions.map(Progression::parse), harmonicRhythm)

private fun cells(vararg cells: String): List<RhythmCell> = cells.map(RhythmCell::parse)
