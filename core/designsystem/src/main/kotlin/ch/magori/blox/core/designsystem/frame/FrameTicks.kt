package ch.magori.blox.core.designsystem.frame

/** Simulation ticks per second: 1 tick = 1 frame = 1/60 s (ADR 0006). */
const val TICKS_PER_SECOND: Int = 60

private const val MILLIS_PER_SECOND = 1_000

/**
 * [ticks] in milliseconds, a fraction dropped: the duration an animation of Compose takes for a `motion-*` duration
 * of `BloxMotion`, on the clock of Compose rather than on the ticks of the game (ADR 0024).
 */
fun millisOf(ticks: Int): Int = ticks * MILLIS_PER_SECOND / TICKS_PER_SECOND

/**
 * Turns the frame times of a screen into fixed ticks of 1/60 s (ADR 0006 and 0012). Shared by the screens that
 * receive frames from `withFrameNanos`: the game and the editor's play head.
 *
 * The screen passes the time of each frame to [advance], which returns how many ticks are due since the previous
 * frame. The remainder is carried over to the next frame. Everything is computed with integers, so there is no drift.
 * The first frame, and the first one after [reset], only sets the time reference. A frame time that goes back takes no
 * tick away and leaves no debt.
 *
 * With a [maxTicksPerFrame], a frame never gives more ticks: the surplus is dropped instead of being caught up, the
 * fraction of a tick kept. Without one (`null`), every tick is caught up.
 *
 * @throws IllegalArgumentException if [maxTicksPerFrame] is zero or negative.
 */
class FrameTicks(private val maxTicksPerFrame: Int? = null) {
    init {
        require(maxTicksPerFrame == null || maxTicksPerFrame > 0) {
            "maxTicksPerFrame must be positive, was $maxTicksPerFrame"
        }
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
        return (if (maxTicksPerFrame == null) ticks else minOf(ticks, maxTicksPerFrame.toLong())).toInt()
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
