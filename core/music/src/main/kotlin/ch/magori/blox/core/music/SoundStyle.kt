package ch.magori.blox.core.music

/**
 * The setting « Instruments »: the sound of every music of BloX. Saved by name, so the names are frozen.
 *
 * A style gives an [Instrument] to each voice, chosen by its [Waveform], which says its role: [melody] for a
 * [Waveform.SQUARE] voice, [bass] for a [Waveform.TRIANGLE] one. Its [echo], if any, is applied to the mix.
 */
public enum class SoundStyle(private val melody: Instrument, private val bass: Instrument, public val echo: Echo?) {
    /** The sound of BloX before the instruments, 8-bit: a square and a triangle, gated, no filter, no echo. */
    ARCADE(
        melody = Instrument(Tone.SQUARE, Gate, lowPass = null, gain = 1.0),
        bass = Instrument(Tone.TRIANGLE, Gate, lowPass = null, gain = 1.0),
        echo = null,
    ),

    /** Soft and soothing: a sine and a filtered triangle, with an echo. */
    VELVET(
        melody = Instrument(Tone.SINE, Adsr(4, 250, 0.6, 200), lowPass = null, gain = 1.0),
        bass = Instrument(Tone.TRIANGLE, Adsr(5, 300, 0.7, 150), lowPass = 600.0, gain = 1.0),
        echo = Echo(delayBeats = 0.75, feedback = 0.3, wet = 0.25),
    ),

    /** Dynamic, synthpop: a filtered sawtooth and a filtered quarter pulse, short notes, with an echo. */
    NEON(
        melody = Instrument(Tone.SAW, Adsr(2, 120, 0.7, 60), lowPass = 2500.0, gain = 1.0),
        bass = Instrument(Tone.PULSE_QUARTER, Adsr(2, 80, 0.5, 40), lowPass = 900.0, gain = 1.0),
        echo = Echo(delayBeats = 0.5, feedback = 0.2, wet = 0.2),
    ),
    ;

    /** The instrument this style gives to a voice on [waveform]. */
    public fun instrumentFor(waveform: Waveform): Instrument = when (waveform) {
        Waveform.SQUARE -> melody
        Waveform.TRIANGLE -> bass
    }
}
