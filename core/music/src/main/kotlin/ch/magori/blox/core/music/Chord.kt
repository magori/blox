package ch.magori.blox.core.music

/** A triad, given by its two stacked thirds in semitones: 4 for a major third, 3 for a minor one. */
public enum class ChordQuality(thirds: List<Int>) {
    MAJOR(listOf(4, 3)),
    MINOR(listOf(3, 4)),
    DIMINISHED(listOf(3, 3)),
    ;

    /** Semitones from the root to the root, the third and the fifth. */
    internal val offsets: List<Int> = thirds.runningFold(0, Int::plus)
}

/** The triad of [quality] on [root]. */
public data class Chord(public val root: Pitch, public val quality: ChordQuality) {
    /**
     * The root, the third and the fifth, from low to high. Refused with an `IllegalArgumentException` if one of them
     * goes past B8; [contains] still answers.
     */
    public val pitches: List<Pitch> get() = quality.offsets.map { Pitch(root.midi + it) }

    /** Whether [pitch] is the root, the third or the fifth, in any octave. */
    public operator fun contains(pitch: Pitch): Boolean = pitch.semitonesAboveInOctave(root) in quality.offsets
}
