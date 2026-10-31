package ch.magori.blox.core.audio

import kotlin.math.PI
import kotlin.math.exp

/**
 * [samples] passed through a one-pole low-pass of [cutoff] hertz, at [sampleRate] samples per second: each output moves
 * from the previous one towards the input by a fraction `1 − e^(−2π × cutoff / sampleRate)`, starting from silence.
 * A constant passes unchanged, a tone at the cutoff comes out about 3 dB lower, and the attenuation grows by about
 * 6 dB an octave above it.
 */
internal fun lowPass(
    samples: FloatArray,
    cutoff: Double,
    sampleRate: Int,
): FloatArray {
    val step = 1 - exp(-2 * PI * cutoff / sampleRate)
    val filtered = FloatArray(samples.size)
    var level = 0.0
    for (index in samples.indices) {
        level += step * (samples[index] - level)
        filtered[index] = level.toFloat()
    }
    return filtered
}
