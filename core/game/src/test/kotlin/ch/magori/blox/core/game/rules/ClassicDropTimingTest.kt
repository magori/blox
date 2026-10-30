package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassicDropTimingTest {
    /** A gravity table made for the test: 7 ticks per row at level 4, 3 at every other level. */
    private val gravityTable = object : GravityTable {
        override fun ticksPerRow(level: Level): Ticks = Ticks(if (level.value == 4) 7 else 3)
    }

    private val dropTiming: DropTiming = ClassicDropTiming(gravityTable)

    @ParameterizedTest(name = "level {0} -> one row after {1} ticks")
    @CsvSource("4, 7", "0, 3", "29, 3")
    fun `the classic drop timing follows the gravity table of the level`(
        level: Int,
        ticksPerRow: Int,
    ) {
        // Given a gravity table of ticksPerRow ticks at that level

        // When / Then: the piece waits until those ticks have passed, then drops
        (1 until ticksPerRow).forEach { ticks ->
            assertThat(drops(ticks, level)).describedAs("after $ticks ticks").isFalse()
        }
        assertThat(drops(ticksPerRow, level)).isTrue()
        assertThat(drops(ticksPerRow + 1, level)).describedAs("one tick late").isTrue()
    }

    @Test
    fun `the classic drop timing does not depend on the tick count`() {
        // Given a piece that waited 2 ticks, then 3, at level 0 (3 ticks per row)
        val tickCounts = listOf(1, 2, 3, 4, 47, 48, 1_000)

        // When / Then: whatever the tick count of the game, it waits after 2 ticks and drops after 3
        tickCounts.forEach { tickCount ->
            assertThat(drops(2, level = 0, tickCount)).describedAs("tick $tickCount").isFalse()
            assertThat(drops(3, level = 0, tickCount)).describedAs("tick $tickCount").isTrue()
        }
    }

    /** Given a piece that waited [ticks] at [level], on the tick [tickCount] of the game: whether it drops. */
    private fun drops(
        ticks: Int,
        level: Int,
        tickCount: Int = 0,
    ): Boolean = dropTiming.dropsAfter(Ticks(ticks), tickCount, Level(level))
}
