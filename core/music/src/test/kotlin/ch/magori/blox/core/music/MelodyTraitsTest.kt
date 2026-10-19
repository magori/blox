package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MelodyTraitsTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY])
    fun `a factor that is not positive and finite is refused, as it would cancel or swamp the weights of the draws`(
        factor: Double,
    ) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { MelodyTraits(fifths = factor) }
        assertThatIllegalArgumentException().isThrownBy { MelodyTraits(descents = factor) }
        assertThatIllegalArgumentException().isThrownBy { MelodyTraits(arpeggios = factor) }
    }
}
