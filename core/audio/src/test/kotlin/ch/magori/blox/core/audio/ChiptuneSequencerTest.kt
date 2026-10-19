package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
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
import kotlin.math.roundToInt

/** The contract of the players, and what is proper to the sequencer: its renderings, its sinks, its failures. */
class ChiptuneSequencerTest : MusicPlayerContract() {
    override fun playerUnderTest(scope: TestScope): PlayerUnderTest = ListenedSequencer(scope)

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
    fun `only the score playing stays in the cache`() = runTest {
        // Given a score played, then another one
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)
        sequencer.play(minuet)
        nextSlice(sink = 1)

        // When the first one is played again
        sequencer.play(korobeiniki)

        // Then it is rendered again: its buffer left the cache when the other score played
        assertThat(nextSlice(sink = 2)).containsExactly(1f, 2f)
        assertThat(rendered.map { it.score }).containsExactly(korobeiniki, minuet, korobeiniki)
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
    fun `the tempo prepared for a new score is never rendered for the score it replaces`() = runTest {
        // Given a score playing
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When another one plays in its place, its danger tempo prepared at once, as a new game does
        sequencer.play(minuet)
        sequencer.prepareTempo(1.25)
        runCurrent()

        // Then the score replaced is not rendered at that tempo: its loop never saw it
        assertThat(rendered).noneMatch { it.score == korobeiniki && it.tempoFactor == 1.25 }
        assertThat(rendered).contains(Rendering(minuet, tempoFactor = 1.25, onRenderingDispatcher = true))
    }

    @Test
    fun `a play stopped before it starts still stops the score playing before it`() = runTest {
        // Given a score playing
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When another one is played, then stopped before its loop could start
        sequencer.play(minuet)
        sequencer.stop()
        runCurrent()

        // Then the first one is stopped too: no loop is left playing
        assertThat(sinks[0].released).isTrue()
        assertThat(sinks).hasSize(1)
    }

    @Test
    fun `a play after a play stopped before it starts waits for the first sink to be released`() = runTest {
        // Given a score playing on a sink whose write does not react to the stop, then another one
        // played and stopped before its loop could start
        val sequencer = sequencer(openSink = gatedSinks::open)
        sequencer.play(korobeiniki)
        runCurrent()
        sequencer.play(minuet)
        sequencer.stop()

        // When a third play comes, and only then the blocked write returns
        sequencer.play(korobeiniki)
        runCurrent()
        gatedSinks.letOneWriteThrough()
        runCurrent()

        // Then the new sink opens only once the first one is released
        assertThat(gatedSinks.log).containsExactly("open#0", "release#0", "open#1")
    }

