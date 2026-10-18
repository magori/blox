package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicLevelProgressionTest {
    private val progression: LevelProgression = ClassicLevelProgression

    @ParameterizedTest(name = "start 0, {0} lines -> level {1}")
    @CsvSource("9, 0", "10, 1")
    fun `starting at level 0 the first level up comes at 10 lines`(
        lines: Int,
        expectedLevel: Int,
    ) {
        assertThat(levelAfter(lines, startingAt = 0)).isEqualTo(Level(expectedLevel))
    }

    @ParameterizedTest(name = "start {0}, {1} lines -> level {2}")
    @CsvSource("0, 0, 0", "5, 0, 5", "5, 30, 5", "5, 59, 5", "9, 0, 9", "9, 99, 9")
    fun `before the first level up the level stays at the start level`(
        start: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        assertThat(levelAfter(lines, startingAt = start)).isEqualTo(Level(expectedLevel))
    }

    @ParameterizedTest(name = "start {0}, {1} lines -> level {2}")
    @CsvSource("5, 60, 6", "9, 100, 10")
    fun `the first level up comes at the start level plus one times 10 lines`(
        start: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        assertThat(levelAfter(lines, startingAt = start)).isEqualTo(Level(expectedLevel))
    }

    @ParameterizedTest(name = "start {0}, {1} lines -> level {2}")
    @CsvSource("0, 20, 2", "5, 70, 7", "9, 110, 11")
    fun `after the first level up the level rises every 10 lines`(
        start: Int,
        lines: Int,
        expectedLevel: Int,
    ) {
        assertThat(levelAfter(lines, startingAt = start)).isEqualTo(Level(expectedLevel))
    }

    @Test
    fun `a start level outside 0 to 9 is refused`() {
        assertThatIllegalArgumentException().isThrownBy { levelAfter(lines = 0, startingAt = 10) }
    }

    /** When: the rule computes the level after [lines] lines in a game started at level [startingAt]. */
    private fun levelAfter(
        lines: Int,
        startingAt: Int,
    ): Level = progression.levelFor(start = Level(startingAt), lines = Lines(lines))
}
