package ch.magori.blox.core.music

/**
 * The timbre a [SoundStyle] gives to a voice, chosen by its [Waveform], which says its role (the melody or the
 * bass): its [tone], the [shape] of each of its notes, the cutoff of its one-pole low-pass filter in hertz
 * ([lowPass], none if null) and the [gain] applied to its samples.
 */
public data class Instrument(
    public val tone: Tone,
    public val shape: NoteShape,
    public val lowPass: Double?,
    public val gain: Double,
)

/** The wave an [Instrument] plays. */
public enum class Tone {
    /** A square, high for half of its period: the melody of Arcade. */
    SQUARE,

    /** A triangle: the bass of Arcade and of Velvet. */
    TRIANGLE,

    /** A pulse high for a quarter of its period, band-limited (PolyBLEP): the bass of Neon. */
    PULSE_QUARTER,

    /** A sine: the melody of Velvet. */
    SINE,

    /** A sawtooth, band-limited (PolyBLEP): the melody of Neon. */
    SAW,
}

/**
 * The shape of each note an [Instrument] plays, over the length of the note: [Gate] or [Adsr]. A sound effect keeps
 * its own [Envelope] whatever the instrument.
 */
public sealed interface NoteShape {
    /** The time a note takes to rise from silence to its full level, in milliseconds. */
    public val attackMillis: Int
}

/**
 * A note that rises from silence to full over [attackMillis], falls to the level [sustain] (from 0 to 1) over
 * [decayMillis], holds it, then releases to silence over [releaseMillis] at the end of the note. Times in
 * milliseconds, none negative.
 */
public data class Adsr(
    override val attackMillis: Int,
    public val decayMillis: Int,
    public val sustain: Double,
    public val releaseMillis: Int,
) : NoteShape {
    init {
        require(attackMillis >= 0 && decayMillis >= 0 && releaseMillis >= 0) {
            "The times of an ADSR cannot be negative, were $attackMillis, $decayMillis and $releaseMillis ms"
        }
        require(sustain in 0.0..1.0) { "The sustain of an ADSR is from 0 to 1, was $sustain" }
    }
}

/**
 * The shape of the notes of BloX before the instruments: full volume from the start of the note up to [heldUntil]
 * of it, a linear fade out up to [silentFrom], then silence, so that repeated notes stay distinct.
 */
public data object Gate : NoteShape {
    override val attackMillis: Int = 0

    /** The fraction of the note held at full volume. */
    public val heldUntil: Double = 0.80

    /** The fraction of the note from which it is silent. */
    public val silentFrom: Double = 0.95
}
