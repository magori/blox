package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.Refusal
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/** The touch of the padlock: aim, place by a second touch, take a placed piece back (rules, Écran). */
@ExtendWith(MainDispatcherExtension::class)
class LocksmithTapTest {
    private val start = PadlockGame.start(Padlock.generate(level = 1, seed = 7))
    private val viewModel = locksmithViewModel(LocksmithRoute(level = 1, seed = 7))

    /** The first given piece, where the solution has it. */
    private val firstPiece: Piece = start.padlock.solution.first { it.type == start.given.first() }

    @Test
    fun `touching a cell aims the selected piece there, its pivot on that cell, nothing placed`() {
        // When
        viewModel.tap(Position(3, 3))

        // Then
        assertThat(viewModel.uiState.value.aim).isEqualTo(Position(3, 3))
        assertThat(viewModel.uiState.value.game).isEqualTo(start)
    }

    @Test
    fun `touching a cell outside the preview moves it there`() {
        // Given
        viewModel.tap(Position(1, 1))

        // When: far from every cell of the preview
        viewModel.tap(Position(8, 8))

        // Then
        assertThat(viewModel.uiState.value.aim).isEqualTo(Position(8, 8))
        assertThat(viewModel.uiState.value.game).isEqualTo(start)
    }

    @Test
    fun `a second touch on the preview places it and the preview is gone`() {
        // Given: the first piece turned as the solution has it, aimed where it goes
        viewModel.turnTo(firstPiece.orientation)
        viewModel.tap(firstPiece.pivot)

        // When: a touch on another cell of the preview
        viewModel.tap(firstPiece.cells.first { it != firstPiece.pivot })

        // Then
        assertThat(viewModel.uiState.value.game.placed).containsEntry(0, firstPiece)
        assertThat(viewModel.uiState.value.aim).isNull()
    }

    @Test
    fun `a refused second touch keeps the preview where it is and tells why`() {
        // Given: the preview with its pivot on the metal corner of the grid
        viewModel.tap(Position(0, 0))

        // When: a second touch on the pivot, a cell of the preview
        viewModel.tap(Position(0, 0))

        // Then
        assertThat(viewModel.uiState.value.aim).isEqualTo(Position(0, 0))
        assertThat(viewModel.uiState.value.events)
            .containsExactly(PadlockEvent.PlacementRefused(rank = 0, reason = Refusal.OUT_OF_HOLE))
    }

    @Test
    fun `moving or turning the preview after a refusal forgets the refusal, without counting a move`() {
        // Given: a refusal
        viewModel.tap(Position(0, 0))
        viewModel.placeAim()
        val refused = viewModel.uiState.value

        // When
        viewModel.tap(Position(4, 4))
        val moved = viewModel.uiState.value
        viewModel.placeAim()
        viewModel.turn(Rotation.CLOCKWISE)

        // Then
        assertThat(moved.events).isEmpty()
        assertThat(moved.move).isEqualTo(refused.move)
        assertThat(viewModel.uiState.value.events).isEmpty()
    }

    @Test
    fun `a refusal keeps the preview, the place button tells the reason`() {
        // Given: the preview on the metal corner of the grid
        viewModel.tap(Position(0, 0))

        // When
        viewModel.placeAim()

        // Then
        assertThat(viewModel.uiState.value.aim).isEqualTo(Position(0, 0))
        assertThat(viewModel.uiState.value.events)
            .containsExactly(PadlockEvent.PlacementRefused(rank = 0, reason = Refusal.OUT_OF_HOLE))
    }

    @Test
    fun `the place button places the preview`() {
        // Given
        viewModel.turnTo(firstPiece.orientation)
        viewModel.tap(firstPiece.pivot)

        // When
        viewModel.placeAim()

        // Then
        assertThat(viewModel.uiState.value.game.placed).containsEntry(0, firstPiece)
        assertThat(viewModel.uiState.value.aim).isNull()
    }

    @Test
    fun `the place button does nothing without a preview`() {
        // When
        viewModel.placeAim()

        // Then
        assertThat(viewModel.uiState.value.game).isEqualTo(start)
        assertThat(viewModel.uiState.value.events).isEmpty()
    }

    @Test
    fun `touching a placed piece takes it back as a preview where it lay`() {
        // Given
        placeFirstPiece()

        // When
        viewModel.tap(firstPiece.cells.last())

        // Then
        val state = viewModel.uiState.value
        assertThat(state.game.placed).isEmpty()
        assertThat(state.game.selected).isEqualTo(0)
        assertThat(state.aim).isEqualTo(firstPiece.pivot)
        assertThat(state.events).containsExactly(PadlockEvent.PieceTakenBack(rank = 0))
    }

    @Test
    fun `taking a placed piece back by its tile shows it as a preview where it lay`() {
        // Given
        placeFirstPiece()

        // When
        viewModel.takeBack(0)

        // Then
        assertThat(viewModel.uiState.value.game.placed).isEmpty()
        assertThat(viewModel.uiState.value.aim).isEqualTo(firstPiece.pivot)
    }

    @Test
    fun `touching the preview over a placed piece is refused as an overlap, nothing taken back`() {
        // Given: the first piece placed, the next one aimed beside it so that its preview covers a cell of it
        placeFirstPiece()
        val game = viewModel.uiState.value.game
        val aim = (0 until Padlock.SIZE).flatMap { row -> (0 until Padlock.SIZE).map { Position(it, row) } }
            .first { it !in firstPiece.cells && game.selectedAt(it)!!.cells.any(firstPiece.cells::contains) }
        viewModel.tap(aim)
        val shared = game.selectedAt(aim)!!.cells.first(firstPiece.cells::contains)

        // When: a second touch on a cell the preview shares with the placed piece, drawn over it
        viewModel.tap(shared)

        // Then
        val state = viewModel.uiState.value
        assertThat(state.game.placed).containsEntry(0, firstPiece)
        assertThat(state.events.single()).isInstanceOf(PadlockEvent.PlacementRefused::class.java)
    }

    @Test
    fun `once the padlock is open a touch changes nothing`() {
        // Given: every piece placed where the solution has it
        start.padlock.solution.forEach { piece ->
            viewModel.selectFree(piece.type)
            viewModel.turnTo(piece.orientation)
            viewModel.tap(piece.pivot)
            viewModel.placeAim()
        }
        val open = viewModel.uiState.value

        // When
        viewModel.tap(firstPiece.pivot)
        viewModel.takeBack(0)

        // Then
        assertThat(open.game.isOpen).isTrue
        assertThat(open.events).contains(PadlockEvent.PadlockOpened)
        assertThat(viewModel.uiState.value).isEqualTo(open)
    }

    @Test
    fun `restart clears the preview`() {
        // Given
        viewModel.tap(Position(3, 3))

        // When
        viewModel.restart()

        // Then
        assertThat(viewModel.uiState.value.aim).isNull()
    }

    private fun placeFirstPiece() {
        viewModel.turnTo(firstPiece.orientation)
        viewModel.tap(firstPiece.pivot)
        viewModel.placeAim()
    }
}
