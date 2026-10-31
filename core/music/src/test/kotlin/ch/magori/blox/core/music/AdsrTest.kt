package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class AdsrTest {
    @ParameterizedTest(name = "attack {0}, decay {1}, sustain {2}, release {3}")
    @CsvSource(
        "-1, 250, 0.6, 200",
        "4, -1, 0.6, 200",
        "4, 250, 0.6, -1",
        "4, 250, -0.1, 200",
        "4, 250, 1.1, 200",
    )
    fun `an ADSR refuses a negative time and a sustain outside 0 and 1`(
        attackMillis: Int,
        decayMillis: Int,
        sustain: Double,
        releaseMillis: Int,
    ) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Adsr(attackMillis, decayMillis, sustain, releaseMillis) }
    }

    @ParameterizedTest(name = "sustain {0}")
    @ValueSource(doubles = [0.0, 1.0])
    fun `an ADSR accepts times of 0 and a sustain of 0 or 1`(sustain: Double) {
        // When / Then
        assertThatCode { Adsr(attackMillis = 0, decayMillis = 0, sustain = sustain, releaseMillis = 0) }
            .doesNotThrowAnyException()
    }
}
