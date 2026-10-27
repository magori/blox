package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** How the sequencer goes on with another score ([ChiptuneSequencer.switchTo]): where, when, at which tempo. */
class ChiptuneSequencerSwitchTest {
    /** A calm score of 3 bars of 2 quarter notes. */
    private val calm = scoreOf(ScoreId.KOROBEINIKI, Pitch.parse("E5"))

    /** A tense score of 3 bars of 2 quarter notes. */
    private val tense = scoreOf(ScoreId.MINUET, Pitch.parse("A4"))

    /** A third score of 3 bars of 2 quarter notes. */
    private val third = scoreOf(MusicPlayerContract.THIRD, Pitch.parse("C5"))

    /** A calm score of 4 bars of 2 quarter notes. */
    private val longCalm = scoreOf(LONG_CALM, Pitch.parse("E5"), bars = 4)

    /** A tense score of 2 bars of 2 quarter notes. */
    private val shortTense = scoreOf(SHORT_TENSE, Pitch.parse("A4"), bars = 2)

    /**
     * What the fake rendering gives for a score and a tempo factor: recognisable values. The calm score at 1.0
     * lasts 12 samples, a bar of 4 (100 to 111); the tense one at 1.0 lasts 18 samples, a bar of 6 (400 to 417);
     * the third one at 1.0, 12 samples (700 to 711). At 1.25, the calm score lasts 9 samples (300 to 308), the tense
     * one 12 (500 to 511). The calm score of 4 bars lasts 16 samples at 1.0, a bar of 4 (200 to 215); the tense one of 2
     * bars, 8 samples, a bar of 4 (600 to 607).
     */
    private val buffers = mapOf(
        (ScoreId.KOROBEINIKI to 1.0) to FloatArray(12) { 100f + it },
        (ScoreId.KOROBEINIKI to 1.25) to FloatArray(9) { 300f + it },
        (ScoreId.MINUET to 1.0) to FloatArray(18) { 400f + it },
        (ScoreId.MINUET to 1.25) to FloatArray(12) { 500f + it },
        (MusicPlayerContract.THIRD to 1.0) to FloatArray(12) { 700f + it },
        (LONG_CALM to 1.0) to FloatArray(16) { 200f + it },
        (SHORT_TENSE to 1.0) to FloatArray(8) { 600f + it },
    )

    /** Each rendering: its score and its tempo factor. */
    private val rendered = mutableListOf<Pair<ScoreId, Double>>()
    private val sinks = mutableListOf<FakeSink>()
    private val renderDispatcher = ManualDispatcher()

    @Test
    fun `a score switched to starts on the next bar, at the same rank`() = runTest {
        // Given the calm score playing, its first slice written
        val sequencer = playing(calm)

        // When the tense one is switched to, ready once the writing is in bar 1 (samples 4 to 7)
        sequencer.switchTo(tense)
        assertThat(nextSlice()).containsExactly(100f, 101f, 102f)
        renderNow()
        assertThat(nextSlice()).containsExactly(103f, 104f, 105f)

        // Then bar 1 of the calm score ends with sample 7, then bar 2 of the tense one starts, at sample 12
        assertThat(nextSlice()).containsExactly(106f, 107f, 412f)
        assertThat(nextSlice()).containsExactly(413f, 414f, 415f)
    }

    @Test
    fun `a score switched to past its length starts on the bar of the same rank modulo its length`() = runTest {
        // Given the calm score of 4 bars playing, the tense one of 2 bars switched to, ready once the writing is in
        // bar 2 (samples 8 to 11)
        val sequencer = playing(longCalm)
        nextSlice()
        nextSlice()
        sequencer.switchTo(shortTense)
        assertThat(nextSlice()).containsExactly(206f, 207f, 208f)
        renderNow()

        // When bar 2 ends
        assertThat(nextSlice()).containsExactly(209f, 210f, 211f)

        // Then the tense score of 2 bars starts on its bar 1, the rank 3 modulo 2, at sample 4
        assertThat(nextSlice()).containsExactly(604f, 605f, 606f)
    }

    @Test
    fun `a switch while stopped resumes on the first beat of the bar of the same rank`() = runTest {
        // Given the calm score of 4 bars stopped in its bar 2 (samples 8 to 11), as a game paused
        val sequencer = sequencer()
        val playback = sequencer.play(longCalm)
        renderNow()
        repeat(3) { nextSlice() }
        playback.stop()
        runCurrent()

        // When the tense score of 2 bars is switched to, then the game resumes
        sequencer.switchTo(shortTense)
        playback.resume()
        renderNow()

        // Then the tense score starts on its bar 0, the rank 2 modulo 2, at sample 0
        assertThat(nextSlice()).containsExactly(600f, 601f, 602f)
    }

    @Test
    fun `switching back to the score switched from renders nothing`() = runTest {
        // Given the tense score switched to from the calm one, playing from sample 13
        val sequencer = playing(calm)
        switchedToTense(sequencer)

        // When the calm score is switched back to
        sequencer.switchTo(calm)

        // Then it starts on the next bar, the start of the loop, from the cache
        assertThat(nextSlice()).containsExactly(413f, 414f, 415f)
        assertThat(nextSlice()).containsExactly(416f, 417f, 100f)
        assertThat(rendered).containsExactly(ScoreId.KOROBEINIKI to 1.0, ScoreId.MINUET to 1.0)
    }

