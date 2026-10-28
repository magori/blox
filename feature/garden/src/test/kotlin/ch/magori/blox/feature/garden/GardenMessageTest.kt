package ch.magori.blox.feature.garden

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenPlacement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The message under the grid: what can be done, or why the preview cannot be placed (docs, Écran). */
class GardenMessageTest {
    @Test
    fun `without a preview, an empty garden says to choose a piece and touch the grid`() {
        assertThat(messageOf(GardenUiState(Garden.EMPTY))).isEqualTo(R.string.feature_garden_message_start)
    }

    @Test
    fun `without a preview, a garden with pieces says a note can be taken back`() {
        assertThat(messageOf(GardenUiState(A_GARDEN))).isEqualTo(R.string.feature_garden_message_garden)
    }

    @Test
    fun `a preview that fits says how to place it`() {
        val state = GardenUiState(A_GARDEN, aim = Position(10, 10))

        assertThat(messageOf(state)).isEqualTo(R.string.feature_garden_message_preview)
    }

    @Test
    fun `a preview over a note says a cell is taken`() {
        // Given an O shown over the T
        val state = GardenUiState(A_GARDEN, chosen = PieceType.O, aim = Position(5, 6))

        assertThat(messageOf(state)).isEqualTo(R.string.feature_garden_message_overlap)
    }

    @Test
    fun `a preview off the grid says so`() {
        // Given an I lying across the right edge
        val lying = GardenUiState(Garden.EMPTY, chosen = PieceType.I, aim = Position(Garden.COLUMNS - 1, 0))

        assertThat(messageOf(lying)).isEqualTo(R.string.feature_garden_message_out_of_grid)
    }

    private companion object {
        /** A T in its first orientation, its pivot on (5, 6). */
        val A_GARDEN: Garden = (
            Garden.EMPTY.place(
                Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 6)),
            ) as GardenPlacement.Placed
            ).garden
    }
}
