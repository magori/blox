package ch.magori.blox.core.audio

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EffectSampleRateTest {
    @ParameterizedTest(name = "{0} Hz")
    @ValueSource(strings = ["44100", "48000", "96000"])
    fun `the effects are rendered at the sample rate of the output, so that it may play them on its fast path`(
        rate: String,
    ) {
        // When / Then
        assertThat(effectSampleRateOf(outputSampleRate = rate)).isEqualTo(rate.toInt())
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "abc", "0", "-48000"])
    fun `an output sample rate that is not a positive number gives 48 000 Hz`(rate: String) {
        // When / Then
        assertThat(effectSampleRateOf(outputSampleRate = rate)).isEqualTo(48_000)
    }

    @Test
    fun `an output that does not tell its sample rate gives 48 000 Hz`() {
        // When / Then
        assertThat(effectSampleRateOf(outputSampleRate = null)).isEqualTo(48_000)
    }
}
