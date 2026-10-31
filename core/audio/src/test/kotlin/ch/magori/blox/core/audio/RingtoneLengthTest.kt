package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Voice
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.roundToInt

class RingtoneLengthTest {
    @Test
    fun `the length of a ringtone is that of its whole loops, in whole seconds`() {
        // Given: eight bars of four beats at 132 beats a minute, a loop of 14.5 s
        val score = aScore(bars = 8, bpm = 132)

        // When
        val seconds = ringtoneSeconds(score)

        // Then: two loops, 29.1 s
        assertThat(seconds).isEqualTo(29)
    }

    @Test
    fun `the ringtone of a music longer than 60 seconds lasts up to its last whole bar before 60 seconds`() {
        // Given: sixteen bars of 4.8 s at 50 beats a minute, a loop of 76.8 s
        val score = aScore(bars = 16, bpm = 50)

        // When
        val seconds = ringtoneSeconds(score)

        // Then: twelve whole bars, 57.6 s
        assertThat(seconds).isEqualTo(58)
    }

    @ParameterizedTest
    @CsvSource("8, 132", "16, 50", "3, 97")
    fun `the length told is that of the samples written`(
        bars: Int,
        bpm: Int,
    ) {
        // Given the samples of the ringtone, as the writer makes them of the rendering by the synthesizer of the music
        val score = aScore(bars, bpm)
        val rendering = Synthesizer(MUSIC_SAMPLE_RATE).render(score)
        val samples = ringtoneSamples(rendering, MUSIC_SAMPLE_RATE, samplesPerBar(score, MUSIC_SAMPLE_RATE))

        // When
        val seconds = ringtoneSeconds(score)

        // Then
        assertThat(seconds).isEqualTo((samples.size.toDouble() / MUSIC_SAMPLE_RATE).roundToInt())
    }

    /** A score of [bars] bars of four beats at [bpm], each bar one whole note. */
    private fun aScore(
        bars: Int,
        bpm: Int,
    ): Score = Score(
        id = ScoreId.Personal("ringtone"),
        bpm = bpm,
        beatsPerBar = 4,
        voices = listOf(Voice(Waveform.SQUARE, 0.07, List(bars) { Note(Pitch.parse("E4"), 4.0) })),
    )
}
