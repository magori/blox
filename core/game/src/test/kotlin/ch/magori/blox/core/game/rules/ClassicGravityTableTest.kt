package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicGravityTableTest {
    private val gravity: GravityTable = ClassicGravityTable

    @Test
    fun `at level 0 the piece falls one row every 48 ticks`() {
        assertThat(ticksAt(level = 0)).isEqualTo(48)
    }

    @ParameterizedTest(name = "level {0} -> {1} ticks per row")
    @CsvSource("1, 43", "2, 38", "3, 33", "4, 28", "5, 23", "6, 18", "7, 13", "8, 8", "9, 6")
    fun `from level 1 to 9 each level has its own speed`(
        level: Int,
        expectedTicks: Int,
    ) {
        assertThat(ticksAt(level)).isEqualTo(expectedTicks)
    }

    @ParameterizedTest(name = "level {0} -> {1} ticks per row")
    @CsvSource("10, 5", "12, 5", "13, 4", "15, 4", "16, 3", "18, 3", "19, 2", "28, 2")
    fun `from level 10 to 28 levels share the same speed by tier`(
        level: Int,
        expectedTicks: Int,
    ) {
        assertThat(ticksAt(level)).isEqualTo(expectedTicks)
    }

    @ParameterizedTest(name = "level {0} -> 1 tick per row")
    @CsvSource("29", "50")
    fun `from level 29 onwards the piece falls one row every tick`(level: Int) {
        assertThat(ticksAt(level)).isEqualTo(1)
    }

    /** Given a level, when the gravity table is asked for its speed: the ticks per row. */
    private fun ticksAt(level: Int): Int = gravity.ticksPerRow(Level(level))
}
