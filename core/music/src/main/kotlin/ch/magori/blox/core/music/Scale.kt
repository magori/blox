package ch.magori.blox.core.music

/** The kind of a [Scale], given by its [steps] in semitones from one degree to the next, over one octave. */
public enum class ScaleType(internal val steps: List<Int>) {
    MAJOR(listOf(2, 2, 1, 2, 2, 2, 1)),

    /** The Aeolian mode. */
    NATURAL_MINOR(listOf(2, 1, 2, 2, 1, 2, 2)),

    /** The natural minor with a raised seventh degree. */
    HARMONIC_MINOR(listOf(2, 1, 2, 2, 1, 3, 1)),
    DORIAN(listOf(2, 1, 2, 2, 2, 1, 2)),
    MIXOLYDIAN(listOf(2, 2, 1, 2, 2, 1, 2)),

    /** The major scale without its fourth and seventh degrees; its chords are those of [MAJOR]. */
    MAJOR_PENTATONIC(listOf(2, 2, 3, 2, 3)),
    ;

    /** Semitones from the tonic to each degree of the first octave, the tonic first. */
    internal val offsets: List<Int> = steps.runningFold(0, Int::plus).dropLast(1)

    init {
        require(steps.sum() == SEMITONES_PER_OCTAVE) { "The steps of $name, $steps, do not add up to one octave" }
    }

    /** The seven-note scale whose degrees the numerals count: the scale itself, or [MAJOR] for the pentatonic. */
    internal val harmony: ScaleType get() = if (this == MAJOR_PENTATONIC) MAJOR else this
}

/** The scale of [type] built on [tonic]. */
public data class Scale(public val tonic: Pitch, public val type: ScaleType) {
    /**
     * The degrees of the first octave, from the tonic up. Refused with an `IllegalArgumentException` if one of them
     * goes past B8.
     */
    public val pitches: List<Pitch> get() = (1..type.offsets.size).map(::degree)

    /**
     * The pitch of degree [number], counted from 1 for the tonic. The degrees go on past the octave (8 is the
     * tonic an octave up in a seven-note scale) and below the tonic (0 is the degree just under it).
     * A degree outside C0..B8 is refused with an `IllegalArgumentException`.
     */
    public fun degree(number: Int): Pitch {
        val index = number - 1
        val size = type.offsets.size
        return Pitch(tonic.midi + index.floorDiv(size) * SEMITONES_PER_OCTAVE + type.offsets[index.mod(size)])
    }

    /**
     * The chord that [numeral] names in this scale: its root is the degree of the numeral, moved by its
     * accidental, and its quality the one of the numeral, even when its third or fifth is not in the scale (the
     * major V of a minor key). The degrees are those of the seven-note scale of the harmony: the major scale on
     * the same tonic for a pentatonic scale.
     */
    public fun chord(numeral: RomanNumeral): Chord {
        val root = Scale(tonic, type.harmony).degree(numeral.degree)
        return Chord(Pitch(root.midi + numeral.accidental.semitones), numeral.quality)
    }

    /**
     * The triad that stacks two thirds of the scale on its fifth degree, for the half cadence: the major V of the major
     * and harmonic minor scales, the minor v of the natural minor, dorian and mixolydian ones. Like the numerals, it
     * counts the degrees of the major scale on the same tonic for a pentatonic scale. The fifth of that triad is
     * perfect in every [ScaleType], so its third alone sets its quality.
     */
    internal fun fifthTriad(): Chord {
        val harmony = Scale(tonic, type.harmony)
        val root = harmony.degree(FIFTH)
        val third = harmony.degree(FIFTH + 2).midi - root.midi
        return Chord(root, if (third == MAJOR_THIRD) ChordQuality.MAJOR else ChordQuality.MINOR)
    }

    /** Whether [pitch] is one of the degrees, in any octave. */
    public operator fun contains(pitch: Pitch): Boolean = pitch.semitonesAboveInOctave(tonic) in type.offsets

    private companion object {
        const val FIFTH = 5

        /** Semitones in a major third. */
        const val MAJOR_THIRD = 4
    }
}
