package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BassStyleTest {
    @ParameterizedTest(name = "{0} beats")
    @ValueSource(doubles = [0.0, -0.5, Double.POSITIVE_INFINITY, Double.NaN])
    fun `a bass style whose notes do not last a positive, finite number of beats is refused`(beats: Double) {
        // When / Then
        assertThatThrownBy { BassStyle(BassMotif.REPEATED_ROOT, beats) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
