package ch.magori.blox.core.audio

import kotlinx.coroutines.channels.Channel

/** A sink whose writes wait for the test to receive them, one slice at a time. */
internal class FakeSink(val sampleRate: Int) : AudioSink {
    val written = Channel<FloatArray>()
    var released = false
        private set

    override suspend fun write(samples: FloatArray) {
        written.send(samples.copyOf())
    }

    override fun release() {
        released = true
    }
}
