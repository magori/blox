package ch.magori.blox.feature.composer.grid

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.TrackGrid

/**
 * What touching a cell does, as TalkBack announces it: place a note, erase it, lengthen a note up to the cell, shorten
 * it there, or bring it back to one cell.
 */
internal enum class CellAction { PLACE, ERASE, LENGTHEN, SHORTEN, ONE_CELL }

/** What touching [cell] on the line of [pitch] does in [mode], as [TrackGrid.place] and [TrackGrid.lengthen] do it. */
internal fun TrackGrid.cellAction(
    mode: EditMode,
    pitch: Pitch,
    cell: Cell,
): CellAction = when (mode) {
    EditMode.PLACE -> if (notes.any { it.pitch == pitch && it.soundsIn(cell) }) CellAction.ERASE else CellAction.PLACE

    EditMode.LENGTHEN -> {
        val note = noteToLengthen(pitch, cell)
        when {
            note == null -> CellAction.PLACE
            note.start >= cell.start -> CellAction.ONE_CELL
            note.soundsIn(cell) -> CellAction.SHORTEN
            else -> CellAction.LENGTHEN
        }
    }
}
