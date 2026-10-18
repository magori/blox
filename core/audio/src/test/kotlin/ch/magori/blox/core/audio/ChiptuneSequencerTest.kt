package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

class ChiptuneSequencerTest {
    private val korobeiniki = scoreOf(Pitch.parse("E5"))
    private val minuet = scoreOf(Pitch.parse("D5"))

    /** What the fake rendering gives: a short, recognisable buffer per score. */
    private val buffers = mapOf(
        korobeiniki to floatArrayOf(1f, 2f, 3f, 4f, 5f),
        minuet to floatArrayOf(-1f, -2f, -3f),
    )

    /** Each rendering: its score, its tempo factor, and whether it ran on the rendering dispatcher. */
    private val rendered = mutableListOf<Rendering>()
    private val sinks = mutableListOf<FakeSink>()
    private val gatedSinks = GatedSinks()

    @Test
    fun `play renders the score on the rendering dispatcher, then writes it`() = runTest {
        // Given
        val sequencer = sequencer()

        // When
        sequencer.play(korobeiniki)

        // Then
        assertThat(sinks).isEmpty()
        assertThat(nextSlice()).containsExactly(1f, 2f)
        assertThat(rendered).containsExactly(Rendering(korobeiniki, tempoFactor = 1.0, onRenderingDispatcher = true))
    }

    @Test
    fun `a score is rendered once, then played again from the cache`() = runTest {
        // Given a score played once
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When it is played again
        sequencer.play(korobeiniki)

        // Then it plays without being rendered again
        assertThat(nextSlice(sink = 1)).containsExactly(1f, 2f)
        assertThat(rendered).hasSize(1)
    }

    @Test
    fun `the slices are written in order, round the loop`() = runTest {
        // Given
        val sequencer = sequencer()

        // When
        sequencer.play(korobeiniki)

        // Then
        assertThat(List(4) { nextSlice().toList() }).containsExactly(
            listOf(1f, 2f),
            listOf(3f, 4f),
            listOf(5f, 1f),
            listOf(2f, 3f),
        )
    }

    @Test
    fun `stop ends the writing and releases the sink`() = runTest {
        // Given a score playing
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice()

        // When
        sequencer.stop()
        runCurrent()

        // Then
        assertThat(sinks.single().released).isTrue()
        assertThat(sinks.single().written.tryReceive().isSuccess).isFalse()
    }

    @Test
    fun `playing another score replaces the one playing, without leaving two loops`() = runTest {
        // Given a score playing
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When another one is played
        sequencer.play(minuet)

        // Then the new one plays, and the first sink is released and written no more
        assertThat(nextSlice(sink = 1)).containsExactly(-1f, -2f)
        assertThat(sinks[0].released).isTrue()
        assertThat(sinks[0].written.tryReceive().isSuccess).isFalse()
    }

    @Test
    fun `stopping a playback that was replaced leaves the new one playing`() = runTest {
        // Given a score playing, replaced by another one
        val sequencer = sequencer()
        val first = sequencer.play(korobeiniki)
        nextSlice(sink = 0)
        sequencer.play(minuet)
        nextSlice(sink = 1)

        // When the first playback is stopped, as its game is left after the next one started
        first.stop()

        // Then the new one goes on writing, its sink open
        assertThat(nextSlice(sink = 1)).containsExactly(-3f, -1f)
        assertThat(sinks[1].released).isFalse()
    }

    @Test
    fun `stop without play does nothing`() = runTest {
        // Given
        val sequencer = sequencer()

        // When / Then
        assertThatNoException().isThrownBy { sequencer.stop() }
        runCurrent()
        assertThat(sinks).isEmpty()
        assertThat(rendered).isEmpty()
    }

    /** The next slice written to the sink number [sink], once the scheduled work has run. */
    @Test
    fun `the sink opens at the sample rate of the rendering`() = runTest {
        // Given
        val sequencer = sequencer()

        // When
        sequencer.play(korobeiniki)
        nextSlice()

        // Then
        assertThat(sinks.single().sampleRate).isEqualTo(SAMPLE_RATE)
    }

    @Test
    fun `a sink that blocks instead of suspending still stops at the next slice`() = runTest {
        // Given a sink whose writes never suspend, and which stops the sequencer on its third write
        lateinit var sequencer: ChiptuneSequencer
        val sink = BlockingFakeSink(onWrite = { count -> if (count == 3) sequencer.stop() })
        sequencer = sequencer(openSink = { sink })

        // When
        sequencer.play(korobeiniki)
        runCurrent()

        // Then
        assertThat(sink.writes).isEqualTo(3)
        assertThat(sink.released).isTrue()
    }

    @Test
    fun `stop then play opens the new sink only once the old one is released`() = runTest {
        // Given a score playing on a sink whose write does not react to the stop
        val sequencer = sequencer(openSink = gatedSinks::open)
        sequencer.play(korobeiniki)
        runCurrent()

        // When it is stopped, another score played, and only then the blocked write returns
        sequencer.stop()
        sequencer.play(minuet)
        runCurrent()
        gatedSinks.letOneWriteThrough()
        runCurrent()

        // Then
        assertThat(gatedSinks.log).containsExactly("open#0", "release#0", "open#1")
    }

