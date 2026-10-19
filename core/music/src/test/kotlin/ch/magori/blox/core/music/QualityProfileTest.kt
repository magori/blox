package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class QualityProfileTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(doubles = [-0.1, 1.1, Double.NaN])
    fun `a share of steps to aim at outside 0 to 1 is refused`(share: Double) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { QualityProfile(share, threshold = 80.0) }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(doubles = [0.0, -0.1, 1.1, Double.NaN])
    fun `a leaning that aims at no move, or at more than all of them, is refused`(target: Double) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { Leaning(Lean.FIFTHS, target) }
    }

    @Test
    fun `a leaning may aim at all the moves`() {
        // When
        val leaning = Leaning(Lean.ARPEGGIOS, 1.0)

        // Then
        assertThat(leaning.target).isEqualTo(1.0)
    }
}
