package ch.magori.blox.core.designsystem.text

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ScoreTextTest {
    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource("0, 000000", "1200, 001200")
    fun `a score is shown on 6 digits padded with zeros`(
        score: Int,
        expected: String,
    ) {
        assertThat(scoreText(score)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource("999999, 999999", "1000000, 999999", "2147483647, 999999")
    fun `beyond 999 999 the score shown stays at 999 999`(
        score: Int,
        expected: String,
    ) {
        assertThat(scoreText(score)).isEqualTo(expected)
    }

    @Test
    fun `a negative score is refused`() {
        assertThatIllegalArgumentException()
            .isThrownBy { scoreText(-1) }
            .withMessageContaining("negative")
    }
}
