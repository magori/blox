package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Waveform

/** Amplitude from −1 to 1 at [phase], a fraction of the period in [0, 1). */
internal fun Waveform.amplitudeAt(phase: Double): Double = when (this) {
    Waveform.SQUARE -> if (phase < 0.5) 1.0 else -1.0

    Waveform.TRIANGLE -> when {
        phase < 0.25 -> 4 * phase
        phase < 0.75 -> 2 - 4 * phase
        else -> 4 * phase - 4
    }
}
