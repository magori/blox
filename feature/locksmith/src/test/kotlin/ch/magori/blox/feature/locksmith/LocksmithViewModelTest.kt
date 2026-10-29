package ch.magori.blox.feature.locksmith

import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.Refusal
import ch.magori.blox.core.locksmith.turnedTo
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import kotlinx.coroutines.MainScope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class LocksmithViewModelTest {
    private val start = PadlockGame.start(Padlock.ofLevel(2))
    private val viewModel = locksmithViewModel(LocksmithRoute(level = 2))

    /** The first given piece, where the solution has it. */
    private val firstPiece = start.padlock.solution.first { it.type == start.given.first() }

    @Test
    fun `the screen opens on padlock n of its route, the same for everyone, nothing placed`() {
        // Then
        val state = viewModel.uiState.value
        assertThat(state.level).isEqualTo(2)
        assertThat(state.game).isEqualTo(start)
        assertThat(state.events).isEmpty()
        assertThat(state.hint).isNull()
    }

    @Test
    fun `selecting a piece selects it in the game`() {
        // When
        viewModel.select(1)

        // Then
        assertThat(viewModel.uiState.value.game).isEqualTo(start.select(1))
    }

    @Test
    fun `turning turns the selected piece in the game`() {
        // When
        viewModel.turn(Rotation.CLOCKWISE)

        // Then
        assertThat(viewModel.uiState.value.game).isEqualTo(start.turn(Rotation.CLOCKWISE))
    }

    @Test
    fun `a turn is told by the screen until the next action`() {
        // When the piece is turned
        viewModel.turn(Rotation.CLOCKWISE)
        val turned = viewModel.uiState.value.turned

        // And then aimed
        viewModel.tap(Position(3, 3))

        // Then
        assertThat(turned).isTrue
        assertThat(viewModel.uiState.value.turned).isFalse
    }

    @Test
    fun `a placement gives the next game and its events`() {
        // When
        viewModel.turnTo(firstPiece.orientation)
        viewModel.place(firstPiece.pivot)

        // Then
        val placed = start.turnedTo(firstPiece.orientation).place(firstPiece.pivot)
        assertThat(viewModel.uiState.value.game).isEqualTo(placed.game)
        assertThat(viewModel.uiState.value.events).isEqualTo(placed.events)
    }

    @Test
    fun `a refused placement tells its reason`() {
        // When: the pivot in the corner, on the metal
        viewModel.place(Position(0, 0))

        // Then
        assertThat(viewModel.uiState.value.events)
            .containsExactly(PadlockEvent.PlacementRefused(rank = 0, reason = Refusal.OUT_OF_HOLE))
    }

    @Test
    fun `two refusals in a row are two moves the screen can tell apart`() {
        // When
        viewModel.place(Position(0, 0))
        val first = viewModel.uiState.value
        viewModel.place(Position(0, 0))

        // Then
        assertThat(viewModel.uiState.value.events).isEqualTo(first.events)
        assertThat(viewModel.uiState.value.move).isEqualTo(first.move + 1)
    }

    @Test
    fun `a hint shows where a piece fits until the next placement`() {
        // Given
        val expected = (start.hint().events.single() as PadlockEvent.HintGiven).hint as Hint.Fit

        // When
        viewModel.hint()
        val hinted = viewModel.uiState.value.hint
        viewModel.select(expected.rank)
        viewModel.turnTo(expected.piece.orientation)
        viewModel.place(expected.piece.pivot)

        // Then
        assertThat(hinted).isEqualTo(expected)
        assertThat(viewModel.uiState.value.hint).isNull()
    }

    @Test
    fun `taking a piece back gives the next game and its event`() {
        // Given
        viewModel.turnTo(firstPiece.orientation)
        viewModel.place(firstPiece.pivot)
        val placed = viewModel.uiState.value.game

        // When
        viewModel.takeBack(0)

        // Then
        assertThat(viewModel.uiState.value.game).isEqualTo(placed.takeBack(0).game)
        assertThat(viewModel.uiState.value.events).containsExactly(PadlockEvent.PieceTakenBack(rank = 0))
    }

    @Test
    fun `restart keeps the padlock and starts it over, hint and events cleared`() {
        // Given
        viewModel.place(Position(4, 4))
        viewModel.hint()

        // When
        viewModel.restart()

        // Then
        assertThat(viewModel.uiState.value.game).isEqualTo(start)
        assertThat(viewModel.uiState.value.hint).isNull()
        assertThat(viewModel.uiState.value.events).isEmpty()
    }

    @Test
    fun `the factory builds the padlock and the level of the route the entry hands it`() {
        // Given
        val extras = MutableCreationExtras().apply {
            set(LocksmithViewModel.RouteKey, LocksmithRoute(level = 3))
        }

        // When
        val built = LocksmithViewModel.Factory(
            FakeSoundEffectPlayer(),
            FakeUserPreferencesRepository(),
            FakePadlockProgressRepository(),
            MainScope(),
            ClearedProgressRelay(),
        ).create(LocksmithViewModel::class, extras)

        // Then
        assertThat(built.uiState.value.level).isEqualTo(3)
        assertThat(built.uiState.value.game).isEqualTo(PadlockGame.start(Padlock.ofLevel(3)))
    }

    @Test
    fun `the factory fails clearly without the route of the entry`() {
        assertThatIllegalArgumentException().isThrownBy {
            LocksmithViewModel.Factory(
                FakeSoundEffectPlayer(),
                FakeUserPreferencesRepository(),
                FakePadlockProgressRepository(),
                MainScope(),
                ClearedProgressRelay(),
            ).create(LocksmithViewModel::class, CreationExtras.Empty)
        }
    }

    @Test
    fun `the padlock shown after the progress is cleared tells it, once, until the first action`() {
        // Given the progress cleared on the screen of the padlocks, padlock 1 shown next
        val cleared = ClearedProgressRelay()
        cleared.handOver()
        val first = locksmithViewModel(LocksmithRoute(level = 1), cleared = cleared)

        // When it is shown, then shown again (a rotation), and a second padlock is shown
        first.showClearedProgress()
        val told = first.uiState.value.cleared
        first.showClearedProgress()
        val second = locksmithViewModel(LocksmithRoute(level = 1), cleared = cleared)
        second.showClearedProgress()

        // Then the first tells it, the second not; the first action makes it give way to the game
        assertThat(told).isTrue
        assertThat(first.uiState.value.cleared).isTrue
        assertThat(second.uiState.value.cleared).isFalse
        first.select(1)
        assertThat(first.uiState.value.cleared).isFalse
    }

    @Test
    fun `a placement or a restart, as any first action, makes the cleared progress give way to the game`() {
        // Given two padlocks 1, each shown after a clear
        val tellingOf = {
            val cleared = ClearedProgressRelay().apply { handOver() }
            locksmithViewModel(LocksmithRoute(level = 1), cleared = cleared).apply { showClearedProgress() }
        }
        val placing = tellingOf()
        val restarting = tellingOf()

        // When a piece is placed on the first, the second is started over
        placing.place(Position(1, 1))
        restarting.restart()

        // Then
        assertThat(placing.uiState.value.cleared).isFalse
        assertThat(restarting.uiState.value.cleared).isFalse
    }

    @Test
    fun `padlock 1, open under the screen of the padlocks, starts over once the progress is cleared, and tells it`() {
        // Given padlock 1 open, its view model kept under the screen of the padlocks
        val cleared = ClearedProgressRelay()
        val first = locksmithViewModel(LocksmithRoute(level = 1), cleared = cleared)
        val one = PadlockGame.start(Padlock.ofLevel(1))
        one.padlock.solution.forEach { first.placing(it) }
        assertThat(first.uiState.value.game.isOpen).isTrue

        // When the progress is cleared there, and padlock 1 shows again
        cleared.handOver()
        first.showClearedProgress()

        // Then padlock 1 starts over, nothing placed, and tells the progress cleared
        val state = first.uiState.value
        assertThat(state.game).isEqualTo(one)
        assertThat(state.outcome).isNull()
        assertThat(state.cleared).isTrue
    }
}
