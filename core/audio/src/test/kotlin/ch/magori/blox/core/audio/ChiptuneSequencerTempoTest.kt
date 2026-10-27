package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
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
        1.02 to FloatArray(12) { 400f + it },
        1.04 to FloatArray(12) { 500f + it },
        1.06 to FloatArray(12) { 600f + it },
    )

    /** Each rendering: its tempo factor, and whether it ran on the rendering dispatcher. */
    private val rendered = mutableListOf<Pair<Double, Boolean>>()
    private val sinks = mutableListOf<FakeSink>()
    private val renderDispatcher = ManualDispatcher()

    @Test
    fun `a new play after a faster music starts from the start of the score at the normal tempo`() = runTest {
        // Given a score sped up to 2.0 (a game in danger), then stopped (the game over)
        val sequencer = sequencer()
        stoppedAtDoubleTempo(sequencer)

        // When a score is played again (the preview of the home screen, a new game)
        sequencer.play(score)

        // Then it plays at the normal tempo, from its start
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
    }

    @Test
    fun `a play after a tempo factor set while nothing plays renders only the normal tempo`() = runTest {
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
    fun `the danger tempo prepared is rendered as soon as the score plays, without holding back its first slice`() =
        runTest {
            // Given
            val sequencer = sequencer()

            // When a score plays, its danger tempo prepared, as a game does
            sequencer.play(score)
            sequencer.prepareTempo(1.25)
            renderNow()

            // Then it plays at once at its tempo, and the danger tempo is rendered meanwhile, before any danger
            assertThat(nextSlice()).containsExactly(100f, 101f, 102f, 103f)
            renderDispatcher.runAll()
            assertThat(rendered).containsExactly(1.0 to true, 1.25 to true)
        }

    @Test
    fun `a new play after a game renders only the normal tempo, as a preview prepares no other tempo`() = runTest {
        // Given a score played by a game, its danger tempo prepared, then stopped
        val sequencer = sequencer()
        sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()
        renderDispatcher.runAll()
        sequencer.stop()
        runCurrent()
        rendered.clear()

        // When another score plays, as the preview of the home screen
        sequencer.play(wholeNote)
        renderNow()
        nextSlice()
        renderDispatcher.runAll()

        // Then only its normal tempo is rendered
        assertThat(rendered).containsExactly(1.0 to true)
    }

    @Test
    fun `a pause during the preparation of the danger tempo does not render it twice`() = runTest {
        // Given a score playing, its danger tempo prepared, and the game paused while that rendering runs
        lateinit var sequencer: ChiptuneSequencer
        var paused = false
        sequencer = sequencer(render = { factor ->
            if (factor == 1.25 && !paused) {
                paused = true
                sequencer.stop()
            }
            buffers.getValue(factor).also { rendered += factor to renderDispatcher.running }
        })
        val playback = sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()
        renderDispatcher.runAll()
        runCurrent()

        // When the game resumes
        playback.resume()
        renderNow()
        nextSlice()
        renderDispatcher.runAll()

        // Then the danger tempo rendered during the pause is kept, not rendered again
        assertThat(rendered).containsExactly(1.0 to true, 1.25 to true)
    }

    @Test
    fun `playing another score abandons the preparations of the one before`() = runTest {
        // Given a score playing, its danger tempo being prepared
        val sequencer = sequencer()
        sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()

        // When another score plays before that rendering could run
        sequencer.play(wholeNote)
        renderNow()
        renderDispatcher.runAll()

        // Then the danger tempo of the first score is never rendered
        assertThat(rendered).containsExactly(1.0 to true, 1.0 to true)
    }

    @Test
    fun `the renderings under way end with the scope of the sequencer`() = runTest {
        // Given a sequencer living in its own scope, a score playing, its danger tempo being prepared
        val sequencerScope = CoroutineScope(Job())
        val sequencer = sequencer(scope = sequencerScope)
        sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()

        // When that scope ends, then the rendering could run
        sequencerScope.cancel()
        runCurrent()
        renderDispatcher.runAll()

        // Then the danger tempo is never rendered
        assertThat(rendered).containsExactly(1.0 to true)
    }

    @Test
    fun `the danger tempo prepared plays from the next beat, without being rendered again`() = runTest {
        // Given a score playing, its danger tempo prepared
        val sequencer = sequencer()
        sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()
        renderDispatcher.runAll()

        // When the stack enters the danger, the first slice written already
        sequencer.setTempoFactor(1.25)
        val slices = List(3) { nextSlice() }

        // Then the danger tempo plays from the next beat, beat 2 (sample 6 at 1.0, sample 5 at 1.25), with
        // no rendering left to run
        assertThat(slices).containsExactly(
            listOf(100f, 101f, 102f, 103f),
            listOf(104f, 105f, 305f, 306f),
            listOf(307f, 308f, 300f, 301f),
        )
        assertThat(rendered).containsExactly(1.0 to true, 1.25 to true)
    }

    @Test
    fun `leaving the danger before its tempo is prepared keeps the preparation for the next danger`() = runTest {
        // Given a score playing, its danger tempo still being prepared, asked for then given up
        val sequencer = sequencer()
        sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()
        sequencer.setTempoFactor(1.25)
        nextSlice()
        runCurrent()
        sequencer.setTempoFactor(1.0)
        nextSlice()
        runCurrent()
        renderDispatcher.runAll()

        // When the stack enters the danger again, the slice of samples 8 to 11 written already
        sequencer.setTempoFactor(1.25)
        val slices = List(3) { nextSlice() }

        // Then the danger tempo plays from the next beat, the start of the loop, rendered only once
        assertThat(slices).containsExactly(
            listOf(108f, 109f, 110f, 111f),
            listOf(300f, 301f, 302f, 303f),
            listOf(304f, 305f, 306f, 307f),
        )
        assertThat(rendered).containsExactly(1.0 to true, 1.25 to true)
    }

    @Test
    fun `a danger tempo prepared stays in the cache when the score is resumed`() = runTest {
        // Given a score that played a while, its danger tempo prepared, then stopped (a pause)
        val sequencer = sequencer()
        val playback = sequencer.play(score)
        sequencer.prepareTempo(1.25)
        renderNow()
        renderDispatcher.runAll()
        repeat(2) { nextSlice() }
        playback.stop()
        runCurrent()

        // When it is resumed
        playback.resume()
        renderNow()
        nextSlice()

        // Then nothing is rendered again
        assertThat(rendered).containsExactly(1.0 to true, 1.25 to true)
    }

    @Test
    fun `a playback resumed at the danger tempo, once another score emptied the cache, prepares the normal tempo`() =
        runTest {
            // Given a score stopped, another one played since, then the danger tempo asked for
            val sequencer = sequencer()
            val playback = sequencer.play(score)
            renderNow()
            nextSlice()
            playback.stop()
            sequencer.play(wholeNote)
            renderNow()
            renderDispatcher.runAll()
            nextSlice()
            sequencer.stop()
            runCurrent()
            sequencer.setTempoFactor(1.25)
            rendered.clear()

            // When the first score is resumed
            playback.resume()
            renderNow()
            nextSlice()
            renderDispatcher.runAll()

            // Then it plays at the danger tempo, and its normal tempo is rendered meanwhile, for the end of the danger
            assertThat(rendered).containsExactly(1.25 to true, 1.0 to true)
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
    fun `the buffers of a tempo left are released`() = runTest {
        // Given a score playing at 1.0, its danger tempo prepared, as a live game does
        val sequencer = playing()
        sequencer.prepareTempo(1.25)

        // When it goes up to 1.02, then 1.04, then 1.06, as the levels of a live game, then back to 1.0, to the
        // danger tempo, and to 1.04
        listOf(1.02, 1.04, 1.06, 1.0, 1.25, 1.04).forEach { playAt(sequencer, it) }

        // Then only the tempo playing, 1.0 and the tempo prepared stayed in the cache: 1.04, left, is rendered again
        assertThat(rendered.map { it.first }).containsExactly(1.0, 1.25, 1.02, 1.04, 1.06, 1.04)
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

    /**
     * Asks [sequencer] for the tempo [factor], then lets it render and write until it plays at it: a slice that ends
     * with a sample of the buffer at [factor].
     */
    private suspend fun TestScope.playAt(
        sequencer: ChiptuneSequencer,
        factor: Double,
    ) {
        sequencer.setTempoFactor(factor)
        val samples = buffers.getValue(factor).toSet()
        repeat(MAX_SLICES_TO_A_TEMPO) {
            val slice = nextSlice()
            renderNow()
            if (slice.last() in samples) return
        }
        throw AssertionError("The tempo $factor never played")
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
        scope: CoroutineScope = backgroundScope,
        renderOn: CoroutineDispatcher = renderDispatcher,
        render: (tempoFactor: Double) -> FloatArray = { tempoFactor ->
            buffers.getValue(tempoFactor).also { rendered += tempoFactor to renderDispatcher.running }
        },
    ): ChiptuneSequencer = ChiptuneSequencer(
        render = { _, tempoFactor -> render(tempoFactor) },
        sampleRate = 22_050,
        openSink = { sampleRate -> FakeSink(sampleRate).also { sinks += it } },
        scope = scope,
        renderDispatcher = renderOn,
        writeDispatcher = StandardTestDispatcher(testScheduler),
        sliceSize = sliceSize,
    )

    private companion object {
        /** Enough slices of 4 samples for a tempo asked for to be rendered and reach the next beat. */
        const val MAX_SLICES_TO_A_TEMPO = 8
    }
}
