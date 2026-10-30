package ch.magori.blox.core.designsystem.frame

/**
 * The play head of a music listened to in a loop of [eighthsPerLoop] eighth notes, moved on by the frames of the
 * screen (ADR 0012): the music editor and the musical garden. Each frame gives its ticks ([FrameTicks]), without a cap,
 * unlike a game: the music goes on in real time while frames are skipped, and the play head catches up every tick to
 * follow it. The first frame only sets the time.
 */
class FramePlayHead(eighthsPerLoop: Int) {
    private val frames = FrameTicks()

    /** Where the music stands, from the start of the loop until the first frames. */
    var head: LoopPlayHead = LoopPlayHead.start(eighthsPerLoop)
        private set

    /** Moves the play head on by the ticks due at [frameNanos], played at [beatsPerMinute]; gives where it stands. */
    fun advance(
        frameNanos: Long,
        beatsPerMinute: Int,
    ): LoopPlayHead {
        head = head.advance(frames.advance(frameNanos), beatsPerMinute)
        return head
    }
}
