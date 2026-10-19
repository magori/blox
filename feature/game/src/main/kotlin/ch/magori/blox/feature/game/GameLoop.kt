package ch.magori.blox.feature.game

/** Simulation ticks per second: 1 tick = 1 frame = 1/60 s (ADR 0006). */
internal const val TICKS_PER_SECOND: Int = 60

/**
 * Turns frame times into fixed simulation ticks of 1/60 s (ADR 0006 and 0012).
 *
 * The screen passes the time of each frame to [advance], which returns how many ticks are due
 * since the previous frame. The remainder is carried over to the next frame. Everything is
 * computed with integers, so there is no drift. A frame never gives more than
 * [maxTicksPerFrame] ticks: after a long pause (application in the background), the surplus
 * is dropped instead of being caught up. Below 15 frames per second (60 / 4), the cap therefore
 * makes the game run slower instead of catching up, which is intended (ADR 0006). Above 60 Hz, the ticks
 * would fall unevenly on the frames (one or none in turn at 90 Hz): the game screen asks the display for
 * 60 Hz while it shows ([gameDisplayMode]).
 */
internal class GameLoop(private val maxTicksPerFrame: Int = 4) {
    init {
        require(maxTicksPerFrame > 0) { "maxTicksPerFrame must be positive, was $maxTicksPerFrame" }
    }

    private var previousFrameNanos: Long? = null

    /** Elapsed real time, in nanoseconds multiplied by [TICKS_PER_SECOND], not yet turned into ticks. */
    private var scaledNanos = 0L

    /** Returns the number of ticks due at [frameNanos]; the first frame only sets the time reference. */
    fun advance(frameNanos: Long): Int {
        val previous = previousFrameNanos
        previousFrameNanos = frameNanos
        if (previous == null) return 0
        scaledNanos += (frameNanos - previous).coerceAtLeast(0L) * TICKS_PER_SECOND
        val ticks = scaledNanos / NANOS_PER_SECOND
        scaledNanos %= NANOS_PER_SECOND
        return minOf(ticks, maxTicksPerFrame.toLong()).toInt()
    }

    /** Forgets the time reference and the remainder: the next frame gives no tick (resuming play). */
    fun reset() {
        previousFrameNanos = null
        scaledNanos = 0L
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
