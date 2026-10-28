package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.board.garden.GardenPlayHead
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood

/** The [Garden.COLUMNS] × [Garden.ROWS] cells of the grid, in reading order. */
internal val GRID_CELLS: List<Position> =
    (0 until Garden.ROWS).flatMap { row -> (0 until Garden.COLUMNS).map { column -> Position(column, row) } }

/**
 * The cells of the grid of [state] the screen draws: free, or a note of the piece placed there, marked when its note
 * belongs to the chord of its group; the preview over them, each of its cells crossed out when it lies over a note.
 * Its cells off the grid are not drawn: the message tells that it goes out of it.
 */
internal fun gardenCells(state: GardenUiState): Map<Position, GardenCell> {
    val garden = state.garden
    val preview = state.preview
    return GRID_CELLS.associateWith { cell ->
        val piece = garden.pieceAt(cell)
        when {
            preview != null && cell in preview.cells -> GardenCell.Preview(preview.type, fits = piece == null)
            piece != null -> GardenCell.Note(piece.type, inChord = garden.mood.isChordNote(cell))
            else -> GardenCell.Free
        }
    }
}

/**
 * What a touch of [cell] does, as TalkBack announces it, as `GardenViewModel.tap` does it: place a preview that fits,
 * move a refused one there, take a note back, show the chosen piece on a free cell.
 */
@StringRes
internal fun cellActionOf(cell: GardenCell): Int = when (cell) {
    is GardenCell.Preview ->
        if (cell.fits) R.string.feature_garden_action_place else R.string.feature_garden_action_move

    is GardenCell.Note -> R.string.feature_garden_action_take_back

    GardenCell.Free -> R.string.feature_garden_action_show
}

/**
 * The part of the play head [cell] lies under, while [state] is listened to: the column played is framed from its top
 * cell to its bottom one; null outside it, or without listening.
 */
internal fun playHeadAt(
    cell: Position,
    state: GardenUiState,
): GardenPlayHead? = when {
    cell.column != state.playedColumn -> null
    cell.row == 0 -> GardenPlayHead.TOP
    cell.row == Garden.ROWS - 1 -> GardenPlayHead.BOTTOM
    else -> GardenPlayHead.MIDDLE
}

/** The chord of the column played, from 0, whose pill is filled while [state] is listened to; null otherwise. */
internal fun playedChordOf(state: GardenUiState): Int? = state.playedColumn?.let { it / GardenMood.EIGHTHS_PER_CHORD }
