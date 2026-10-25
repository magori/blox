package ch.magori.blox.core.audio

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReopeningSinkTest {
    private val opened = mutableListOf<DyingSink>()

    @Test
    fun `deaths far apart from one another are each reopened, beyond three`() = runTest {
        // Given outputs that each play 3 slices, then die, and a window of 2 slices
        val sink = ReopeningSink(window = 2) { DyingSink(diesOnWrite = 4).also { opened += it } }

        // When 20 slices are written
        repeat(20) { sink.write(FloatArray(1)) }

        // Then every death was far enough from the one before to be reopened: 7 outputs for 20 slices
        assertThat(opened).hasSize(7)
        assertThat(opened.dropLast(1)).allMatch { it.released }
    }

    @Test
    fun `an output reopened plays at the gain set`() = runTest {
        // Given an output that dies on its second write, the music turned down
        val sink = ReopeningSink { DyingSink(diesOnWrite = 2).also { opened += it } }
        sink.setGain(0.25f)

        // When it dies, and is reopened
        repeat(2) { sink.write(FloatArray(1)) }

        // Then the new output plays at the same gain
        assertThat(opened.map { it.gain }).containsExactly(0.25f, 0.25f)
    }

    /** An output whose write number [diesOnWrite] finds it dead; it keeps the [gain] it plays at. */
    private class DyingSink(private val diesOnWrite: Int) : AudioSink {
        private var writes = 0
        var released = false
            private set
        var gain = 1f
            private set

        override suspend fun write(samples: FloatArray) {
            writes++
            if (writes == diesOnWrite) throw DeadSinkException("The audio output died")
        }

        override fun setGain(gain: Float) {
            this.gain = gain
        }

        override fun release() {
            released = true
        }
    }
}
