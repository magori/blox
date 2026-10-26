package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock

/**
 * The cells of the padlock of [state] the screen draws, its [Padlock.SIZE] × [Padlock.SIZE], the ring of metal included:
 * the hole, the metal, the placed pieces; the hint over them, on the free cells where its piece fits or on the placed
 * piece that blocks; the preview over everything, each of its cells crossed out when it lies off the hole or over a
 * placed piece.
 */
internal fun lockCells(state: LocksmithUiState): Map<Position, LockCell> {
    val game = state.game
    val hint = state.hint
    val hinted = (hint as? Hint.Fit)?.piece?.cells.orEmpty()
    val blocking = (hint as? Hint.Blocking)?.rank
    val preview = state.preview
    val grid = 0 until Padlock.SIZE
    return grid.flatMap { row -> grid.map { column -> Position(column, row) } }.associateWith { cell ->
        val rank = game.rankAt(cell)
        when {
            preview != null && cell in preview.cells ->
                LockCell.Preview(preview.type, fits = cell in game.padlock.hole && rank == null)

            rank != null -> LockCell.Placed(game.given[rank], hinted = rank == blocking)

            cell in hinted -> LockCell.Hint

            cell in game.padlock.hole -> LockCell.Hole

            else -> LockCell.Metal
        }
    }
}
