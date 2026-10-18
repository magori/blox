package ch.magori.blox.feature.game.sidepanel

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SidePanelTextTest {
    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource("0, 000000", "1200, 001200")
    fun `the score is shown on 6 digits padded with zeros`(
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

    @ParameterizedTest(name = "level {0} -> {1}")
    @CsvSource("5, 05", "29, 29", "100, 100")
    fun `the level is shown on at least 2 digits, without a cap`(
        level: Int,
        expected: String,
    ) {
        assertThat(levelText(Level(level))).isEqualTo(expected)
    }

    @ParameterizedTest(name = "lines {0} -> {1}")
    @CsvSource("0, 000", "123, 123", "1000, 1000")
    fun `the lines are shown on at least 3 digits, without a cap`(
        lines: Int,
        expected: String,
    ) {
        assertThat(linesText(Lines(lines))).isEqualTo(expected)
    }
}
