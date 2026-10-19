package ch.magori.blox.feature.composer.grid

import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.model.notes
import ch.magori.blox.feature.composer.model.pitch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CellActionTest {
    // On the line of E4, in the first bar: a rest of a beat, then a note of E4 held over three cells.
    private val grid = TrackGrid.playing(Track.MELODY, ScoreDraft.new().scale, notes("r:1.0 E4:1.5"))
    private val e4 = pitch("E4")

    private fun actions(mode: EditMode) = (0 until 8).map { grid.cellAction(mode, e4, Cell(bar = 0, eighth = it)) }

    @Test
    fun `in Place mode touching a cell places a note on an empty cell, and erases the note it shows otherwise`() {
        // When
        val actions = actions(EditMode.PLACE)

        // Then
        assertThat(actions).containsExactly(
            CellAction.PLACE,
            CellAction.PLACE,
            CellAction.ERASE,
            CellAction.ERASE,
            CellAction.ERASE,
            CellAction.PLACE,
            CellAction.PLACE,
            CellAction.PLACE,
        )
    }

    @Test
    fun `in Lengthen mode a touch places, brings back to one cell, shortens or lengthens, as the model does`() {
        // When
        val actions = actions(EditMode.LENGTHEN)

        // Then: no note on its left places one; the attack brings the note back to one cell; a held cell shortens it;
        // a cell after it lengthens it
        assertThat(actions).containsExactly(
            CellAction.PLACE,
            CellAction.PLACE,
            CellAction.ONE_CELL,
            CellAction.SHORTEN,
            CellAction.SHORTEN,
            CellAction.LENGTHEN,
            CellAction.LENGTHEN,
            CellAction.LENGTHEN,
        )
    }

    @Test
    fun `in Lengthen mode a note of the bar before is not lengthened, a note is placed`() {
        // Given a note in the first bar only
        val cell = Cell(bar = 1, eighth = 3)

        // When
        val action = grid.cellAction(EditMode.LENGTHEN, e4, cell)

        // Then
        assertThat(action).isEqualTo(CellAction.PLACE)
    }
}
