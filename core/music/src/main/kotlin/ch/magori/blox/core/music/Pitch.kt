package ch.magori.blox.core.music

import kotlin.math.pow

/** Semitones in an octave, in equal temperament: shared by pitches, scales and chords. */
internal const val SEMITONES_PER_OCTAVE: Int = 12

/**
 * A pitch from C0 to B8, identified by its MIDI note number (A4 is 69, C0 is 12, B8 is 119). Names are
 * read with [parse], in English notation: a letter from C to B, an optional sharp, an octave from 0 to 8
 * (`"A4"`, `"F#5"`).
 */
@JvmInline
public value class Pitch(public val midi: Int) {
    init {
        require(midi in C0_MIDI..B8_MIDI) { "A pitch goes from C0 ($C0_MIDI) to B8 ($B8_MIDI), was MIDI number $midi" }
    }

    /** Equal temperament frequency in hertz: 440 × 2^((midi − 69) / 12). */
    public val frequency: Double get() = A4_HZ * 2.0.pow((midi - A4_MIDI) / SEMITONES_PER_OCTAVE.toDouble())

    private val pitchClass: Int get() = midi.mod(SEMITONES_PER_OCTAVE)

    /** Semitones from [base] up to this pitch, octaves left out: from 0 to 11, 0 for the same note in any octave. */
    internal fun semitonesAboveInOctave(base: Pitch): Int = (midi - base.midi).mod(SEMITONES_PER_OCTAVE)

    /**
     * The same note in the octave that starts at [lowest]: from [lowest] up to the semitone under the octave above it.
     * Refused with an `IllegalArgumentException` if it goes past B8.
     */
    public fun inOctaveFrom(lowest: Pitch): Pitch = Pitch(lowest.midi + semitonesAboveInOctave(lowest))

    /** The name of the pitch, written with sharps only: `"A4"`, `"F#5"`. */
    override fun toString(): String = NAMES[pitchClass] + (midi.floorDiv(SEMITONES_PER_OCTAVE) - 1)

    public companion object {
        private const val A4_MIDI = 69
        private const val C0_MIDI = 12
        private const val B8_MIDI = 119
        private const val A4_HZ = 440.0
        private val NAME = Regex("([A-G])(#?)([0-8])")
        private val SEMITONES = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
        private val NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        /** Reads a name such as `"A4"` or `"F#5"`: MIDI number = 12 × (octave + 1) + semitone. */
        public fun parse(name: String): Pitch {
            val match = requireNotNull(NAME.matchEntire(name)) { "\"$name\" is not a pitch name such as A4 or F#5" }
            val (letter, sharp, octave) = match.destructured
            val semitone = SEMITONES.getValue(letter.single()) + sharp.length
            return Pitch(SEMITONES_PER_OCTAVE * (octave.toInt() + 1) + semitone)
        }
    }
}
