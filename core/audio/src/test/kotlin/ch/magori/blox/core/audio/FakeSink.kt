package ch.magori.blox.core.audio

import kotlinx.coroutines.channels.Channel

/**
 * A sink whose writes wait for the test to receive them, one slice at a time. Its write number
 * [diesOnWrite], if any, fails as an output that died: a restart of the audio server, a new output.
 * With [releaseFails], its release throws, after being counted, as that of a track in a bad state.
 */
internal class FakeSink(
    val sampleRate: Int,
    private val diesOnWrite: Int? = null,
    private val releaseFails: Boolean = false,
) : AudioSink {
    val written = Channel<FloatArray>()

    /** How many times it was released: once, at most, for a sink well closed. */
    var releases = 0
        private set
    val released: Boolean get() = releases > 0
    private var writes = 0

    override suspend fun write(samples: FloatArray) {
        writes++
        if (writes == diesOnWrite) throw DeadSinkException("The audio output died")
        written.send(samples.copyOf())
    }

    override fun release() {
        releases++
        check(!releaseFails) { "The release failed" }
    }
}
