package ch.magori.blox.feature.composer.grid

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.model.CELLS_PER_BAR
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditorState

/**
 * A line of the grid as shown: its [pitch], its [name], whether it is [inScale], whether a space above it sets it
 * apart from the octave above, [spaceAbove], what its [cells] show in the selected bar, in which of them a note of the
 * other track sounds, [otherTrack], to be marked in watermark, and what touching each of them does, [actions].
 */
@Immutable
internal data class RowView(
    val pitch: Pitch,
    val name: RowName,
    val inScale: Boolean,
    val spaceAbove: Boolean,
    val cells: List<CellView>,
    val otherTrack: List<Boolean>,
    val actions: List<CellAction>,
)

/**
 * The lines of the grid of the open track of [editor], from the highest down, in its selected bar. With
 * [showOtherTrack], the notes of the other track are marked on the line of the same name in the same octave of the
 * range of the open track; a note of the other track whose line the open track does not have (a note out of the scale
 * of the open track, or out of its range) is not marked: the watermark is a guide, not a second grid.
 */
internal fun gridRows(
    editor: EditorState,
    showOtherTrack: Boolean,
): List<RowView> {
    val grid = editor.draft[editor.track]
    val cells = (0 until CELLS_PER_BAR).map { Cell(editor.bar, it) }
    val other = editor.track.other
    val otherNotes = if (showOtherTrack) editor.draft[other].notes else emptyList()
    val shift = editor.track.range.lowest.midi - other.range.lowest.midi
    val names = grid.rows.asReversed().map { rowName(it.pitch, editor.track.range) }
    return grid.rows.asReversed().mapIndexed { index, row ->
        val name = names[index]
        val echoes = otherNotes.filter { it.pitch.midi + shift == row.pitch.midi }
        RowView(
            pitch = row.pitch,
            name = name,
            inScale = row.inScale,
            spaceAbove = index > 0 && names[index - 1].register != name.register,
            cells = cells.map { grid.cellView(row.pitch, it) },
            otherTrack = cells.map { cell -> echoes.any { it.soundsIn(cell) } },
            actions = cells.map { grid.cellAction(editor.mode, row.pitch, it) },
        )
    }
}
