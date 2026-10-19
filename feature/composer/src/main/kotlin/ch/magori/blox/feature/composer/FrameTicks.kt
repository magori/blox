package ch.magori.blox.feature.composer

import ch.magori.blox.feature.composer.model.TICKS_PER_SECOND

/**
 * Turns the frame times of the screen into ticks of 1/60 s (ADR 0006 and 0012), with integers, the rest carried over
 * to the next frame: no drift. Unlike the loop of a game (`GameLoop`, which drops the ticks of a long pause rather
 * than catch them up), it catches up every tick: the music goes on in real time while frames are skipped, and the
 * play head follows it. A frame time that goes back takes no tick away.
 */
internal class FrameTicks {
    private var previousFrameNanos: Long? = null

    /** Elapsed real time, in nanoseconds multiplied by [TICKS_PER_SECOND], not yet turned into ticks. */
    private var scaledNanos = 0L

    /** Returns the ticks due at [frameNanos]; the first frame only sets the time reference. */
    fun advance(frameNanos: Long): Int {
        val previous = previousFrameNanos
        previousFrameNanos = frameNanos
        if (previous == null) return 0
        scaledNanos += (frameNanos - previous).coerceAtLeast(0L) * TICKS_PER_SECOND
        val ticks = scaledNanos / NANOS_PER_SECOND
        scaledNanos %= NANOS_PER_SECOND
        return ticks.toInt()
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