    @Test
    fun `a play of a cached score stopped before it starts opens no sink`() = runTest {
        // Given a score playing, its buffer in the cache
        val sequencer = sequencer()
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When it is played again, then stopped before its loop could start
        sequencer.play(korobeiniki)
        sequencer.stop()
        runCurrent()

        // Then no new sink opens, though nothing was left to render
        assertThat(sinks).hasSize(1)
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
    fun `a sink that dies once is reopened and the music goes on where it stood`() = runTest {
        // Given a sink that dies on its second write, the slice 3, 4; each opening notes whether every
        // sink opened before is released
        val othersReleasedOnOpen = mutableListOf<Boolean>()
        val sequencer = sequencer(openSink = { sampleRate ->
            othersReleasedOnOpen += sinks.all { it.released }
            FakeSink(sampleRate, diesOnWrite = if (sinks.isEmpty()) 2 else null).also { sinks += it }
        })
        sequencer.play(korobeiniki)
        assertThat(nextSlice(sink = 0)).containsExactly(1f, 2f)

        // When it dies, then the music goes on
        val next = List(2) { nextSlice(sink = 1).toList() }

        // Then the dead sink is released before a new one opens at the same rate, and the slice lost is
        // written to the new one
        assertThat(othersReleasedOnOpen).containsExactly(true, true)
        assertThat(sinks[1].sampleRate).isEqualTo(SAMPLE_RATE)
        assertThat(next).containsExactly(listOf(3f, 4f), listOf(5f, 1f))
    }

    @Test
    fun `a sink that keeps dying stops the music, reported`() = runTest {
        // Given sinks that all die on their second write, after one slice played
        val reported = mutableListOf<Exception>()
        val sequencer = sequencer(
            openSink = { sampleRate -> FakeSink(sampleRate, diesOnWrite = 2).also { sinks += it } },
            onFailure = { reported += it },
        )
        sequencer.play(korobeiniki)

        // When each one plays its slice, then dies
        repeat(4) { nextSlice(sink = it) }
        runCurrent()

        // Then after three reopenings the music stops, every sink released, the failure reported
        assertThat(sinks).hasSize(4)
        assertThat(sinks).allMatch { it.released }
        assertThat(reported.single()).isInstanceOf(DeadSinkException::class.java)
    }

    @Test
    fun `a sink that dies while the music stops is not reopened`() = runTest {
        // Given a sink whose write waits for a permit, ignoring the stop, then finds the output dead
        val permits = Channel<Unit>(Channel.UNLIMITED)
        var opened = 0
        val sequencer = sequencer(openSink = {
            opened++
            object : AudioSink {
                override suspend fun write(samples: FloatArray) {
                    withContext(NonCancellable) { permits.receive() }
                    throw DeadSinkException("The audio output died")
                }

                override fun release() {}
            }
        })
        sequencer.play(korobeiniki)
        runCurrent()

        // When the music stops during that write, then the write returns
        sequencer.stop()
        permits.trySend(Unit)
        runCurrent()

        // Then no other sink opens
        assertThat(opened).isEqualTo(1)
        permits.trySend(Unit)
    }

    @Test
    fun `a dead sink whose release fails is released only once, and the music stops, reported`() = runTest {
        // Given a sink that dies on its second write, and whose release throws
        val reported = mutableListOf<Exception>()
        val sequencer = sequencer(
            openSink = { sampleRate ->
                FakeSink(sampleRate, diesOnWrite = 2, releaseFails = true).also { sinks += it }
            },
            onFailure = { reported += it },
        )
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When it dies
        runCurrent()

        // Then it is released once, not again when the loop ends, and no other sink opens
        assertThat(sinks.single().releases).isEqualTo(1)
        assertThat(reported).hasSize(1)
    }

    @Test
    fun `a sink reopened that dies at once stops the music, reported, without a crash`() = runTest {
        // Given a sink that dies on its second write, and every sink opened after it dies on its first one
        val reported = mutableListOf<Exception>()
        val sequencer = sequencer(
            openSink = { sampleRate ->
                FakeSink(sampleRate, diesOnWrite = if (sinks.isEmpty()) 2 else 1).also { sinks += it }
            },
            onFailure = { reported += it },
        )
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When the first sink dies, then the one reopened
        runCurrent()

        // Then the music stops: two sinks only, both released, and the failure reported
        assertThat(sinks).hasSize(2)
        assertThat(sinks).allMatch { it.released }
        assertThat(reported.single()).isInstanceOf(DeadSinkException::class.java)
        assertThat(backgroundScope.isActive).isTrue()
    }

    @Test
    fun `a sink that dies again after playing a while is reopened again`() = runTest {
        // Given a sink that dies on its second write, and the one reopened on its third, as two Bluetooth changes
        val sequencer = sequencer(openSink = { sampleRate ->
            FakeSink(sampleRate, diesOnWrite = if (sinks.isEmpty()) 2 else 3).also { sinks += it }
        })
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)
        List(2) { nextSlice(sink = 1) }

        // When the second sink dies too
        val next = nextSlice(sink = 2)

        // Then a third one goes on where the music stood
        assertThat(next).containsExactly(2f, 3f)
        assertThat(sinks[1].released).isTrue()
    }

