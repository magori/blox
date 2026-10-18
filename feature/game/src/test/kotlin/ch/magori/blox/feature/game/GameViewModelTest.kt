package ch.magori.blox.feature.game

import androidx.lifecycle.viewmodel.MutableCreationExtras
import app.cash.turbine.test
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.GravityTable
import ch.magori.blox.core.game.rules.Randomizer
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameViewModelTest {
    private val seeds = mutableListOf<Long>()

    /** Real engines with rules made for tests: pieces T, I, O, … in turn, and one tick per row. */
    private val engineFactory = GameEngineFactory { seed ->
        seeds += seed
        GameEngine(
            ClassicRules.copy(gravityTable = oneTickPerRow),
            ScriptedRandomizer(PieceType.T, PieceType.I, PieceType.O),
        )
    }

    @Test
    fun `on creation the view model starts a game with the seed and start level of the route`() {
        // When
        val viewModel = createViewModel(GameRoute(startLevel = 5, seed = 42L))

        // Then
        assertThat(seeds).containsExactly(42L)
        assertThat(viewModel.uiState.value).isEqualTo(
            GameUiState(
                board = Board.empty(),
                piece = spawnedT,
                next = PieceType.I,
                level = Level(5),
                lines = Lines(0),
                score = 0,
                rotationSystem = ClassicRotationSystem,
            ),
        )
    }

    @Test
    fun `the first frame does nothing and a frame one sixtieth of a second later ticks the engine`() = runTest {
        // Given a game whose piece drops one row per tick
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        viewModel.uiState.test {
            assertThat(awaitItem().piece).isEqualTo(spawnedT)

            // When the first frame sets the time reference
            viewModel.onFrame(frameNanos = 1_000_000_000L)

            // Then nothing changes
            expectNoEvents()

            // When the next frame comes 1/60 s later
            viewModel.onFrame(frameNanos = 1_000_000_000L + ONE_SIXTIETH_NANOS)

            // Then one tick dropped the piece one row, and the new state is published
            assertThat(awaitItem().piece).isEqualTo(spawnedT.droppedBy(rows = 1))
        }
    }

    @Test
    fun `a long frame gives at most four ticks`() {
        // Given
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)

        // When a frame comes half a second later, 30 ticks of time
        viewModel.onFrame(frameNanos = 500_000_000L)

        // Then the piece dropped only four rows
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 4))
    }

    @Test
    fun `when frames start again the game loop is reset and the next frame gives no tick`() {
        // Given a game that ticked once, then left the screen
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)
        val beforeLeaving = viewModel.uiState.value

        // When the screen comes back ten seconds later
        viewModel.onFramesStarted()
        viewModel.onFrame(frameNanos = 10_000_000_000L)

        // Then that first frame only sets the time reference
        assertThat(viewModel.uiState.value).isEqualTo(beforeLeaving)
    }

    private fun createViewModel(route: GameRoute): GameViewModel =
        GameViewModel.Factory(engineFactory, ClassicRotationSystem).create(
            GameViewModel::class,
            MutableCreationExtras().apply { set(GameViewModel.RouteKey, route) },
        )

    private companion object {
        const val ONE_SIXTIETH_NANOS = 16_666_667L

        val oneTickPerRow = object : GravityTable {
            override fun ticksPerRow(level: Level): Int = 1
        }

        /** The T as it appears: pivot at (5, 0), spawn orientation. */
        val spawnedT = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 0))
    }

    private fun Piece.droppedBy(rows: Int): Piece = copy(pivot = Position(pivot.column, pivot.row + rows))

    /** Deals [pieces] in turn, again and again. */
    private class ScriptedRandomizer(vararg pieces: PieceType) : Randomizer {
        private val deck = pieces.toList()
        private var drawn = 0

        override fun next(previous: PieceType?): PieceType = deck[drawn++ % deck.size]
    }
}
