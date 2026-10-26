package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock

/** The columns and the rows inside the ring of metal of a padlock: the 8 × 8 cells the screen shows and touches. */
internal val INSIDE: IntRange = 1..Padlock.SIZE - 2

/** The [INSIDE] × [INSIDE] cells, in reading order. */
internal val INSIDE_CELLS: List<Position> = INSIDE.flatMap { row -> INSIDE.map { column -> Position(column, row) } }

/** Whether this cell lies inside the ring of metal of a padlock. */
internal val Position.isInside: Boolean get() = column in INSIDE && row in INSIDE

/**
 * The cells of the padlock of [state] the screen draws, the [INSIDE] × [INSIDE] inside the ring of metal, which the
 * screen draws as a thin rim: the hole, the metal, the placed pieces; the hint over them, on the free cells where its
 * piece fits or on the placed piece that blocks; the preview over everything, each of its cells crossed out when it
 * lies off the hole or over a placed piece. Those of the preview on the ring are [rimCells].
 */
internal fun lockCells(state: LocksmithUiState): Map<Position, LockCell> {
    val game = state.game
    val hint = state.hint
    val hinted = (hint as? Hint.Fit)?.piece?.cells.orEmpty()
    val blocking = (hint as? Hint.Blocking)?.rank
    val preview = state.preview
    return INSIDE_CELLS.associateWith { cell ->
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

/**
 * The cells of the preview of [state] that lie on the ring of metal, around the [INSIDE] square: the screen draws them
 * over the rim, beyond it, crossed out, since a piece never fits there. Those off the grid are not drawn.
 */
internal fun rimCells(state: LocksmithUiState): Map<Position, LockCell> {
    val preview = state.preview ?: return emptyMap()
    val grid = 0 until Padlock.SIZE
    return preview.cells
        .filter { it.column in grid && it.row in grid && !it.isInside }
        .associateWith { LockCell.Preview(preview.type, fits = false) }
}