    @Test
    fun `play then play opens the new sink only once the old one is released`() = runTest {
        // Given a score playing on a sink whose write does not react to the stop
        val sequencer = sequencer(openSink = gatedSinks::open)
        sequencer.play(korobeiniki)
        runCurrent()

        // When another score is played, and only then the blocked write returns
        sequencer.play(minuet)
        runCurrent()
        gatedSinks.letOneWriteThrough()
        runCurrent()

        // Then
        assertThat(gatedSinks.log).containsExactly("open#0", "release#0", "open#1")
    }

    @Test
    fun `a sink that cannot open stops the music without a crash`() = runTest {
        // Given
        val sequencer = sequencer(openSink = { throw IllegalStateException("No audio output") })

        // When
        sequencer.play(korobeiniki)
        runCurrent()

        // Then nothing is left playing: the scope is still alive, and a stop does nothing
        assertThat(backgroundScope.isActive).isTrue()
        assertThatNoException().isThrownBy { sequencer.stop() }
    }

    @Test
    fun `a failure that stops the music is reported, to be logged`() = runTest {
        // Given a sequencer whose sink cannot open
        val failure = IllegalStateException("No audio output")
        val reported = mutableListOf<Exception>()
        val sequencer = sequencer(openSink = { throw failure }, onFailure = { reported += it })

        // When
        sequencer.play(korobeiniki)
        runCurrent()

        // Then
        assertThat(reported).containsExactly(failure)
    }

    @Test
    fun `a write that fails releases the sink and stops the music without a crash`() = runTest {
        // Given a sink whose second write fails
        val sink = BlockingFakeSink(onWrite = { count ->
            if (count ==
                2
            ) {
                throw IllegalStateException("AudioTrack error")
            }
        })
        val sequencer = sequencer(openSink = { sink })

        // When
        sequencer.play(korobeiniki)
        runCurrent()

        // Then
        assertThat(sink.writes).isEqualTo(2)
        assertThat(sink.released).isTrue()
        assertThat(backgroundScope.isActive).isTrue()
    }

    @Test
    fun `a stop during the rendering opens no sink`() = runTest {
        // Given a sequencer stopped while it renders
        lateinit var sequencer: ChiptuneSequencer
        sequencer = sequencer(onRender = { sequencer.stop() })

        // When
        sequencer.play(korobeiniki)
        runCurrent()

        // Then
        assertThat(rendered).hasSize(1)
        assertThat(sinks).isEmpty()
    }

    private suspend fun TestScope.nextSlice(sink: Int = 0): FloatArray {
        runCurrent()
        assertThat(sinks).describedAs("open sinks").hasSizeGreaterThan(sink)
        return sinks[sink].written.receive()
    }

    private fun TestScope.sequencer(
        openSink: (Int) -> AudioSink = { sampleRate -> FakeSink(sampleRate).also { sinks += it } },
        onRender: () -> Unit = {},
        onFailure: (Exception) -> Unit = {},
    ): ChiptuneSequencer {
        val renderDispatcher = WatchedDispatcher(StandardTestDispatcher(testScheduler))
        return ChiptuneSequencer(
            render = { score, tempoFactor ->
                onRender()
                buffers.getValue(score).also { rendered += Rendering(score, tempoFactor, renderDispatcher.running) }
            },
            sampleRate = SAMPLE_RATE,
            openSink = openSink,
            scope = backgroundScope,
            renderDispatcher = renderDispatcher,
            writeDispatcher = StandardTestDispatcher(testScheduler),
            sliceSize = 2,
            onFailure = onFailure,
        )
    }

    private fun scoreOf(pitch: Pitch): Score = Score(
        title = pitch.toString(),
        bpm = 150,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.07, notes = listOf(Note(pitch, beats = 4.0)))),
    )

    /** Runs its work on [delegate], and tells whether it is [running] some at the moment. */
    private class WatchedDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
        var running = false
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            delegate.dispatch(context) {
                running = true
                try {
                    block.run()
                } finally {
                    running = false
                }
            }
        }
    }

    /**
     * Sinks whose writes wait for a permit and ignore cancellation while they wait, as a write that
     * cannot be interrupted. The [log] tells when each one opens and is released.
     */
    private class GatedSinks {
        val log = mutableListOf<String>()
        private val permits = Channel<Unit>(Channel.UNLIMITED)
        private var opened = 0

        fun open(@Suppress("UNUSED_PARAMETER") sampleRate: Int): AudioSink {
            val number = opened++
            log += "open#$number"
            return object : AudioSink {
                override suspend fun write(samples: FloatArray) {
                    withContext(NonCancellable) { permits.receive() }
                }

                override fun release() {
                    log += "release#$number"
                }
            }
        }

        fun letOneWriteThrough() {
            permits.trySend(Unit)
        }
    }

    /** A sink like an AudioTrack: its writes block, they never suspend. */
    private class BlockingFakeSink(private val onWrite: (count: Int) -> Unit) : AudioSink {
        var writes = 0
            private set
        var released = false
            private set

        override suspend fun write(samples: FloatArray) {
            writes++
            // An Error, not an Exception: nothing in the sequencer may swallow it.
            if (writes > MAX_WRITES) throw AssertionError("Still writing after $MAX_WRITES slices")
            onWrite(writes)
        }

        override fun release() {
            released = true
        }

        private companion object {
            const val MAX_WRITES = 10
        }
    }

    private data class Rendering(val score: Score, val tempoFactor: Double, val onRenderingDispatcher: Boolean)

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