    @Test
    fun `a sink that dies and cannot be reopened stops the music, reported, released once`() = runTest {
        // Given a sink that dies on its second write, when no audio output can open any more
        val failure = IllegalStateException("No audio output")
        val reported = mutableListOf<Exception>()
        val sequencer = sequencer(
            openSink = { sampleRate ->
                if (sinks.isNotEmpty()) throw failure
                FakeSink(sampleRate, diesOnWrite = 2).also { sinks += it }
            },
            onFailure = { reported += it },
        )
        sequencer.play(korobeiniki)
        nextSlice(sink = 0)

        // When it dies
        runCurrent()

        // Then the music stops, the failure to reopen reported, and the dead sink released only once
        assertThat(reported).containsExactly(failure)
        assertThat(sinks.single().releases).isEqualTo(1)
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
        // A synthetic score: its identifier plays no part here, the scores differ by their notes.
        id = ScoreId.KOROBEINIKI,
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

    /**
     * A sequencer whose renderings give, in every sample, a code of their score and tempo factor: what its
     * only open sink is given tells what is heard.
     */
    private class ListenedSequencer(private val test: TestScope) : PlayerUnderTest {
        private val sinks = mutableListOf<FakeSink>()
        private val rendered = mutableListOf<Pair<ScoreId, Double>>()

        override val player = ChiptuneSequencer(
            render = { score, tempoFactor ->
                rendered += score.id to tempoFactor
                FloatArray(BUFFER_SIZE) { codeOf(score.id, tempoFactor) }
            },
            sampleRate = SAMPLE_RATE,
            openSink = { sampleRate -> FakeSink(sampleRate).also { sinks += it } },
            scope = test.backgroundScope,
            renderDispatcher = StandardTestDispatcher(test.testScheduler),
            writeDispatcher = StandardTestDispatcher(test.testScheduler),
            sliceSize = 2,
        )

        /**
         * The last sample of a few slices written to the only open sink, enough for a tempo asked for to be
         * rendered and switched to; null when no sink is open.
         */
        override suspend fun heard(): Heard? {
            val code = listen()?.roundToInt() ?: return null
            return Heard(ScoreId.entries[code / SCORE_CODE - 1], tempo = (code % SCORE_CODE) / 100.0)
        }

        /**
         * The tempo factors other than 1.0 rendered at least once for [score]: a tempo prepared is rendered
         * by the loop between two slices, so a few slices are listened to first, if a sink is open.
         */
        override fun prepared(score: ScoreId): Set<Double> {
            listen()
            return rendered.filter { (scoreId, tempoFactor) -> scoreId == score && tempoFactor != 1.0 }
                .map { (_, tempoFactor) -> tempoFactor }
                .toSet()
        }

        /** The last sample of a few slices written to the only open sink, once the work due has run; null if none. */
        private fun listen(): Float? {
            test.runCurrent()
            val open = sinks.filterNot { it.released }
            if (open.isEmpty()) return null
            assertThat(open).describedAs("open sinks").hasSize(1)
            var last = 0f
            repeat(SLICES_HEARD) {
                test.runCurrent()
                last = open.single().written.tryReceive().getOrThrow().last()
            }
            return last
        }

        private companion object {
            /** Four beats of two samples, a slice each: a new tempo plays from the next slice. */
            const val BUFFER_SIZE = 8
            const val SLICES_HEARD = 6
            const val SCORE_CODE = 1_000

            fun codeOf(
                score: ScoreId,
                tempoFactor: Double,
            ): Float = ((score.ordinal + 1) * SCORE_CODE + (tempoFactor * 100).roundToInt()).toFloat()
        }
    }

    private data class Rendering(val score: Score, val tempoFactor: Double, val onRenderingDispatcher: Boolean)

    private companion object {
        const val SAMPLE_RATE = 22_050
    }
}
