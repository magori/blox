package ch.magori.blox.feature.garden

import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What each cell of the grid shows: free, a note of its piece, of the chord, the preview (docs, Écran, Grille). */
class GardenCellsTest {
    @Test
    fun `every cell of the 16 by 14 grid is shown, a free one free, one of a placed piece a note of its type`() {
        // Given an I on the top row
        val state = GardenUiState(I_ON_TOP)

        // When
        val cells = gardenCells(state)

        // Then
        assertThat(cells).hasSize(Garden.COLUMNS * Garden.ROWS)
        assertThat(cells[Position(0, 0)]).isInstanceOf(GardenCell.Note::class.java)
        assertThat((cells[Position(3, 0)] as GardenCell.Note).type).isEqualTo(PieceType.I)
        assertThat(cells[Position(4, 0)]).isEqualTo(GardenCell.Free)
    }

    @Test
    fun `a note is marked of the chord when its note belongs to the chord of its group`() {
        // Given an I on the bottom row, C4 in the joyful mood, from column 2 to 5: chord I (C E G) then V (G B D)
        val garden = Garden.parse(EMPTY_ROWS + "\n..IIII..........").withMood(GardenMood.JOYFUL)

        // When
        val cells = gardenCells(GardenUiState(garden))

        // Then
        assertThat(cells[Position(3, 13)]).isEqualTo(GardenCell.Note(PieceType.I, inChord = true))
        assertThat(cells[Position(4, 13)]).isEqualTo(GardenCell.Note(PieceType.I, inChord = false))
    }

    @Test
    fun `the preview covers its cells, each crossed out over a note, those off the grid left out`() {
        // Given the I on the top row, and a T shown with its pivot on (4, 0): its cells (3, 0), (4, 0), (5, 0), (4, 1)
        val state = GardenUiState(I_ON_TOP, chosen = PieceType.T, aim = Position(4, 0))

        // When
        val cells = gardenCells(state)

        // Then
        assertThat(cells[Position(3, 0)]).isEqualTo(GardenCell.Preview(PieceType.T, fits = false))
        assertThat(cells[Position(4, 0)]).isEqualTo(GardenCell.Preview(PieceType.T, fits = true))
        assertThat(cells[Position(4, 1)]).isEqualTo(GardenCell.Preview(PieceType.T, fits = true))
        assertThat(cells).hasSize(Garden.COLUMNS * Garden.ROWS)
    }

    private companion object {
        /** The 13 rows above the bottom one, free. */
        val EMPTY_ROWS: String = List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n")

        /** An I lying on the top row, columns 0 to 3. */
        val I_ON_TOP: Garden = Garden.parse(
            """
            IIII............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        ).withMood(GardenMood.JOYFUL)
    }
}
