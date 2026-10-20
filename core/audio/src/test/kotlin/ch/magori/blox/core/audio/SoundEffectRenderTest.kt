package ch.magori.blox.core.audio

import ch.magori.blox.core.music.Blip
import ch.magori.blox.core.music.Envelope
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import kotlin.math.abs

class SoundEffectRenderTest {
    @Test
    fun `an effect lasts its milliseconds at the sample rate`() {
        // Given an effect of 30 + 45 ms, at 44 100 Hz
        val effect = SoundEffect(
            Waveform.SQUARE,
            volume = 0.5,
            Envelope(attackMillis = 2, releaseMillis = 10),
            listOf(Blip(Pitch.parse("C6"), millis = 30), Blip(Pitch.parse("E6"), millis = 45)),
        )

        // When
        val samples = Synthesizer(sampleRate = 44_100).render(effect)

        // Then 75 ms of 44.1 samples
        assertThat(samples).hasSize(3_308)
    }

    @Test
    fun `each blip rises over the attack from silence to the volume, holds it, then falls over the release`() {
        // Given two square blips of 40 ms at 1 000 Hz, one sample a millisecond: the level is the volume times the
        // envelope
        val effect = SoundEffect(
            Waveform.SQUARE,
            volume = 0.5,
            Envelope(attackMillis = 2, releaseMillis = 10),
            listOf(Blip(Pitch.parse("C6"), millis = 40), Blip(Pitch.parse("E6"), millis = 40)),
        )

        // When
        val levels = Synthesizer(sampleRate = 1_000).render(effect).map { abs(it).toDouble() }

        // Then each blip has the same shape
        val shape = mapOf(0 to 0.0, 1 to 0.25, 2 to 0.5, 29 to 0.5, 30 to 0.5, 35 to 0.25, 39 to 0.05)
        listOf(0, 40).forEach { start ->
            shape.forEach { (index, level) ->
                assertThat(levels[start + index]).describedAs("level of sample ${start + index}")
                    .isCloseTo(level, within(1e-6))
            }
        }
    }

    @Test
    fun `a blip plays its pitch on the waveform of the effect`() {
        // Given a square A4 of one second, at 44 100 Hz
        val effect = SoundEffect(
            Waveform.SQUARE,
            volume = 0.5,
            Envelope(attackMillis = 2, releaseMillis = 10),
            listOf(Blip(Pitch.parse("A4"), millis = 1_000)),
        )

        // When
        val samples = Synthesizer(sampleRate = 44_100).render(effect)

        // Then it rises through 0 every 100.2 samples, the period of 440 Hz
        val risingEdges = (1 until samples.size).filter { samples[it - 1] < 0 && samples[it] > 0 }
        val period = (risingEdges.last() - risingEdges.first()).toDouble() / (risingEdges.size - 1)
        assertThat(period).isCloseTo(44_100.0 / 440, within(0.01))
    }
}
