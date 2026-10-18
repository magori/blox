package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicScoringRuleTest {
    private val scoring: ScoringRule = ClassicScoringRule

    @Test
    fun `clearing no line scores no point`() {
        assertThat(pointsForClearing(lines = 0, atLevel = 0)).isEqualTo(0)
    }

    @ParameterizedTest(name = "{0} line(s) at level 0 -> {1} points")
    @CsvSource("1, 40", "2, 100", "3, 300", "4, 1200")
    fun `at level 0 each number of lines cleared has its base value`(
        lines: Int,
        expectedPoints: Int,
    ) {
        assertThat(pointsForClearing(lines, atLevel = 0)).isEqualTo(expectedPoints)
    }

    @ParameterizedTest(name = "{0} line(s) at level {1} -> {2} points")
    @CsvSource("1, 1, 80", "4, 9, 12000", "1, 29, 1200")
    fun `the base value is multiplied by the level plus one`(
        lines: Int,
        level: Int,
        expectedPoints: Int,
    ) {
        assertThat(pointsForClearing(lines, atLevel = level)).isEqualTo(expectedPoints)
    }

    @ParameterizedTest(name = "{0} lines cleared at once is refused")
    @CsvSource("-1", "5")
    fun `a number of lines cleared outside 0 to 4 is refused`(lines: Int) {
        assertThatIllegalArgumentException().isThrownBy { pointsForClearing(lines, atLevel = 0) }
    }

    @ParameterizedTest(name = "{0} row(s) soft dropped -> {1} points")
    @CsvSource("0, 0", "5, 5")
    fun `a soft drop scores one point per row dropped`(
        rows: Int,
        expectedPoints: Int,
    ) {
        assertThat(pointsForSoftDropping(rows)).isEqualTo(expectedPoints)
    }

    @Test
    fun `a negative number of rows soft dropped is refused`() {
        assertThatIllegalArgumentException().isThrownBy { pointsForSoftDropping(rows = -1) }
    }

    /** When: the rule scores [lines] cleared at once at level [atLevel]. */
    private fun pointsForClearing(
        lines: Int,
        atLevel: Int,
    ): Int = scoring.pointsForLinesCleared(lines, Level(atLevel))

    /** When: the rule scores [rows] dropped while holding down. */
    private fun pointsForSoftDropping(rows: Int): Int = scoring.pointsForSoftDrop(rows)
}
