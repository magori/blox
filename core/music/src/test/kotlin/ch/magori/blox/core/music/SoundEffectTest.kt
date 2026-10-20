package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SoundEffectTest {
    @Test
    fun `an effect lasts the sum of its blips`() {
        // Given
        val effect = SoundEffect(
            waveform = Waveform.SQUARE,
            volume = 0.1,
            envelope = Envelope(attackMillis = 2, releaseMillis = 10),
            blips = listOf(Blip(Pitch.parse("C6"), millis = 40), Blip(Pitch.parse("G6"), millis = 90)),
        )

        // When / Then
        assertThat(effect.millis).isEqualTo(130)
    }

    @Test
    fun `an effect without blips is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { anEffect(blips = emptyList()) }
    }

    @ParameterizedTest(name = "{0} ms is refused")
    @ValueSource(ints = [0, -10])
    fun `a blip lasts a positive number of milliseconds`(millis: Int) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Blip(Pitch.parse("C6"), millis) }
    }

    @ParameterizedTest(name = "a volume of {0} is refused")
    @ValueSource(doubles = [0.0, -0.1, 1.01, Double.NaN])
    fun `the volume of an effect is above 0 and at most 1`(volume: Double) {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { anEffect(volume = volume) }
    }

    @Test
    fun `an envelope that rises or falls over a negative time is refused`() {
        // Given / When / Then
        assertThatIllegalArgumentException().isThrownBy { Envelope(attackMillis = -1, releaseMillis = 10) }
        assertThatIllegalArgumentException().isThrownBy { Envelope(attackMillis = 2, releaseMillis = -1) }
    }

    @Test
    fun `an envelope longer than a blip is refused, one as long as the shortest blip is accepted`() {
        // Given blips of 30 and 50 ms
        val blips = listOf(Blip(Pitch.parse("C6"), millis = 30), Blip(Pitch.parse("E6"), millis = 50))

        // When / Then
        assertThatIllegalArgumentException().isThrownBy { anEffect(Envelope(10, 21), blips) }
        assertThatNoException().isThrownBy { anEffect(Envelope(10, 20), blips) }
    }

    private fun anEffect(
        envelope: Envelope = Envelope(attackMillis = 2, releaseMillis = 10),
        blips: List<Blip> = listOf(Blip(Pitch.parse("C6"), millis = 40)),
        volume: Double = 0.1,
    ): SoundEffect = SoundEffect(Waveform.SQUARE, volume, envelope, blips)
}
