package ch.magori.blox.feature.game

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import app.cash.turbine.test
import ch.magori.blox.core.data.RecordResult
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.GravityTable
import ch.magori.blox.core.game.rules.Randomizer
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.gameover.GameOverUiState
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class GameViewModelTest {
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val seeds = mutableListOf<Long>()

    /** The record before the game: the fake answers with it, as a new record, to every submit. */
    private val highScores = FakeHighScoreRepository(RecordResult(previous = 100, isNewRecord = true))

    /** Real engines with rules made for tests: pieces T, I, O, … in turn, and one tick per row. */
    private val engineFactory = GameEngineFactory { seed ->
        seeds += seed
        GameEngine(
            ClassicRules.copy(gravityTable = oneTickPerRow),
            ScriptedRandomizer(PieceType.T, PieceType.I, PieceType.O),
        )
    }

    @Test
    fun `while full rows are cleared the screen shows the locked board and no piece`() {
        // Given three pieces that fill row 19 together: columns 0 to 3, 4 to 7, then 8 and 9
        val rowFiller = SpawnInTurn(
            Orientation(setOf(Position(-5, 0), Position(-4, 0), Position(-3, 0), Position(-2, 0))),
            Orientation(setOf(Position(-1, 0), Position(0, 0), Position(1, 0), Position(2, 0))),
            Orientation(setOf(Position(3, -1), Position(4, -1), Position(3, 0), Position(4, 0))),
        )
        val factory = GameEngineFactory {
            GameEngine(
                ClassicRules.copy(rotationSystem = rowFiller, gravityTable = oneTickPerRow),
                ScriptedRandomizer(PieceType.I, PieceType.I, PieceType.O),
            )
        }
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), factory)

        // When the game is played tick by tick until row 19 is full
        val clearing = generateSequence(0L) { it + ONE_SIXTIETH_NANOS }
            .take(MAX_TICKS)
            .map { frameNanos ->
                viewModel.onFrame(frameNanos)
                viewModel.uiState.value
            }
            .first { state -> (0..9).all { state.board[Position(it, 19)] is Cell.Filled } }

        // Then the board still holds the full row, and no piece is shown
        assertThat(clearing.board).isEqualTo(
            Board.parse(
                """
                ........OO
                IIIIIIIIOO
                """,
            ),
        )
        assertThat(clearing.piece).isNull()
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
                score = Points(0),
                rotationSystem = ClassicRotationSystem,
                record = 100,
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

    @Test
    fun `a control change is given to the engine before the ticks of the next frame`() {
        // Given a game whose first frame set the time reference
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)

        // When left is pressed, then the next frame comes
        viewModel.onControl(ControlChange.Pressed(ControlButton.LEFT))
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then the piece moved one column to the left on that tick
        assertThat(viewModel.uiState.value.piece?.pivot?.column).isEqualTo(spawnedT.pivot.column - 1)
    }

    @Test
    fun `every control change between two frames is given to the engine, in order, none lost`() {
        // Given a game whose first frame set the time reference
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)

        // When A is pressed and released before the next frame comes
        viewModel.onControl(ControlChange.Pressed(ControlButton.A))
        viewModel.onControl(ControlChange.Released(ControlButton.A))
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then the press was not lost: the piece turned clockwise on that tick
        val turned = ClassicRotationSystem.candidates(spawnedT, Rotation.CLOCKWISE).single()
        assertThat(viewModel.uiState.value.piece?.orientation).isEqualTo(turned.orientation)
    }

    @Test
    fun `at the game over the score is submitted once and the screen gets the final values and the record result`() {
        // Given the final state of the same game, played by the engine alone
        val final = finalStateOfTheTestGame()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When the game is played frame by frame until it is over
        playUntilGameOver(viewModel)

        // Then
        assertThat(highScores.submitted).containsExactly(final.progress.score.value)
        assertThat(viewModel.uiState.value.gameOver).isEqualTo(
            GameOverUiState(
                score = final.progress.score.value,
                lines = final.progress.lines.value,
                level = final.progress.level.value,
                fourLineClears = final.progress.statistics.fourLineClears,
                ticksPlayed = final.tickCount,
                newRecord = true,
            ),
        )
    }

    @Test
    fun `once the game is over the frames change nothing and the score is not submitted again`() {
        // Given a game over
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        val lastFrame = playUntilGameOver(viewModel)
        val over = viewModel.uiState.value

        // When frames keep coming for a second
        (1..60).forEach { frame -> viewModel.onFrame(lastFrame + frame * ONE_SIXTIETH_NANOS) }

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(over)
        assertThat(highScores.submitted).hasSize(1)
    }

    @Test
    fun `when the records cannot be written the game over still shows, without a new record`() {
        // Given records that fail to write
        highScores.failure = IOException("disk full")
        val final = finalStateOfTheTestGame()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When
        playUntilGameOver(viewModel)

        // Then
        assertThat(viewModel.uiState.value.gameOver).isEqualTo(final.toGameOverUiState(newRecord = false))
    }

    @Test
    fun `while the score is being submitted the game over waits, then shows once with the answer`() = runTest {
        // Given records that take their time to write
        val writing = CompletableDeferred<Unit>()
        highScores.gate = writing
        val final = finalStateOfTheTestGame()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        val lastFrame = playUntilSubmitted(viewModel)

        // When frames keep coming for a second while the score is being written
        (1..60).forEach { frame -> viewModel.onFrame(lastFrame + frame * ONE_SIXTIETH_NANOS) }

        // Then the game over waits, and the score was submitted once
        assertThat(viewModel.uiState.value.gameOver).isNull()
        assertThat(highScores.submitted).hasSize(1)

        viewModel.uiState.test {
            assertThat(awaitItem().gameOver).isNull()

            // When the records answer
            writing.complete(Unit)

            // Then the game over shows once, with the answer, and further frames publish nothing
            assertThat(awaitItem().gameOver).isEqualTo(final.toGameOverUiState(newRecord = true))
            (61..120).forEach { frame -> viewModel.onFrame(lastFrame + frame * ONE_SIXTIETH_NANOS) }
            expectNoEvents()
        }
    }

    @Test
    fun `the score is saved even when the view model is cleared while it is being submitted`() {
        // Given a game whose score is being written, in a view model store like the navigation entry's
        val writing = CompletableDeferred<Unit>()
        highScores.gate = writing
        val final = finalStateOfTheTestGame()
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(
            store,
            GameViewModel.Factory(engineFactory, highScores),
            routeExtras(GameRoute(startLevel = 0, seed = 1L)),
        )[GameViewModel::class]
        playUntilSubmitted(viewModel)

        // When the entry leaves the stack before the records answer, then they answer
        store.clear()
        writing.complete(Unit)

        // Then the score was saved all the same
        assertThat(highScores.saved).containsExactly(final.progress.score.value)
    }

    @Test
    fun `the screen draws with the rotation system of the rules of the engine`() {
        // Given an engine whose rules have their own rotation system
        val ownRotationSystem = SpawnInTurn(ClassicRotationSystem.spawnOrientation(PieceType.T))
        val factory = GameEngineFactory {
            GameEngine(ClassicRules.copy(rotationSystem = ownRotationSystem), ScriptedRandomizer(PieceType.T))
        }

        // When
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), factory)

        // Then
        assertThat(viewModel.uiState.value.rotationSystem).isSameAs(ownRotationSystem)
    }

    @Test
    fun `on pause the game is paused`() {
        // Given a game under way
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When
        viewModel.onPause()

        // Then
        assertThat(viewModel.uiState.value.paused).isTrue()
    }

    @Test
    fun `while paused the frames change nothing, not even the duration of the game`() {
        // Given a game played for half a second, then paused
        val final = finalStateOfTheTestGame()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        (0..30).forEach { frame -> viewModel.onFrame(frame * ONE_SIXTIETH_NANOS) }
        viewModel.onPause()
        val paused = viewModel.uiState.value

        // When frames keep coming for two seconds
        (31..150).forEach { frame -> viewModel.onFrame(frame * ONE_SIXTIETH_NANOS) }

        // Then nothing changed; and once resumed, the game lasts as long as without the pause
        assertThat(viewModel.uiState.value).isEqualTo(paused)
        viewModel.onResume()
        playUntil(viewModel, "the screen shows the game over", firstFrame = 151 * ONE_SIXTIETH_NANOS) {
            viewModel.uiState.value.gameOver != null
        }
        assertThat(viewModel.uiState.value.gameOver?.ticksPlayed).isEqualTo(final.tickCount)
    }

    @Test
    fun `the controls received while paused are ignored, also after the game resumes`() {
        // Given a paused game whose first frame set the time reference
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onPause()

        // When left is pressed during the pause, then the game resumes and the next frame comes
        viewModel.onControl(ControlChange.Pressed(ControlButton.LEFT))
        viewModel.onResume()
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then the piece only fell: left was not pressed for the game
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 1))
    }

    @Test
    fun `on resume the game goes on exactly where it stood, without catching up the pause`() {
        // Given a game that ticked once, then was paused for two seconds of frames
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)
        viewModel.onPause()
        (2..121).forEach { frame -> viewModel.onFrame(frame * ONE_SIXTIETH_NANOS) }

        // When it resumes, and the next frame comes
        viewModel.onResume()
        viewModel.onFrame(frameNanos = 122 * ONE_SIXTIETH_NANOS)

        // Then it is no longer paused, and that frame gave one tick, not a catch-up of the pause
        assertThat(viewModel.uiState.value.paused).isFalse()
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 2))
    }

    @Test
    fun `once the game is over a pause does nothing`() {
        // Given a game over
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        playUntilGameOver(viewModel)
        val over = viewModel.uiState.value

        // When
        viewModel.onPause()

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(over)
    }

    @Test
    fun `pausing a game already paused does nothing`() = runTest {
        // Given a paused game
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onPause()

        viewModel.uiState.test {
            val paused = awaitItem()

            // When it is paused again (the screen leaves the front during the pause)
            viewModel.onPause()

            // Then nothing is published, and the game is still paused
            expectNoEvents()
            assertThat(viewModel.uiState.value).isEqualTo(paused)
            assertThat(paused.paused).isTrue()
        }
    }

    @Test
    fun `the record shown is the one of the records, its real value, updated when it changes`() = runTest {
        // Given a record of 100
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        viewModel.uiState.test {
            assertThat(awaitItem().record).isEqualTo(100)

            // When the record changes, beyond what the screen can show
            highScores.record.value = 1_234_567

            // Then
            assertThat(awaitItem().record).isEqualTo(1_234_567)
        }
    }

    @Test
    fun `records that fail while they are read leave the game going, with the last record known`() {
        // Given records that give 100, then fail
        highScores.highScoreFlow = flow {
            emit(100)
            throw IOException("records unreadable")
        }

        // When the game goes on
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(viewModel.uiState.value.record).isEqualTo(100)
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 1))
    }

    @Test
    fun `until the records give it the record is unknown, not 0`() {
        // Given records that have not answered yet
        highScores.highScoreFlow = MutableSharedFlow()

        // When
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(viewModel.uiState.value.record).isNull()
    }

    @Test
    fun `the record stays shown when the frames move the game`() {
        // Given a record of 100
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When frames tick the engine
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(viewModel.uiState.value.record).isEqualTo(100)
    }

    @Test
    fun `after a pause without frames, the frames start again without catching up the time paused`() {
        // Given a game that ticked once, paused for ten seconds without any frame (the screen stops them)
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)
        viewModel.onPause()

        // When the player resumes, the screen starts its frames again, and they come
        viewModel.onResume()
        viewModel.onFramesStarted()
        viewModel.onFrame(frameNanos = 10_000_000_000L)
        val afterFirstFrame = viewModel.uiState.value.piece
        viewModel.onFrame(frameNanos = 10_000_000_000L + ONE_SIXTIETH_NANOS)

        // Then the first frame gave no tick, the next one a single tick
        assertThat(afterFirstFrame).isEqualTo(spawnedT.droppedBy(rows = 1))
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 2))
    }

    @Test
    fun `frames started again while paused change nothing, the game stays paused until it resumes`() {
        // Given a paused game, whose screen stopped then started again (ON_STOP, then ON_START)
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onPause()
        val paused = viewModel.uiState.value

        // When frames come again
        viewModel.onFramesStarted()
        (1..60).forEach { frame -> viewModel.onFrame(10_000_000_000L + frame * ONE_SIXTIETH_NANOS) }

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(paused)
        assertThat(viewModel.uiState.value.paused).isTrue()
    }

    private fun createViewModel(
        route: GameRoute,
        factory: GameEngineFactory = engineFactory,
    ): GameViewModel = GameViewModel.Factory(factory, highScores).create(
        GameViewModel::class,
        routeExtras(route),
    )

    /** The creation extras a navigation entry gives: the [route] of the game. */
    private fun routeExtras(route: GameRoute): CreationExtras =
        MutableCreationExtras().apply { set(GameViewModel.RouteKey, route) }

    private companion object {
        const val ONE_SIXTIETH_NANOS = 16_666_667L

        /** Enough ticks for the test game to fill the board and end, entry delays included. */
        const val MAX_TICKS = 5_000

        val oneTickPerRow = object : GravityTable {
            override fun ticksPerRow(level: Level): Ticks = Ticks(1)
        }

        /** The T as it appears: pivot at (5, 0), spawn orientation. */
        val spawnedT = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 0))
    }

    private fun Piece.droppedBy(rows: Int): Piece = copy(pivot = Position(pivot.column, pivot.row + rows))

    /** The state in which the test game ends, played tick by tick by its engine alone, without frames. */
    private fun finalStateOfTheTestGame(): GameState {
        val engine = engineFactory.create(seed = 1L)
        var update = engine.start(Level(0))
        repeat(MAX_TICKS) {
            update = engine.tick(update.state)
            if (GameEvent.GameOver in update.events) return update.state
        }
        error("The test game did not end within $MAX_TICKS ticks")
    }

    /** Gives [viewModel] one frame per tick until its screen shows the game over; the time of that last frame. */
    private fun playUntilGameOver(viewModel: GameViewModel): Long =
        playUntil(viewModel, "the screen shows the game over") { viewModel.uiState.value.gameOver != null }

    /** Gives [viewModel] one frame per tick until it submitted the score; the time of that last frame. */
    private fun playUntilSubmitted(viewModel: GameViewModel): Long =
        playUntil(viewModel, "the score is submitted") { highScores.submitted.isNotEmpty() }

    private fun playUntil(
        viewModel: GameViewModel,
        what: String,
        firstFrame: Long = 0L,
        done: () -> Boolean,
    ): Long = generateSequence(firstFrame) { it + ONE_SIXTIETH_NANOS }
        .take(MAX_TICKS)
        .onEach(viewModel::onFrame)
        .firstOrNull { done() }
        ?: error("Not done within $MAX_TICKS frames: $what")

    /** A rotation system for tests: each piece appears in the next of [orientations], in turn. */
    private class SpawnInTurn(vararg orientations: Orientation) : RotationSystem {
        private val spawns = orientations.toList()
        private var spawned = 0

        override fun orientations(type: PieceType): List<Orientation> = spawns

        override fun spawnOrientation(type: PieceType): Orientation = spawns[spawned++ % spawns.size]

        override fun candidates(
            piece: Piece,
            rotation: Rotation,
        ): List<Piece> = emptyList()
    }

    /** Deals [pieces] in turn, again and again. */
    private class ScriptedRandomizer(vararg pieces: PieceType) : Randomizer {
        private val deck = pieces.toList()
        private var drawn = 0

        override fun next(previous: PieceType?): PieceType = deck[drawn++ % deck.size]
    }
}
