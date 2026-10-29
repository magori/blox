package ch.magori.blox.feature.locksmith

import ch.magori.blox.feature.locksmith.PadlocksItem.Cell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** A tile of the grid « Serrures » tells its state by its words, the colour only doubling them. */
class PadlockTileTest {
    @Test
    fun `the words of a tile tell its state, and whether the player comes from it`() {
        assertThat(wordsOf(Cell(1, CellState.OPEN))).containsExactly(R.string.feature_locksmith_padlocks_open)
        assertThat(wordsOf(Cell(1, CellState.PERFECT))).containsExactly(R.string.feature_locksmith_padlocks_perfect)
        assertThat(wordsOf(Cell(1, CellState.FRONTIER))).containsExactly(R.string.feature_locksmith_padlocks_frontier)
        assertThat(wordsOf(Cell(1, CellState.LOCKED))).containsExactly(R.string.feature_locksmith_padlocks_locked)
        assertThat(wordsOf(Cell(1, CellState.OPEN, current = true)))
            .containsExactly(R.string.feature_locksmith_padlocks_open, R.string.feature_locksmith_padlocks_current)
    }

    @Test
    fun `a tile is read with its state in full, a perfect padlock being open`() {
        assertThat(readOf(Cell(1, CellState.PERFECT, current = true))).containsExactly(
            R.string.feature_locksmith_padlocks_open,
            R.string.feature_locksmith_padlocks_perfect,
            R.string.feature_locksmith_padlocks_current,
        )
        assertThat(readOf(Cell(1, CellState.LOCKED))).containsExactly(R.string.feature_locksmith_padlocks_locked)
    }
}
