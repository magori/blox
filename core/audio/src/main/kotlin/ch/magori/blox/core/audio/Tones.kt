package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Tone
import kotlin.math.PI
import kotlin.math.sin

/**
 * Amplitude from −1 to 1 at [phase], a fraction of the period in [0, 1), of a wave whose phase moves by [phaseStep]
 * (its frequency over the sample rate) from one sample to the next.
 */
internal fun Tone.amplitudeAt(
    phase: Double,
    phaseStep: Double,
): Double = when (this) {
    Tone.SQUARE -> if (phase < 0.5) 1.0 else -1.0

    Tone.TRIANGLE -> when {
        phase < QUARTER -> 4 * phase
        phase < 1 - QUARTER -> 2 - 4 * phase
        else -> 4 * phase - 4
    }

    Tone.SINE -> sin(2 * PI * phase)

    Tone.PULSE_QUARTER -> {
        val naive = if (phase < QUARTER) 1.0 else -1.0
        naive + polyBlep(phase, phaseStep) - polyBlep((phase + 1 - QUARTER) % 1.0, phaseStep)
    }

    Tone.SAW -> 2 * phase - 1 - polyBlep(phase, phaseStep)
}

/**
 * The PolyBLEP correction of a step of 2 at phase 0, for a wave whose phase moves by [phaseStep] a sample: a polynomial
 * that rounds the step over the sample before it and the sample after it, so that its harmonics above the Nyquist
 * frequency mostly vanish instead of folding back as aliasing.
 */
private fun polyBlep(
    phase: Double,
    phaseStep: Double,
): Double = when {
    phase < phaseStep -> {
        val x = phase / phaseStep
        2 * x - x * x - 1
    }

    phase > 1 - phaseStep -> {
        val x = (phase - 1) / phaseStep
        x * x + 2 * x + 1
    }

    else -> 0.0
}

private const val QUARTER = 0.25
