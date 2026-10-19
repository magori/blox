package ch.magori.blox.core.music

import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ScoreIdTest {
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "  "])
    fun `a personal score is not known by a blank key`(key: String) {
        // When / Then
        assertThatIllegalArgumentException().isThrownBy { ScoreId.Personal(key) }
    }
}
