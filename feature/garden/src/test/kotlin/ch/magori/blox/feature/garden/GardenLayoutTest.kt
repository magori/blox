package ch.magori.blox.feature.garden

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The grid and the tray in the width of the screen: as drawn on 390 dp, narrowed on a narrower phone (V40). */
class GardenLayoutTest {
    @Test
    fun `the grid keeps cells of 22 dp where it fits, and narrows them to fit a narrower screen`() {
        // Given the column of a phone of 390 dp, and of 360 dp, 16 dp off each side
        val wide = gridCellFor(358.dp)
        val narrow = gridCellFor(328.dp)

        // Then 16 cells and the frame of the grid fill the width at most
        assertThat(wide).isEqualTo(22.dp)
        assertThat(narrow * 16 + GRID_FRAME * 2).isEqualTo(328.dp)
    }

    @Test
    fun `the information stands beside Tout effacer while it holds on 2 lines, whole words, under it otherwise`() {
        // Given a room of 150 px beside the button, lines of 20 px
        val room = 150

        // Then
        assertThat(holdsBeside(room, longestWord = 90, height = 40, lineHeight = 20)).isTrue()
        // A single line is trimmed of the leading above and below it: 2 lines are more than twice as high
        assertThat(holdsBeside(room, longestWord = 90, height = 44, lineHeight = 20)).isTrue()
        assertThat(holdsBeside(room, longestWord = 90, height = 60, lineHeight = 20)).isFalse()
        assertThat(holdsBeside(room, longestWord = 160, height = 40, lineHeight = 20)).isFalse()
    }
}
