package ch.magori.blox.feature.garden

import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenPlacement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/** The touch of the grid: show the chosen piece, move it, place it, take a placed one back (docs, Écran). */
@ExtendWith(MainDispatcherExtension::class)
class GardenTapTest {
    private val kept = FakeGardenRepository()

    // Built in each test, once the main dispatcher is the test one: it reads the garden kept at once.
    private val viewModel by lazy { gardenViewModel(kept) }

    @Test
    fun `touching a free cell shows the chosen piece as a preview, its pivot on that cell, nothing placed`() {
        // When
        viewModel.tap(Position(5, 6))

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.preview).isEqualTo(T_AT_5_6)
        assertThat(state?.garden).isEqualTo(Garden.EMPTY)
    }

    @Test
    fun `a second touch on the preview places it, kept, and the preview is gone`() {
        // Given a T shown at (5, 6): its cells (4, 6), (5, 6), (6, 6), (5, 7)
        viewModel.tap(Position(5, 6))

        // When a touch on another cell of the preview
        viewModel.tap(Position(5, 7))

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.garden?.pieces).containsExactly(T_AT_5_6)
        assertThat(kept.state.value.pieces).containsExactly(T_AT_5_6)
        assertThat(state?.preview).isNull()
    }

    @Test
    fun `touching a placed piece takes it back as the preview, where it lay, in its type and orientation`() {
        // Given an L placed on its side
        val lying = ClassicRotationSystem.orientations(PieceType.L).first()
        val placed = Piece(PieceType.L, lying, Position(10, 3))
        kept.state.value = (Garden.EMPTY.place(placed) as GardenPlacement.Placed).garden

        // When
        viewModel.tap(placed.cells.first())

        // Then the garden no longer holds it, kept so, and it is the preview
        val state = viewModel.uiState.value
        assertThat(state?.garden?.pieces).isEmpty()
        assertThat(kept.state.value.pieces).isEmpty()
        assertThat(state?.preview).isEqualTo(placed)
    }

    @Test
    fun `Retirer takes the preview away, a piece taken back with it`() {
        // Given a piece taken back, the preview
        kept.state.value = (Garden.EMPTY.place(T_AT_5_6) as GardenPlacement.Placed).garden
        viewModel.tap(Position(5, 6))

        // When
        viewModel.remove()

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.preview).isNull()
        assertThat(state?.garden?.pieces).isEmpty()
    }

    @Test
    fun `Poser on a refused preview places nothing, and the preview stays where it is`() {
        // Given a T placed, and an O shown over it
        kept.state.value = (Garden.EMPTY.place(T_AT_5_6) as GardenPlacement.Placed).garden
        viewModel.choose(PieceType.O)
        viewModel.tap(Position(6, 5))

        // When
        viewModel.place()

        // Then
        val state = viewModel.uiState.value
        assertThat(state?.garden?.pieces).containsExactly(T_AT_5_6)
        assertThat(state?.aim).isEqualTo(Position(6, 5))
    }

    @Test
    fun `turning turns the chosen piece a quarter turn round the pivot of the preview`() {
        // Given
        viewModel.tap(Position(5, 6))

        // When
        viewModel.turn(Rotation.CLOCKWISE)

        // Then
        val turned = ClassicRotationSystem.rotate(PieceType.T, T_AT_5_6.orientation, Rotation.CLOCKWISE)
        assertThat(viewModel.uiState.value?.preview).isEqualTo(T_AT_5_6.copy(orientation = turned))
    }

    @Test
    fun `touching a refused preview over a note moves it there, placing nothing, taking nothing back`() {
        // Given a T placed, and an O shown over it: its cells (5, 5), (6, 5), (5, 6), (6, 6), the last two over the T
        kept.state.value = (Garden.EMPTY.place(T_AT_5_6) as GardenPlacement.Placed).garden
        viewModel.choose(PieceType.O)
        viewModel.tap(Position(6, 5))

        // When a touch on a cell of the preview over the T
        viewModel.tap(Position(5, 6))

        // Then the preview moves there, the T stays
        val state = viewModel.uiState.value
        assertThat(state?.garden?.pieces).containsExactly(T_AT_5_6)
        assertThat(state?.preview?.type).isEqualTo(PieceType.O)
        assertThat(state?.aim).isEqualTo(Position(5, 6))
    }

    @Test
    fun `turning a piece taken back keeps its pivot where it lay`() {
        // Given a T placed, taken back
        kept.state.value = (Garden.EMPTY.place(T_AT_5_6) as GardenPlacement.Placed).garden
        viewModel.tap(Position(4, 6))

        // When
        viewModel.turn(Rotation.COUNTERCLOCKWISE)

        // Then
        assertThat(viewModel.uiState.value?.aim).isEqualTo(T_AT_5_6.pivot)
    }

    private companion object {
        /** A T in its first orientation, its pivot on (5, 6). */
        val T_AT_5_6 = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 6))
    }
}
