package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SoundStyleTest {
    @Test
    fun `the names of the sound styles are frozen`() {
        // Given the style is saved by name: renaming one would lose the choice of the player

        // When
        val names = SoundStyle.entries.map { it.name }

        // Then
        assertThat(names).containsExactly("ARCADE", "VELVET", "NEON")
    }

    @Test
    fun `in the Arcade style the square plays a square gated as today and the triangle a triangle`() {
        // When
        val melody = SoundStyle.ARCADE.instrumentFor(Waveform.SQUARE)
        val bass = SoundStyle.ARCADE.instrumentFor(Waveform.TRIANGLE)

        // Then
        assertThat(melody).isEqualTo(Instrument(Tone.SQUARE, Gate, lowPass = null, gain = 1.0))
        assertThat(bass).isEqualTo(Instrument(Tone.TRIANGLE, Gate, lowPass = null, gain = 1.0))
        assertThat(Gate.heldUntil).isEqualTo(0.80)
        assertThat(Gate.silentFrom).isEqualTo(0.95)
    }

    @Test
    fun `the Arcade style has no echo and no filter`() {
        // When
        val instruments = Waveform.entries.map { SoundStyle.ARCADE.instrumentFor(it) }

        // Then
        assertThat(SoundStyle.ARCADE.echo).isNull()
        assertThat(instruments).allSatisfy { assertThat(it.lowPass).isNull() }
    }

    @Test
    fun `the Velvet style takes the starting values of the specification`() {
        // Given values to check by ear on a phone (P14.16), then by the measure of the level (P14.17)
        val style = SoundStyle.VELVET

        // When
        val melody = style.instrumentFor(Waveform.SQUARE)
        val bass = style.instrumentFor(Waveform.TRIANGLE)

        // Then
        assertThat(melody).isEqualTo(Instrument(Tone.SINE, Adsr(4, 250, 0.6, 200), lowPass = null, gain = 1.0))
        assertThat(bass).isEqualTo(Instrument(Tone.TRIANGLE, Adsr(5, 300, 0.7, 150), lowPass = 600.0, gain = 1.0))
        assertThat(style.echo).isEqualTo(Echo(delayBeats = 0.75, feedback = 0.3, wet = 0.25))
    }

    @Test
    fun `the Neon style takes the starting values of the specification`() {
        // Given values to check by ear on a phone (P14.16), then by the measure of the level (P14.17)
        val style = SoundStyle.NEON

        // When
        val melody = style.instrumentFor(Waveform.SQUARE)
        val bass = style.instrumentFor(Waveform.TRIANGLE)

        // Then
        assertThat(melody).isEqualTo(Instrument(Tone.SAW, Adsr(2, 120, 0.7, 60), lowPass = 2500.0, gain = 1.0))
        assertThat(bass).isEqualTo(Instrument(Tone.PULSE_QUARTER, Adsr(2, 80, 0.5, 40), lowPass = 900.0, gain = 1.0))
        assertThat(style.echo).isEqualTo(Echo(delayBeats = 0.5, feedback = 0.2, wet = 0.2))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SoundStyle::class)
    fun `every style has an instrument for every waveform`(style: SoundStyle) {
        // When / Then
        assertThatCode { Waveform.entries.forEach { style.instrumentFor(it) } }.doesNotThrowAnyException()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(SoundStyle::class)
    fun `every instrument of every style starts within 5 milliseconds`(style: SoundStyle) {
        // Given a slow attack would move the beat heard in Au tempo

        // When
        val attacks = Waveform.entries.map { style.instrumentFor(it).shape.attackMillis }

        // Then
        assertThat(attacks).allSatisfy { assertThat(it).isBetween(0, 5) }
    }
}
