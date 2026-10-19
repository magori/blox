package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class ChiptuneSequencerTempoTest {
    /** A score of 4 quarter notes: the tempo may switch on every beat. */
    private val score = scoreOf(1.0, 1.0, 1.0, 1.0)

    /** A score of one whole note: the tempo may switch only at the start of the loop. */
    private val wholeNote = scoreOf(4.0)

    /**
     * What the fake rendering gives at each tempo factor: recognisable values, as long as the tempo
     * makes them. At 1.0, a beat lasts 3 samples (100 to 111); at 2.0, 1.5 samples (200 to 205).
     */
    private val buffers = mapOf(
        1.0 to FloatArray(12) { 100f + it },
        2.0 to FloatArray(6) { 200f + it },
        1.25 to FloatArray(9) { 300f + it },
    )

    /** Each rendering: its tempo factor, and whether it ran on the rendering dispatcher. */
    private val rendered = mutableListOf<Pair<Double, Boolean>>()
    private val sinks = mutableListOf<FakeSink>()
    private val renderDispatcher = ManualDispatcher()

    @ParameterizedTest(name = "a tempo factor of {0} is refused")
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `the tempo factor must be positive and finite`(factor: Double) = runTest {
        // Given
        val sequencer = sequencer()

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { sequencer.setTempoFactor(factor) }
    }

    @Test
    fun `a new play starts at the normal tempo, though the music played before was faster`() = runTest {
        // Given a score sped up to 2.0 (a game in danger), then stopped (the game over)
        val sequencer = sequencer()
        stoppedAtDoubleTempo(sequencer)

        // When a score is played again (the preview of the home screen, a new game)
        sequencer.play(score)

        // Then it plays at the normal tempo, from its start
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
    }

    @Test
    fun `a tempo factor set while nothing plays is not kept for the next play`() = runTest {
        // Given
        val sequencer = sequencer()
        sequencer.setTempoFactor(2.0)

        // When
        sequencer.play(score)
        renderNow()

        // Then
        assertThat(rendered).containsExactly(1.0 to true)
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
    }

    @Test
    fun `a tempo change keeps the same sink, and the writing goes on at the new tempo`() = runTest {
        // Given a score playing at 1.0, asked for 2.0
        val sequencer = playing()
        sequencer.setTempoFactor(2.0)
        nextSlice()

        // When the rendering at 2.0 is ready
        runCurrent()
        renderDispatcher.runAll()

        // Then the next slices come to the same sink, now at the new tempo
        nextSlice()
        assertThat(nextSlice()).anyMatch { it >= 200f }
        assertThat(sinks).hasSize(1)
    }

    @Test
    fun `a new tempo is rendered once on the rendering dispatcher, and going back to 1 point 0 renders nothing`() =
        runTest {
            // Given a score playing at 2.0 after playing at 1.0
            val sequencer = playing()
            sequencer.setTempoFactor(2.0)
            nextSlice()
            runCurrent()
            renderDispatcher.runAll()
            repeat(2) { nextSlice() }

            // When it goes back to 1.0
            sequencer.setTempoFactor(1.0)
            val slices = List(4) { nextSlice() }
            renderDispatcher.runAll()

            // Then it plays at 1.0 again, from the cache
            assertThat(slices.flatten()).anyMatch { it < 200f }
            assertThat(rendered).containsExactly(1.0 to true, 2.0 to true)
        }

    @Test
    fun `the tempo changes at the start of the next beat, once the current beat is played`() = runTest {
        // Given a score playing at 1.0, its rendering at 2.0 ready when the writing is at sample 8, in
        // beat 2 (samples 6 to 8)
        val sequencer = playing()
        sequencer.setTempoFactor(2.0)
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
        runCurrent()
        renderDispatcher.runAll()
        assertThat(nextSlice()).containsExactly(104f, 105f, 106f, 107f)

        // When / Then: beat 2 ends with sample 8, then beat 3 starts at sample 5 of the new buffer
        assertThat(nextSlice()).containsExactly(108f, 205f, 200f, 201f)
    }

    @Test
    fun `a rendering not ready when a beat starts waits for the next beat, the old tempo goes on`() = runTest(
        timeout = 1.seconds,
    ) {
        // Given a score playing at 1.0, asked for 2.0, its rendering not ready for beats 1, 2 and 3
        val sequencer = playing()
        sequencer.setTempoFactor(2.0)
        assertThat(List(3) { nextSlice() }.flatten()).isEqualTo(List(12) { 100f + it })

        // When the rendering is ready, the writing at sample 0: the start of beat 0
        renderDispatcher.runAll()

        // Then the new tempo starts there, rendered once, though it was waited for over three slices
        assertThat(nextSlice()).containsExactly(200f, 201f, 202f, 203f)
        assertThat(rendered).containsExactly(1.0 to true, 2.0 to true)
    }

    @Test
    fun `after the change the samples written are exactly those of the buffer at the new tempo`() = runTest {
        // Given a score switched from 1.0 to 2.0 at the start of beat 3
        val sequencer = playing()
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()
        renderDispatcher.runAll()
        nextSlice()

        // When
        val written = List(3) { nextSlice() }.flatten()

        // Then: the end of beat 2 at 1.0, then the buffer at 2.0 as it was rendered, round the loop
        assertThat(written).containsExactly(108f, 205f, 200f, 201f, 202f, 203f, 204f, 205f, 200f, 201f, 202f, 203f)
    }

    @Test
    fun `of two quick tempo changes only the last one is applied`() = runTest {
        // Given a score playing at 1.0, asked for 1.25, its rendering started
        val sequencer = playing()
        sequencer.setTempoFactor(1.25)
        nextSlice()
        runCurrent()

        // When it is asked for 1.0 again before the next beat, then the rendering at 1.25 could run
        sequencer.setTempoFactor(1.0)
        val slices = List(4) { nextSlice() }
        renderDispatcher.runAll()
        val later = List(3) { nextSlice() }

        // Then the music stays at 1.0, and the rendering at 1.25, no longer wanted, never ran
        assertThat((slices + later).flatten()).allMatch { it in 100f..111f }
        assertThat(rendered).containsExactly(1.0 to true)
    }

    @Test
    fun `of two quick tempo changes to new tempos only the last one is rendered and played`() = runTest {
        // Given a score playing at 1.0, asked for 1.25, its rendering started
        val sequencer = playing()
        sequencer.setTempoFactor(1.25)
        nextSlice()
        runCurrent()

        // When it is asked for 2.0 before the rendering at 1.25 could run, then the renderings could run
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()
        renderDispatcher.runAll()
        val later = List(4) { nextSlice() }.flatten()

        // Then 2.0 plays, and 1.25 was never rendered nor played
        assertThat(later).anyMatch { it in 200f..205f }
        assertThat(later).noneMatch { it in 300f..308f }
        assertThat(rendered).containsExactly(1.0 to true, 2.0 to true)
    }

    @Test
    fun `a stop while a new tempo renders leaves no sink open and throws nothing`() = runTest {
        // Given a score playing at 1.0, its rendering at 2.0 under way
        val sequencer = playing()
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()

        // When it is stopped, then the rendering would have run
        sequencer.stop()
        runCurrent()
        renderDispatcher.runAll()
        runCurrent()

        // Then
        assertThat(sinks.single().released).isTrue()
        assertThat(backgroundScope.isActive).isTrue()
        assertThat(rendered).containsExactly(1.0 to true)
    }

    @Test
    fun `a note across the next beat delays the change to the next beat where every voice starts a note`() = runTest {
        // Given a whole note playing at 1.0, its rendering at 2.0 ready in beat 2
        val sequencer = playing(wholeNote)
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()
        renderDispatcher.runAll()
        nextSlice()

        // When / Then: the whole note is not cut on beat 3, the change waits for the start of the loop
        assertThat(nextSlice()).containsExactly(108f, 109f, 110f, 111f)
        assertThat(nextSlice()).containsExactly(200f, 201f, 202f, 203f)
    }

    @Test
    fun `when the next beat is the end of the buffer, the new tempo starts from its first sample`() =
        runTest(timeout = 1.seconds) {
            // Given a score playing at 1.0 in slices of 5, asked for 2.0, its rendering ready at sample 10
            val sequencer = playing(sliceSize = 5)
            sequencer.setTempoFactor(2.0)
            assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f, 104f)
            assertThat(nextSlice()).containsExactly(105f, 106f, 107f, 108f, 109f)
            renderDispatcher.runAll()

            // When / Then: the loop ends after sample 11, the new tempo starts from the start of its buffer
            assertThat(nextSlice()).containsExactly(110f, 111f, 200f, 201f, 202f)
        }

    @Test
    fun `play after stop does not wait for a tempo rendering under way`() = runTest(timeout = 2.seconds) {
        // Given a rendering at 2.0 that blocks a real thread until the test lets it go
        val renderThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val blocked = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        try {
            val sequencer = sequencer(
                renderOn = renderThread,
                render = { factor ->
                    if (factor == 2.0) {
                        blocked.complete(Unit)
                        release.await()
                    }
                    buffers.getValue(factor)
                },
            )
            sequencer.play(score)
            awaitSinks(count = 1)
            nextSlice()
            sequencer.setTempoFactor(2.0)
            nextSlice()
            blocked.await()

            // When the music stops, then plays again at 1.0 while that rendering is still blocked
            sequencer.stop()
            sequencer.setTempoFactor(1.0)
            sequencer.play(score)
            runCurrent()

            // Then the new loop writes at once, from the cache
            assertThat(sinks).hasSize(2)
            assertThat(sinks.last().written.receive().toList()).containsExactly(100f, 101f, 102f, 103f)
        } finally {
            release.countDown()
            renderThread.close()
        }
    }

    @Test
    fun `a tempo rendering that fails leaves the music playing at the old tempo`() = runTest(timeout = 1.seconds) {
        // Given a score playing at 1.0, whose rendering at 2.0 fails
        val sequencer = sequencer(
            render = { factor ->
                check(factor != 2.0) { "rendering failed" }
                buffers.getValue(factor)
            },
        )
        sequencer.play(score)
        renderNow()
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()

        // When the rendering at 2.0 runs, and fails
        renderDispatcher.runAll()

        // Then the music goes on at 1.0, on the same sink
        assertThat(List(4) { nextSlice() }.flatten()).allMatch { it in 100f..111f }
        assertThat(sinks.single().released).isFalse()
    }

    @Test
    fun `a stopped playback resumes from the start of the beat where it stopped`() = runTest {
        // Given a score stopped at sample 8, in beat 2 (samples 6 to 8)
        val sequencer = sequencer()
        val playback = sequencer.play(score)
        renderNow()
        nextSlice()
        nextSlice()
        playback.stop()
        runCurrent()

        // When
        playback.resume()

        // Then it goes on from the start of beat 2, so that no note is cut
        assertThat(nextSlice()).containsExactly(106f, 107f, 108f, 109f)
    }

    @Test
    fun `a playback resumed then stopped before it wrote anything resumes from the same place`() = runTest {
        // Given a score stopped at sample 8, in beat 2 (samples 6 to 8), resumed and stopped at once
        val sequencer = sequencer()
        val playback = sequencer.play(score)
        renderNow()
        nextSlice()
        nextSlice()
        playback.stop()
        runCurrent()
        val second = playback.resume()
        second.stop()

        // When
        second.resume()

        // Then it still goes on from the start of beat 2
        assertThat(nextSlice()).containsExactly(106f, 107f, 108f, 109f)
    }

    @Test
    fun `a whole note stopped in its middle resumes from the start of the loop`() = runTest {
        // Given a whole note stopped at sample 8 of 12: no beat but the first where no note is cut
        val sequencer = sequencer()
        val playback = sequencer.play(wholeNote)
        renderNow()
        nextSlice()
        nextSlice()
        playback.stop()
        runCurrent()

        // When
        playback.resume()

        // Then
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
    }

    @Test
    fun `playing the score again, as a new game does, starts from its start`() = runTest {
        // Given a score stopped at sample 8
        val sequencer = sequencer()
        val playback = sequencer.play(score)
        renderNow()
        nextSlice()
        nextSlice()
        playback.stop()
        runCurrent()

        // When it is played, not resumed
        sequencer.play(score)

        // Then
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
    }

    @Test
    fun `a playback stopped at the danger tempo resumes at that tempo, where it stood`() = runTest {
        // Given a score switched to 2.0 on beat 3, stopped at sample 2 of the buffer at 2.0 (beat 1)
        val sequencer = sequencer()
        val playback = stoppedAtDoubleTempo(sequencer)

        // When
        playback.resume()

        // Then it goes on at 2.0, from the start of beat 1 (sample 2)
        assertThat(nextSlice()).containsExactly(202f, 203f, 204f, 205f)
    }

    @Test
    fun `a playback resumed at another tempo resumes at the same place in the score`() = runTest {
        // Given a score stopped at 1.0 at sample 8 of 12, in beat 2; then the tempo asked for is 2.0
        val sequencer = sequencer()
        val playback = sequencer.play(score)
        renderNow()
        nextSlice()
        nextSlice()
        playback.stop()
        runCurrent()
        sequencer.setTempoFactor(2.0)

        // When
        playback.resume()
        renderNow()

        // Then sample 8 of 12 is sample 4 of 6, in beat 2, which starts at sample 3
        assertThat(nextSlice()).containsExactly(203f, 204f, 205f, 200f)
    }

    @Test
    fun `a place that falls on the end of the buffer at the new tempo resumes from its start`() = runTest {
        // Given a score stopped at 1.0 at sample 11 of 12, then asked for 2.0: 11 of 12 is 5.5 of 6, rounded to 6
        val sequencer = sequencer(sliceSize = 11)
        val playback = sequencer.play(score)
        renderNow()
        nextSlice()
        playback.stop()
        runCurrent()
        sequencer.setTempoFactor(2.0)

        // When
        playback.resume()
        renderNow()

        // Then it is brought back into the buffer: the start of the loop
        assertThat(nextSlice()).startsWith(200f, 201f, 202f)
    }

    /**
     * [score] played by [sequencer] at 1.0, switched to 2.0 on beat 3 (sample 9 of 12), then stopped at
     * sample 2 of the buffer at 2.0, in beat 1, as a game in danger then over or paused; its playback.
     */
    private suspend fun TestScope.stoppedAtDoubleTempo(sequencer: ChiptuneSequencer): Playback {
        val playback = sequencer.play(score)
        renderNow()
        sequencer.setTempoFactor(2.0)
        nextSlice()
        runCurrent()
        renderDispatcher.runAll()
        nextSlice()
        assertThat(nextSlice()).containsExactly(108f, 205f, 200f, 201f)
        playback.stop()
        runCurrent()
        return playback
    }

    /** A sequencer playing [played] at 1.0, its buffer rendered, before its first write is received. */
    private fun TestScope.playing(
        played: Score = score,
        sliceSize: Int = 4,
    ): ChiptuneSequencer = sequencer(sliceSize).also {
        it.play(played)
        renderNow()
    }

    private fun scoreOf(vararg beats: Double): Score = Score(
        // A synthetic score: its identifier plays no part here.
        id = ScoreId.KOROBEINIKI,
        bpm = 150,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.07, notes = beats.map { Note(Pitch.parse("E5"), it) })),
    )

    /** Lets the waiting work run, then the renderings waiting for the rendering dispatcher, then their results. */
    private fun TestScope.renderNow() {
        runCurrent()
        renderDispatcher.runAll()
        runCurrent()
    }

    /** Lets the test scheduler run until [count] sinks are open, for renderings on a real thread. */
    private suspend fun TestScope.awaitSinks(count: Int) {
        while (sinks.size < count) delay(1)
        runCurrent()
    }

    /** The next slice written to the last sink opened. */
    private suspend fun TestScope.nextSlice(): List<Float> {
        runCurrent()
        return sinks.last().written.receive().toList()
    }

    /**
     * A sequencer whose renderings run on [renderOn] and give what [render] gives for a tempo factor:
     * by default, the [buffers], noting each rendering in [rendered].
     */
    private fun TestScope.sequencer(
        sliceSize: Int = 4,
        renderOn: CoroutineDispatcher = renderDispatcher,
        render: (tempoFactor: Double) -> FloatArray = { tempoFactor ->
            buffers.getValue(tempoFactor).also { rendered += tempoFactor to renderDispatcher.running }
        },
    ): ChiptuneSequencer = ChiptuneSequencer(
        render = { _, tempoFactor -> render(tempoFactor) },
        sampleRate = 22_050,
        openSink = { sampleRate -> FakeSink(sampleRate).also { sinks += it } },
        scope = backgroundScope,
        renderDispatcher = renderOn,
        writeDispatcher = StandardTestDispatcher(testScheduler),
        sliceSize = sliceSize,
    )

    /** Runs its work only when the test says so: a rendering not ready yet is a task left in it. */
    private class ManualDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        var running = false
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            tasks.addLast(block)
        }

        /** Runs the work waiting, renderings included. */
        fun runAll() {
            while (tasks.isNotEmpty()) {
                running = true
                try {
                    tasks.removeFirst().run()
                } finally {
                    running = false
                }
            }
        }
    }
}
