package ch.magori.blox.feature.locksmith

import androidx.annotation.StringRes
import ch.magori.blox.feature.locksmith.PadlocksItem.Cell

/**
 * The words a tile shows under its number: its state, then « en cours » when the player comes from it. The text always
 * tells the state; the colour only doubles it.
 */
internal fun wordsOf(cell: Cell): List<Int> = listOf(stateWordOf(cell.state)) + currentOf(cell)

/**
 * What TalkBack reads of a tile after its number, before its best opening: its state in full, a perfect padlock being
 * open and perfect, then « en cours » when the player comes from it.
 */
internal fun readOf(cell: Cell): List<Int> {
    val state = if (cell.state == CellState.PERFECT) {
        listOf(R.string.feature_locksmith_padlocks_open, R.string.feature_locksmith_padlocks_perfect)
    } else {
        listOf(stateWordOf(cell.state))
    }
    return state + currentOf(cell)
}

@StringRes
private fun stateWordOf(state: CellState): Int = when (state) {
    CellState.OPEN -> R.string.feature_locksmith_padlocks_open
    CellState.PERFECT -> R.string.feature_locksmith_padlocks_perfect
    CellState.FRONTIER -> R.string.feature_locksmith_padlocks_frontier
    CellState.LOCKED -> R.string.feature_locksmith_padlocks_locked
}

private fun currentOf(cell: Cell): List<Int> =
    if (cell.current) listOf(R.string.feature_locksmith_padlocks_current) else emptyList()
