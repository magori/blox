package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Volume
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A sink that reopens its output when a write finds it dead ([DeadSinkException]), then writes the same
 * samples to the new one: the music goes on where it stood. It reopens at most 3 times in a row; the
 * count starts again once an output has played [window] slices, so that deaths far apart are each
 * reopened, and an output that keeps dying stops the music. A sink reopened that dies again before that
 * write went through, another failure of a write, or an output that cannot reopen, reach the caller.
 * Each output it opens is released once, even when its release throws, the dead one before the next one
 * opens. An output that dies while the music stops is not reopened. An output reopened plays at the gain set.
 */
internal class ReopeningSink(private val window: Int = REOPENING_WINDOW, private val open: () -> AudioSink) :
    AudioSink {
    private var output: AudioSink? = open()

    /** The reopenings since the last output that played [window] slices. */
    private var reopenings = 0

    /** The slices written to the output since it was opened. */
    private var played = 0

    /** The gain set, that of a new output until another one is: an output reopened plays at it too. */
    private var gain = Volume.FULL.gain

    override suspend fun write(samples: FloatArray) {
        val current = checkNotNull(output) { "The sink is released" }
        try {
            current.write(samples)
        } catch (dead: DeadSinkException) {
            if (reopenings == MAX_REOPENINGS) throw dead
            reopenings++
            played = 0
            output = null
            current.release()
            // A real sink blocks instead of suspending: the music may have stopped during that write.
            currentCoroutineContext().ensureActive()
            val reopened = open().apply { setGain(gain) }
            output = reopened
            reopened.write(samples)
        }
        if (++played == window) reopenings = 0
    }

    override fun setGain(gain: Float) {
        this.gain = gain
        output?.setGain(gain)
    }

    override fun release() {
        val current = output ?: return
        output = null
        current.release()
    }

    private companion object {
        const val MAX_REOPENINGS = 3

        /** About 48 s of music in slices of [SLICE_SIZE] at 44 100 Hz. */
        const val REOPENING_WINDOW = 2_048
    }
}
