package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Echo
import kotlin.math.ceil
import kotlin.math.ln

/**
 * [samples] of a loop with [echo]: the sound comes back after [delaySamples], each time at the feedback of the echo,
 * and the echoes are added at the part of the echo. The echo is circular: what comes back past the end of the loop
 * sounds on its start, as when the loop plays again, so that the loop has no seam and keeps its length. The sum is
 * clipped to [−1, 1], like the mix; with the volumes of the scores and the echoes of the styles, it never needs to be.
 */
internal fun withEcho(
    samples: FloatArray,
    echo: Echo,
    delaySamples: Int,
): FloatArray {
    val size = samples.size
    val echoes = DoubleArray(size)
    // Each pass plays the loop once more: enough of them for the echoes from before the start to die out
    repeat(passesToDieOut(echo, delaySamples, size)) {
        for (index in 0 until size) {
            val from = Math.floorMod(index - delaySamples, size)
            echoes[index] = samples[from] + echo.feedback * echoes[from]
        }
    }
    return FloatArray(size) { (samples[it] + echo.wet * echoes[it]).coerceIn(-1.0, 1.0).toFloat() }
}

/**
 * The plays of a loop of [size] samples after which an echo of [delaySamples] that came back [repeats] times is
 * below [DIED_OUT] of its level: one, and one more for each loop those repeats span.
 */
private fun passesToDieOut(
    echo: Echo,
    delaySamples: Int,
    size: Int,
): Int {
    val repeats = if (echo.feedback == 0.0) 1.0 else ceil(ln(DIED_OUT) / ln(echo.feedback))
    return 1 + ceil(repeats * delaySamples / size).toInt()
}

/** The level, relative to the sound, below which an echo has died out: far below a step of 16 bits. */
private const val DIED_OUT = 1e-6
