package ch.magori.blox.core.audio

import ch.magori.blox.core.locksmith.PadlockChords
import ch.magori.blox.core.music.Blip
import ch.magori.blox.core.music.Envelope
import ch.magori.blox.core.music.GardenSounds
import ch.magori.blox.core.music.LocksmithSounds
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.RomanNumeral
import ch.magori.blox.core.music.SoundEffect
import ch.magori.blox.core.music.SoundEffects
import ch.magori.blox.core.music.SoundStyle
import ch.magori.blox.core.music.Tone
import ch.magori.blox.core.music.Waveform
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SoundEffectStyleTest {
    private val synthesizer = Synthesizer(sampleRate = SAMPLE_RATE)

    @Test
    fun `in the Velvet style the lines cleared play on the tone of the velvet melody`() {
        // Given the lines cleared, on the square, whose first blip is a C6 of 40 ms at 0.08: held at full volume from
        // the end of its rise of 2 ms (96 samples) to the start of its fall of 15 ms (sample 1 200)
        val effect = SoundEffects.linesCleared
        assertThat(effect.waveform).isEqualTo(Waveform.SQUARE)
        assertThat(SoundStyle.VELVET.instrumentFor(Waveform.SQUARE).tone).isEqualTo(Tone.SINE)

        // When
        val samples = synthesizer.render(effect, SoundStyle.VELVET)

        // Then its held part is the sine of the velvet melody, at the volume of the effect
        val frequency = Pitch.parse("C6").frequency
        (HELD_FROM until HELD_UNTIL).forEach { index ->
            val sine = 0.08 * sin(2 * PI * frequency * index / SAMPLE_RATE)
            assertThat(samples[index].toDouble()).describedAs("sample $index").isCloseTo(sine, within(1e-6))
        }
    }

    @Test
    fun `in the Neon style an effect passes the low-pass of its instrument`() {
        // Given a note of the garden, on the square, and the melody of Neon: a saw, filtered at 2 500 Hz
        val effect = GardenSounds.note(Pitch.parse("C5"))
        val melody = SoundStyle.NEON.instrumentFor(Waveform.SQUARE)
        assertThat(melody.lowPass).isEqualTo(2_500.0)

        // When
        val samples = synthesizer.render(effect, SoundStyle.NEON)

        // Then it is the note on the saw of Neon, without filter, passed through its low-pass
        val unfiltered = synthesizer.render(effect, melody.copy(lowPass = null))
        assertThat(samples).containsExactly(*lowPass(unfiltered, cutoff = 2_500.0, SAMPLE_RATE))
    }

    @ParameterizedTest(name = "in the style {0}")
    @EnumSource(SoundStyle::class)
    fun `an effect keeps its own envelope, starting within 5 milliseconds, whatever the style`(style: SoundStyle) {
        Waveform.entries.forEach { waveform ->
            // Given an A4 of 300 ms, at 0.5, rising over 2 ms and falling over its last 10 ms
            val effect = SoundEffect(
                waveform,
                volume = 0.5,
                Envelope(attackMillis = 2, releaseMillis = 10),
                listOf(Blip(Pitch.parse("A4"), millis = 300)),
            )

            // When
            val samples = synthesizer.render(effect, style)

            // Then it starts from silence, is loud within 5 ms, then holds its level, unlike the decay of a note of
            // the style, up to its own fall, which ends near silence
            val held = peakBetween(samples, fromMillis = 10, untilMillis = 30)
            assertThat(samples[0]).describedAs("$waveform starts from silence").isEqualTo(0f)
            assertThat(peakBetween(samples, fromMillis = 0, untilMillis = 5)).describedAs("$waveform within 5 ms")
                .isGreaterThan(held / 2)
            assertThat(peakBetween(samples, fromMillis = 250, untilMillis = 280)).describedAs("$waveform held")
                .isCloseTo(held, within(held * 0.02f))
            assertThat(abs(samples.last())).describedAs("$waveform falls").isLessThan(held / 10)
        }
    }

    @ParameterizedTest(name = "in the style {0}")
    @EnumSource(SoundStyle::class)
    fun `an effect has no echo, so that it lasts its milliseconds`(style: SoundStyle) {
        // Given the lines cleared, 160 ms
        val effect = SoundEffects.linesCleared

        // When
        val samples = synthesizer.render(effect, style)

        // Then it lasts 160 ms at 48 000 Hz, and nothing comes back over its last note: it ends in silence
        assertThat(samples).hasSize(7_680)
        assertThat(abs(samples.last())).isLessThan(0.001f)
    }

    @ParameterizedTest(name = "in the style {0}")
    @EnumSource(SoundStyle::class)
    fun `in every style the effects of the locksmith and of the garden take the tone of their waveform`(
        style: SoundStyle,
    ) {
        // Given the effects of a chord of the locksmith, the arpeggio on the square and the bass on the triangle, its
        // opening flourish and its refusal, and a note of the garden, on the square
        val chord = PadlockChords.KEY.chord(RomanNumeral.parse("V"))
        val effects = LocksmithSounds.effectsOf(chord) +
            listOf(LocksmithSounds.openingFlourish, LocksmithSounds.refusal, GardenSounds.note(Pitch.parse("E5")))
        assertThat(effects.map { it.waveform }.toSet()).containsExactlyInAnyOrder(*Waveform.entries.toTypedArray())

        effects.forEach { effect ->
            // When
            val samples = synthesizer.render(effect, style)

            // Then each plays on the instrument the style gives to its waveform, the melody or the bass
            val instrument = style.instrumentFor(effect.waveform)
            assertThat(samples).describedAs("${effect.waveform} in $style")
                .containsExactly(*synthesizer.render(effect, instrument))
        }
    }

    /** The largest level of [samples] from [fromMillis] until [untilMillis]. */
    private fun peakBetween(
        samples: FloatArray,
        fromMillis: Int,
        untilMillis: Int,
    ): Float = samples.copyOfRange(sampleAt(fromMillis), sampleAt(untilMillis)).maxOf { abs(it) }

    private fun sampleAt(millis: Int): Int = millis * SAMPLE_RATE / 1_000

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val HELD_FROM = 96
        const val HELD_UNTIL = 1_200
    }
}
