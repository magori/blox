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

    /** An output whose write number [diesOnWrite] finds it dead. */
    private class DyingSink(private val diesOnWrite: Int) : AudioSink {
        private var writes = 0
        var released = false
            private set

        override suspend fun write(samples: FloatArray) {
            writes++
            if (writes == diesOnWrite) throw DeadSinkException("The audio output died")
        }

        override fun release() {
            released = true
        }
    }
}
