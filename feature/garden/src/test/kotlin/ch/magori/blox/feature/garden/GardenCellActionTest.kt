package ch.magori.blox.feature.garden

import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** What TalkBack announces a touch of a cell does: as `GardenViewModel.tap` does it. */
class GardenCellActionTest {
    @Test
    fun `a touch places a preview that fits, moves a refused one, takes a note back, shows the piece on a free cell`() {
        assertThat(cellActionOf(GardenCell.Preview(PieceType.T, fits = true)))
            .isEqualTo(R.string.feature_garden_action_place)
        assertThat(cellActionOf(GardenCell.Preview(PieceType.T, fits = false)))
            .isEqualTo(R.string.feature_garden_action_move)
        assertThat(cellActionOf(GardenCell.Note(PieceType.T))).isEqualTo(R.string.feature_garden_action_take_back)
        assertThat(cellActionOf(GardenCell.Free)).isEqualTo(R.string.feature_garden_action_show)
    }
}
