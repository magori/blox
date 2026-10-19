package ch.magori.blox.feature.composer.grid

import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.GridNote
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CellViewTest {
    private val e4 = pitch("E4")

    @Test
    fun `a cell shows the attack of a note, its held part, or nothing`() {
        // Given a note of E4 over the first two cells
        val grid = melody(GridNote(e4, start = 0, length = 4))

        // When
        val attack = grid.cellView(e4, Cell(bar = 0, eighth = 0))
        val held = grid.cellView(e4, Cell(bar = 0, eighth = 1))
        val empty = grid.cellView(e4, Cell(bar = 0, eighth = 2))

        // Then
        assertThat(attack).isEqualTo(CellView(CellPart.ATTACK, CellPart.HELD))
        assertThat(held).isEqualTo(CellView(CellPart.HELD, CellPart.HELD))
        assertThat(empty).isEqualTo(CellView(CellPart.SILENT, CellPart.SILENT))
    }

    @Test
    fun `a cell is split in two sixteenth notes when its halves do not show one note`() {
        // Given two sixteenth notes in the first cell, a sixteenth note in the second, a note ending in the middle of the
        // fourth, and an eighth note in the fifth
        val grid = melody(
            GridNote(e4, start = 0, length = 1),
            GridNote(e4, start = 1, length = 1),
            GridNote(e4, start = 2, length = 1),
            GridNote(e4, start = 4, length = 3),
            GridNote(e4, start = 8, length = 2),
        )

        // When
        val split = (0..4).map { grid.cellView(e4, Cell(bar = 0, eighth = it)).isSplit }

        // Then
        assertThat(split).containsExactly(true, true, false, true, false)
    }

    private fun melody(vararg notes: GridNote): TrackGrid = ScoreDraft.new().melody.copy(notes = notes.toList())
}