    @Test
    fun `a second switch before the bar line gives up the rendering of the first`() = runTest {
        // Given the calm score playing, the tense one switched to, its rendering asked for but not run yet
        val sequencer = playing(calm)
        sequencer.switchTo(tense)
        nextSlice()
        runCurrent()

        // When a third score is switched to before the bar line, then the renderings may run
        sequencer.switchTo(third)
        assertThat(nextSlice()).containsExactly(103f, 104f, 105f)
        renderNow()

        // Then the third score starts on the next bar, the start of the loop, and the tense one was never rendered
        assertThat(nextSlice()).containsExactly(106f, 107f, 108f)
        assertThat(nextSlice()).containsExactly(109f, 110f, 111f)
        assertThat(nextSlice()).containsExactly(700f, 701f, 702f)
        assertThat(rendered).containsExactly(ScoreId.KOROBEINIKI to 1.0, MusicPlayerContract.THIRD to 1.0)
    }

    @Test
    fun `a score switched to as the danger begins is rendered at the danger tempo, not at the tempo before`() =
        runTest {
            // Given the calm score playing, its danger tempo prepared and rendered
            val sequencer = sequencer()
            sequencer.play(calm)
            sequencer.prepareTempo(1.25)
            renderNow()
            renderDispatcher.runAll()

            // When the danger begins: the tense score switched to, at the danger tempo
            sequencer.setTempoFactor(1.25)
            sequencer.switchTo(tense)
            nextSlice()
            renderNow()

            // Then the tense score is rendered at the danger tempo only, and plays at it
            assertThat(rendered).containsExactly(
                ScoreId.KOROBEINIKI to 1.0,
                ScoreId.KOROBEINIKI to 1.25,
                ScoreId.MINUET to 1.25,
            )
            assertThat(List(4) { nextSlice() }.flatten()).anyMatch { it in 500f..511f }
        }

    @Test
    fun `a playback resumed keeps the score switched from ready`() = runTest {
        // Given the tense score switched to from the calm one, stopped at sample 13, as a game paused
        val sequencer = sequencer()
        val playback = sequencer.play(calm)
        renderNow()
        switchedToTense(sequencer)
        playback.stop()
        runCurrent()

        // When the game resumes, then the calm score is switched back to
        playback.resume()
        runCurrent()
        sequencer.switchTo(calm)

        // Then the tense one resumes from the start of beat 4, then the calm one starts on the next bar, from the cache
        val written = List(3) { nextSlice() }.flatten()
        assertThat(written).containsExactly(412f, 413f, 414f, 415f, 416f, 417f, 100f, 101f, 102f)
        assertThat(rendered).containsExactly(ScoreId.KOROBEINIKI to 1.0, ScoreId.MINUET to 1.0)
    }

    @Test
    fun `a switch right after a play plays the score switched to from its start, without rendering the other`() =
        runTest {
            // Given a score played, its loop not started yet, as when the focus comes back
            val sequencer = sequencer()
            sequencer.play(calm)

            // When another score is switched to at once
            sequencer.switchTo(tense)
            renderNow()

            // Then
            assertThat(nextSlice()).containsExactly(400f, 401f, 402f)
            assertThat(rendered).containsExactly(ScoreId.MINUET to 1.0)
        }

    /**
     * The tense score switched to by [sequencer], playing the calm one, its first slice not received yet: it plays
     * from bar 2 of the tense score, its next slice from sample 13.
     */
    private suspend fun TestScope.switchedToTense(sequencer: ChiptuneSequencer) {
        sequencer.switchTo(tense)
        nextSlice()
        renderNow()
        nextSlice()
        assertThat(nextSlice()).containsExactly(106f, 107f, 412f)
    }

    /** A sequencer playing [played] at 1.0, its buffer rendered, before its first write is received. */
    private fun TestScope.playing(played: Score): ChiptuneSequencer = sequencer().also {
        it.play(played)
        renderNow()
    }

    private fun scoreOf(
        id: ScoreId,
        pitch: Pitch,
        bars: Int = 3,
    ): Score = Score(
        id = id,
        bpm = 150,
        beatsPerBar = 2,
        voices = listOf(Voice(Waveform.SQUARE, volume = 0.07, notes = List(2 * bars) { Note(pitch, beats = 1.0) })),
    )

    /** Lets the waiting work run, then the renderings waiting for the rendering dispatcher, then their results. */
    private fun TestScope.renderNow() {
        runCurrent()
        renderDispatcher.runAll()
        runCurrent()
    }

    /** The next slice written to the last sink opened. */
    private suspend fun TestScope.nextSlice(): List<Float> {
        runCurrent()
        return sinks.last().written.receive().toList()
    }

    private fun TestScope.sequencer(): ChiptuneSequencer = ChiptuneSequencer(
        render = { score, tempoFactor ->
            rendered += score.id to tempoFactor
            buffers.getValue(score.id to tempoFactor)
        },
        sampleRate = 22_050,
        openSink = { sampleRate -> FakeSink(sampleRate).also { sinks += it } },
        scope = backgroundScope,
        renderDispatcher = renderDispatcher,
        writeDispatcher = StandardTestDispatcher(testScheduler),
        sliceSize = 3,
    )

    private companion object {
        val LONG_CALM: ScoreId = ScoreId.Personal("long calm")
        val SHORT_TENSE: ScoreId = ScoreId.Personal("short tense")
    }
}
