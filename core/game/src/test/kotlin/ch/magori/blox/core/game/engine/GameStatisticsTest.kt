package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.LineClear
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GameStatisticsTest {
    @Test
    fun `a game starts with no four-line clear and no tick played`() {
        val statistics = GameStatistics()

        assertThat(statistics.fourLineClears).isEqualTo(0)
        assertThat(statistics.ticksPlayed).isEqualTo(0)
    }

    @ParameterizedTest(name = "{0} line(s) cleared at once -> {1} four-line clear(s)")
    @CsvSource("4, 1", "1, 0", "2, 0", "3, 0")
    fun `only a clear of 4 lines at once counts as a four-line clear`(
        linesCleared: Int,
        expectedFourLineClears: Int,
    ) {
        assertThat(afterClears(linesCleared).fourLineClears).isEqualTo(expectedFourLineClears)
    }

    @Test
    fun `four-line clears add up over the game`() {
        assertThat(afterClears(4, 2, 4).fourLineClears).isEqualTo(2)
    }

    @ParameterizedTest(name = "{0} tick(s) played")
    @CsvSource("1", "60", "125")
    fun `each tick played adds 1 to the ticks played`(ticks: Int) {
        assertThat(afterTicks(ticks).ticksPlayed).isEqualTo(ticks)
    }

    /** When: the lines in [linesClearedEachTime] are cleared at once, one clear after the other, from the start. */
    private fun afterClears(vararg linesClearedEachTime: Int): GameStatistics =
        linesClearedEachTime.fold(GameStatistics()) { statistics, lines -> statistics.afterClear(LineClear(lines)) }

    /** When: [ticks] ticks are played from the start of a game. */
    private fun afterTicks(ticks: Int): GameStatistics =
        (1..ticks).fold(GameStatistics()) { statistics, _ -> statistics.afterTick() }
}
