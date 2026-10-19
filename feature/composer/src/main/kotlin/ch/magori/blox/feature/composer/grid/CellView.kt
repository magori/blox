package ch.magori.blox.feature.composer.grid

import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.TrackGrid

/** What a sixteenth note of a line shows: nothing, the attack of a note, or a note held from before. */
internal enum class CellPart { SILENT, ATTACK, HELD }

/** What a cell of a line shows: its [first] sixteenth note, then its [second]. */
internal data class CellView(val first: CellPart, val second: CellPart) {
    /**
     * Whether the cell is drawn as two sixteenth notes: its halves do not show a single note held across it, nor
     * nothing.
     */
    val isSplit: Boolean get() = second != CellPart.HELD && !isEmpty

    /** Whether the cell shows no note. */
    val isEmpty: Boolean get() = first == CellPart.SILENT && second == CellPart.SILENT
}

/** What [cell] shows on the line of [pitch]. */
internal fun TrackGrid.cellView(
    pitch: Pitch,
    cell: Cell,
): CellView = CellView(partAt(pitch, cell.start), partAt(pitch, cell.end - 1))

private fun TrackGrid.partAt(
    pitch: Pitch,
    sixteenth: Int,
): CellPart {
    val note = notes.firstOrNull { it.pitch == pitch && sixteenth in it.start until it.end }
    return when {
        note == null -> CellPart.SILENT
        note.start == sixteenth -> CellPart.ATTACK
        else -> CellPart.HELD
    }
}
