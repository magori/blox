package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Echo
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.melodyVoice
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.abs

class EchoMixTest {
    @Test
    fun `a click comes back after the delay of the echo at its feedback`() {
        // Given a click on the first of 1 050 samples, and an echo of 100 samples, a feedback of 0.5 and a part of 0.4
        val click = FloatArray(1_050).also { it[0] = 0.5f }
        val echo = Echo(delayBeats = 1.0, feedback = 0.5, wet = 0.4)

        // When
        val echoed = withEcho(click, echo, delaySamples = 100)

        // Then the click comes back every 100 samples, at the part of the echo, then each time at half its level
        assertThat(echoed[0].toDouble()).isCloseTo(0.5, within(1e-6))
        assertThat(echoed[100].toDouble()).isCloseTo(0.5 * 0.4, within(1e-6))
        assertThat(echoed[200].toDouble()).isCloseTo(0.5 * 0.4 * 0.5, within(1e-6))
        assertThat(echoed[300].toDouble()).isCloseTo(0.5 * 0.4 * 0.25, within(1e-6))
        assertThat(echoed[50].toDouble()).isCloseTo(0.0, within(1e-3))
        assertThat(echoed[150].toDouble()).isCloseTo(0.0, within(1e-3))
    }

    @Test
    fun `the delay of the echo follows the tempo, in beats`() {
        // Given a click on the first of 2 000 samples, at 1 000 samples a second, and an echo of half a beat
        val synthesizer = Synthesizer(sampleRate = 1_000)
        val click = FloatArray(2_000).also { it[0] = 0.5f }
        val echo = Echo(delayBeats = 0.5, feedback = 0.0, wet = 1.0)

        // When the music plays at 120 bpm, then 1.25 times faster
        val atTempo = synthesizer.withEcho(click, echo, bpm = 120.0)
        val faster = synthesizer.withEcho(click, echo, bpm = 150.0)

        // Then the click comes back after half a beat: 250 ms at 120 bpm, 200 ms at 150 bpm
        assertThat(firstEchoOf(atTempo)).isEqualTo(250)
        assertThat(firstEchoOf(faster)).isEqualTo(200)
    }

    @Test
    fun `the echo of the end of the loop sounds at its start, so that the loop has no seam`() {
        // Given a bar of 4 beats at 150 bpm whose only note is the last beat, and the echo of Velvet, of 3/4 of a beat
        val synthesizer = Synthesizer(sampleRate = 44_100)
        val score = lastBeatOnly()
        val echoDelay = 13_230 // 3/4 of a beat of 17 640 samples

        // When
        val velvet = synthesizer.render(score, style = SoundStyle.VELVET)

        // Then the echo of the last beat, past the end of the loop, comes back on its first samples
        assertThat(SoundStyle.VELVET.echo?.delayBeats).isEqualTo(0.75)
        val start = velvet.copyOfRange(0, echoDelay)
        assertThat(start.maxOf { abs(it) }).isGreaterThan(0.01f)
    }

    @ParameterizedTest(name = "in the style {0}")
    @EnumSource(SoundStyle::class)
    fun `the rendering keeps the length of the score`(style: SoundStyle) {
        // Given a bar of 4 beats at 150 bpm
        val synthesizer = Synthesizer(sampleRate = 44_100)

        // When it is rendered in the style, at tempo 1 and 1.25
        val atTempo = synthesizer.render(lastBeatOnly(), tempoFactor = 1.0, style)
        val faster = synthesizer.render(lastBeatOnly(), tempoFactor = 1.25, style)

        // Then it lasts 4 beats, echo or not: 1.6 s, then 1.28 s
        assertThat(atTempo).hasSize(70_560)
        assertThat(faster).hasSize(56_448)
    }

    @Test
    fun `the echo never takes the mix beyond -1 and 1`() {
        // Given a loud square, at 0.9, and an echo that comes back almost as loud, fully added
        val square = FloatArray(1_000) { if (it / 50 % 2 == 0) 0.9f else -0.9f }
        val echo = Echo(delayBeats = 1.0, feedback = 0.9, wet = 1.0)

        // When
        val echoed = withEcho(square, echo, delaySamples = 100)

        // Then
        assertThat(echoed.maxOf { abs(it) }).isLessThanOrEqualTo(1f)
    }

    /** A score of one bar of 4 beats at 150 bpm, a melody silent but for its last beat, an E5. */
    private fun lastBeatOnly(): Score = Score(
        ScoreId.KOROBEINIKI, // a synthetic score: its identifier plays no part here
        bpm = 150,
        beatsPerBar = 4,
        voices = listOf(melodyVoice(listOf(Note(pitch = null, beats = 3.0), Note(Pitch.parse("E5"), beats = 1.0)))),
    )

    /** The index of the first sound after the click on the first sample. */
    private fun firstEchoOf(samples: FloatArray): Int = (1 until samples.size).first { samples[it] != 0f }
}
