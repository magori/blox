package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.locksmith.PadlockResult
import ch.magori.blox.core.locksmith.PadlockTier
import ch.magori.blox.feature.locksmith.PadlocksItem.Cell
import ch.magori.blox.feature.locksmith.PadlocksItem.TierHeader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** The grid of the screen « Serrures »: a header per tier, then its padlocks, open, the frontier, locked. */
class PadlocksGridTest {
    @Test
    fun `the open padlocks then the frontier, each under the header of its tier`() {
        // Given padlocks 1 to 4 open, on a grid of 2 columns: the row of the frontier, 4 and 5, is full
        val openings = (1..4).map(::openedWithAHint)

        // When
        val grid = padlocksGridOf(openings, current = null, columns = 2)

        // Then
        assertThat(grid).containsExactly(
            TierHeader(PadlockTier.APPRENTICE),
            Cell(1, CellState.OPEN, openings[0]),
            Cell(2, CellState.OPEN, openings[1]),
            Cell(3, CellState.OPEN, openings[2]),
            TierHeader(PadlockTier.JOURNEYMAN),
            Cell(4, CellState.OPEN, openings[3]),
            Cell(5, CellState.FRONTIER),
        )
    }

    @Test
    fun `a padlock opened in as many moves as pieces, without refusal or hint, is perfect`() {
        // Given padlock 1 opened perfectly, padlock 2 with a hint
        val perfect = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val openings = listOf(perfect, openedWithAHint(2))

        // When
        val grid = padlocksGridOf(openings, current = null, columns = 3)

        // Then
        assertThat(grid.filterIsInstance<Cell>().map(Cell::state))
            .containsExactly(CellState.PERFECT, CellState.OPEN, CellState.FRONTIER)
    }

    @Test
    fun `locked padlocks complete the row of the frontier, and no more`() {
        // Given padlocks 1 to 4 open, on a grid of 4 columns: the journeymen begin a row, 4 open and 5 the frontier
        val openings = (1..4).map(::openedWithAHint)

        // When
        val grid = padlocksGridOf(openings, current = null, columns = 4)

        // Then
        assertThat(grid.takeLastWhile { it != TierHeader(PadlockTier.JOURNEYMAN) }).containsExactly(
            Cell(4, CellState.OPEN, openings[3]),
            Cell(5, CellState.FRONTIER),
            Cell(6, CellState.LOCKED),
            Cell(7, CellState.LOCKED),
        )
    }

    @Test
    fun `the locked padlocks stop at the end of the tier of the frontier`() {
        // Given padlock 1 open, on a grid of 4 columns: the apprentices end at padlock 3
        val openings = listOf(openedWithAHint(1))

        // When
        val grid = padlocksGridOf(openings, current = null, columns = 4)

        // Then
        assertThat(grid).containsExactly(
            TierHeader(PadlockTier.APPRENTICE),
            Cell(1, CellState.OPEN, openings[0]),
            Cell(2, CellState.FRONTIER),
            Cell(3, CellState.LOCKED),
        )
    }

    @Test
    fun `the padlock the player comes from is in progress, open or the frontier`() {
        // Given padlocks 1 and 2 open
        val openings = (1..2).map(::openedWithAHint)

        // When the player comes from padlock 2, an old one replayed, or from padlock 3, the frontier
        val fromOld = padlocksGridOf(openings, current = 2, columns = 3)
        val fromFrontier = padlocksGridOf(openings, current = 3, columns = 3)

        // Then
        assertThat(fromOld.filterIsInstance<Cell>().filter(Cell::current).map(Cell::level)).containsExactly(2)
        assertThat(fromFrontier.filterIsInstance<Cell>().filter(Cell::current).map(Cell::level)).containsExactly(3)
    }

    @Test
    fun `a locked padlock is never in progress, even when the player comes from it`() {
        // Given padlock 1 open, the player coming from padlock 3, as after an opening of padlock 2 not recorded
        val openings = listOf(openedWithAHint(1))

        // When
        val grid = padlocksGridOf(openings, current = 3, columns = 3)

        // Then
        assertThat(grid.last()).isEqualTo(Cell(3, CellState.LOCKED))
    }

    @Test
    fun `without an opening, padlock 1 is the frontier, its row completed`() {
        // When
        val grid = padlocksGridOf(emptyList(), current = null, columns = 4)

        // Then
        assertThat(grid).containsExactly(
            TierHeader(PadlockTier.APPRENTICE),
            Cell(1, CellState.FRONTIER),
            Cell(2, CellState.LOCKED),
            Cell(3, CellState.LOCKED),
        )
    }

    @ParameterizedTest(name = "frontier {0} on {1} columns: locked {2} to {3}")
    @CsvSource(
        // The craftsmen, 13 to 20: the frontier 20 ends a row of 4, and ends the tier on 5 columns.
        "20, 4, 0, -1",
        "20, 5, 0, -1",
        // The masters begin with 21: its row completed on 4 and on 5 columns.
        "21, 4, 22, 24",
        "21, 5, 22, 25",
        // Beyond 30, the cycle stays among the masters, 21 on: locked padlocks only complete the row.
        "31, 4, 32, 32",
        "33, 5, 34, 35",
        "36, 4, 0, -1",
    )
    fun `the locked padlocks complete the row of the frontier within its tier, also on the cycle beyond 30`(
        frontier: Int,
        columns: Int,
        firstLocked: Int,
        lastLocked: Int,
    ) {
        // Given padlocks 1 to the one before the frontier open
        val openings = (1 until frontier).map(::openedWithAHint)

        // When
        val grid = padlocksGridOf(openings, current = null, columns = columns)

        // Then
        val locked = grid.filterIsInstance<Cell>().filter { it.state == CellState.LOCKED }.map(Cell::level)
        assertThat(locked).isEqualTo((firstLocked..lastLocked).toList())
        assertThat(grid.filterIsInstance<Cell>().last { it.state != CellState.LOCKED }.level).isEqualTo(frontier)
    }

    private companion object {
        /** An opening of padlock [level], of 3 pieces, with a hint: not perfect. */
        fun openedWithAHint(level: Int) = PadlockResult(pieces = 3, moves = 3 + level, refusals = 0, hints = 1)
    }
}
