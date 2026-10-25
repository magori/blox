package ch.magori.blox.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.board.next.nextPieceCells
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.ScriptedRandomizer
import ch.magori.blox.core.game.engine.spawned
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
import ch.magori.blox.core.game.rules.ClassicScoringRule
import ch.magori.blox.core.game.rules.GravityTable
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.core.game.rules.ScoringRule
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.core.haptics.Haptic
import ch.magori.blox.core.music.Music
import ch.magori.blox.core.music.MusicFile
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.ScoreId
import ch.magori.blox.core.music.Scores
import ch.magori.blox.core.music.SoundEffects
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.gameover.GameOverUiState
import ch.magori.blox.feature.game.navigation.GameRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException

class GameViewModelTest {
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val seeds = mutableListOf<Long>()

    /** The record before the game: 100, which the test game beats (see [hundredPointsPerLock]). */
    private val highScores = FakeHighScoreRepository(initial = 100)

    /**
     * The settings of the player: Korobeiniki, unless a test chooses another music, and the vibrations and the sound
     * effects turned on (they are off by default), unless a test turns them off.
     */
    private val preferences =
        FakeUserPreferencesRepository(UserPreferences.DEFAULT.copy(vibrations = true, soundEffects = true))

    private val music = FakeMusicPlayer()

    private val haptics = FakeHaptics()

    private val soundEffects = FakeSoundEffectPlayer()

    /**
     * The music of each choice, as `app` gives it: Korobeiniki and the minuet, nothing else, unless a test chooses
     * otherwise.
     */
    private var musicOf: suspend (MusicChoice) -> Music? = { choice ->
        mapOf(MusicChoice.KOROBEINIKI to Scores.korobeiniki, MusicChoice.MINUET to Scores.minuet)[choice]
    }

    /** The score of the music of the player chosen for the games, as `app` reads it: none, unless a test chooses one. */
    private var gameMusic: suspend () -> Score? = { null }

    /**
     * Real engines with rules made for tests: pieces T, I, O, … in turn, one tick per row, and 100 points per
     * piece locked, so that the test game, which clears no line, beats the record.
     */
    private val engineFactory = GameEngineFactory { seed ->
        seeds += seed
        GameEngine(
            ClassicRules.copy(gravityTable = oneTickPerRow, scoringRule = hundredPointsPerLock),
            ScriptedRandomizer.cycling(PieceType.T, PieceType.I, PieceType.O),
        )
    }

    @Test
    fun `while full rows are cleared the screen shows the locked board and no piece`() {
        // Given three pieces that fill row 19 together: columns 0 to 3, 4 to 7, then 8 and 9
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), oneLineGame)

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
                IIIIJJJJOO
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
                next = nextPieceCells(PieceType.I, ClassicRotationSystem),
                level = Level(5),
                lines = Lines(0),
                score = Points(0),
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
                score = final.progress.score,
                lines = final.progress.lines,
                level = final.progress.level,
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
    fun `a game over below the record shows no new record`() {
        // Given a record that no game can beat
        val unbeatable = FakeHighScoreRepository(initial = Int.MAX_VALUE)
        val final = finalStateOfTheTestGame()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), records = unbeatable)

        // When
        playUntilGameOver(viewModel)

        // Then the score was submitted, and the screen shows no new record
        assertThat(unbeatable.submitted).containsExactly(final.progress.score.value)
        assertThat(viewModel.uiState.value.gameOver).isEqualTo(final.toGameOverUiState(newRecord = false))
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
    fun `a programming error while the score is submitted is not swallowed`() {
        // Given records whose writing fails with a bug, not with a file that cannot be written
        highScores.failure = IllegalStateException("a bug in the records")

        // When the game is played until its score is submitted
        val failure = runCatching {
            runTest { playUntilSubmitted(createViewModel(GameRoute(startLevel = 0, seed = 1L))) }
        }.exceptionOrNull()

        // Then the bug reaches the uncaught exceptions, as it would crash the application
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug in the records")
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
        val viewModel = createViewModelIn(store)
        playUntilSubmitted(viewModel)

        // When the entry leaves the stack before the records answer, then they answer
        store.clear()
        writing.complete(Unit)

        // Then the score was saved all the same
        assertThat(highScores.saved).containsExactly(final.progress.score.value)
    }

    @Test
    fun `the screen gets the cells of the next piece in its spawn orientation`() {
        // Given an engine whose rules spawn the I upright, unlike the classic rules
        val uprightI = SpawnAs(
            PieceType.T to ClassicRotationSystem.spawnOrientation(PieceType.T),
            PieceType.I to ClassicRotationSystem.orientations(PieceType.I).first(),
        )
        val factory = GameEngineFactory {
            GameEngine(
                ClassicRules.copy(rotationSystem = uprightI),
                // The piece that appears, and the next one shown.
                ScriptedRandomizer(PieceType.T, PieceType.I),
            )
        }

        // When
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), factory)

        // Then the next I is drawn upright, its cells brought to the origin
        assertThat(viewModel.uiState.value.next).isEqualTo(
            NextPieceCells(
                type = PieceType.I,
                cells = (0..3).map { row -> Position(column = 0, row = row) }.toSet(),
                columns = 1,
                rows = 4,
            ),
        )
    }

    @Test
    fun `the next piece shown is the same instance from one frame to the next while it does not change`() {
        // Given a game whose first frame set the time reference
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        val before = viewModel.uiState.value

        // When the next frame moves the piece down, the next piece staying the same
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then the screen state changed, but not the next piece: it was not computed again
        val after = viewModel.uiState.value
        assertThat(after.piece).isNotEqualTo(before.piece)
        assertThat(after.next).isSameAs(before.next)
    }

    @Test
    fun `a game under way asks the screen for frames`() {
        // When
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(viewModel.uiState.value.running).isTrue()
    }

    @Test
    fun `a paused game asks the screen for no frame`() {
        // Given a game under way
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When
        viewModel.onPause()

        // Then
        assertThat(viewModel.uiState.value.running).isFalse()
    }

    @Test
    fun `once the game is over the screen is asked for no frame, even before the records answered`() {
        // Given records that take their time to write
        highScores.gate = CompletableDeferred()
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When the game is played until it is over, its score being submitted
        playUntilSubmitted(viewModel)

        // Then no frame is asked for, while the game over screen waits for the records
        assertThat(viewModel.uiState.value.gameOver).isNull()
        assertThat(viewModel.uiState.value.running).isFalse()
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
    fun `unreadable records show 0 and the game goes on`() {
        // Given unreadable records: as the contract says, they give 0, once, and the flow then ends
        highScores.highScoreFlow = flowOf(0)

        // When the game goes on
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(viewModel.uiState.value.record).isEqualTo(0)
        assertThat(viewModel.uiState.value.piece).isEqualTo(spawnedT.droppedBy(rows = 1))
    }

    @Test
    fun `a programming error while the records are read is not swallowed`() {
        // Given records whose reading fails with a bug, not with an unreadable file
        val bug = IllegalStateException("a bug in the records")
        highScores.highScoreFlow = flow { throw bug }

        // When
        val failure = uncaughtFailureOfCreation()

        // Then the bug reaches the uncaught exceptions, as it would crash the application
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage(bug.message)
    }

    @Test
    fun `a programming error while the settings are read is not swallowed`() {
        // Given settings whose reading fails with a bug, not with an unreadable file
        val bug = IllegalStateException("a bug in the settings")
        preferences.preferencesFlow = flow { throw bug }

        // When
        val failure = uncaughtFailureOfCreation()

        // Then the bug reaches the uncaught exceptions, as it would crash the application
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage(bug.message)
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("scoresOfChoices")
    fun `at the start of the game the music of the settings plays, at the normal tempo`(
        choice: MusicChoice,
        expected: Score,
    ) {
        // Given the music chosen on the home screen, and a previous game that ended in danger, the music faster
        preferences.state.value = UserPreferences.DEFAULT.copy(music = choice)
        music.setTempoFactor(1.25)
        music.calls.clear()

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(music.calls).containsExactly("play ${expected.id}")
        assertThat(music.tempo).isEqualTo(1.0)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("homeChoices")
    fun `at the start of the game the music of the player chosen for the games plays instead of the home screen's`(
        choice: MusicChoice,
    ) {
        // Given a music of the player chosen for the games, and any music chosen on the home screen, even none
        preferences.state.value = UserPreferences.DEFAULT.copy(music = choice)
        gameMusic = { personalScore }

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(music.calls).containsExactly("play ${personalScore.id}")
    }

    @Test
    fun `without a music of the player chosen for the games the music of the home screen plays`() {
        // Given the minuet chosen on the home screen, and no music of the player chosen for the games
        preferences.state.value = UserPreferences.DEFAULT.copy(music = MusicChoice.MINUET)
        gameMusic = { null }

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(music.calls).containsExactly("play ${Scores.minuet.id}")
    }

    @Test
    fun `with my file chosen on the home screen the music file of the player plays, its tempo left to the player`() {
        // Given « Mon fichier » chosen on the home screen, with a file
        val myFile = MusicFile("content://music/my-file")
        preferences.state.value = UserPreferences.DEFAULT.copy(music = MusicChoice.MY_FILE)
        musicOf = { choice -> myFile.takeIf { choice == MusicChoice.MY_FILE } }

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then the file plays; the danger tempo goes to the player as for a score, which plays a file at its speed
        assertThat(music.calls).containsExactly("play file ${myFile.uri}")
        assertThat(music.playingFile).isEqualTo(myFile)
    }

    @Test
    fun `the music of the player chosen for the games plays without waiting for the settings`() {
        // Given settings that do not answer, and a music of the player chosen for the games
        preferences.state.value = null
        gameMusic = { personalScore }

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(music.calls).containsExactly("play ${personalScore.id}")
    }

    @Test
    fun `a game paused before the music of the player was read plays it, its danger tempo prepared, when it resumes`() {
        // Given a game whose music of the player for the games has not been read yet
        val read = CompletableDeferred<Score?>()
        gameMusic = { read.await() }
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When the game is paused, then the music is read
        viewModel.onPause()
        read.complete(personalScore)

        // Then nothing plays
        assertThat(music.calls).noneMatch { it.startsWith("play") || it.startsWith("resume") }

        // When the game resumes
        viewModel.onResume()

        // Then the music of the player plays, its danger tempo prepared
        assertThat(music.calls).containsExactly("play ${personalScore.id}")
        assertThat(music.prepared).containsExactly(1.25)
    }

    @Test
    fun `a programming error while the music of the player chosen for the games is read is not swallowed`() {
        // Given a music of the player whose reading fails with a bug, not with an unreadable file
        val bug = IllegalStateException("a bug in the musics")
        gameMusic = { throw bug }

        // When
        val failure = uncaughtFailureOfCreation()

        // Then the bug reaches the uncaught exceptions, as it would crash the application
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage(bug.message)
    }

    @Test
    fun `leaving a game after the next one started leaves the music of the next one playing`() {
        // Given a game playing Korobeiniki, then the next game (Rejouer) playing the minuet
        val store = ViewModelStore()
        createViewModelIn(store)
        preferences.state.value = UserPreferences.DEFAULT.copy(music = MusicChoice.MINUET)
        createViewModel(GameRoute(startLevel = 0, seed = 2L))

        // When the first game leaves the stack
        store.clear()

        // Then the music of the next game still plays
        assertThat(music.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `the music of a new game is not stopped by the earlier game whose music it replaced`() {
        // Given a game playing Korobeiniki, still alive
        createViewModel(GameRoute(startLevel = 0, seed = 1L))
        preferences.state.value = UserPreferences.DEFAULT.copy(music = MusicChoice.MINUET)

        // When the next game (Rejouer) plays the minuet in its place: the playback of the first one no longer plays
        createViewModel(GameRoute(startLevel = 0, seed = 2L))

        // Then the music of the next game still plays
        assertThat(music.playing).isEqualTo(Scores.minuet.id)
    }

    @Test
    fun `leaving the game screen stops the music`() {
        // Given a game playing Korobeiniki, in a view model store like the navigation entry's
        val store = ViewModelStore()
        createViewModelIn(store)
        music.calls.clear()

        // When the entry leaves the stack
        store.clear()

        // Then
        assertThat(music.calls).containsExactly("stop")
    }

    @Test
    fun `when the stack leaves the danger the music goes back to its tempo`() {
        // Given four upright I in column 0, which make the stack 16 high (danger), then four flat J in columns 1
        // to 4, four flat L in columns 5 to 8 and an upright O in column 9, which fill the four bottom rows,
        // cleared: the stack falls to 12 (no danger)
        fun upright(column: Int) = Orientation((0..3).map { row -> Position(column - 5, row) }.toSet())
        fun flat(firstColumn: Int) = Orientation((firstColumn..firstColumn + 3).map { Position(it - 5, 0) }.toSet())
        val factory = GameEngineFactory {
            GameEngine(
                ClassicRules.copy(
                    rotationSystem = SpawnAs(
                        PieceType.I to upright(0),
                        PieceType.J to flat(1),
                        PieceType.L to flat(5),
                        PieceType.O to upright(9),
                    ),
                    gravityTable = oneTickPerRow,
                ),
                // The pieces, plus the next one shown while the last one falls.
                ScriptedRandomizer(
                    *Array(4) { PieceType.I },
                    *Array(4) { PieceType.J },
                    *Array(4) { PieceType.L },
                    PieceType.O,
                    PieceType.I,
                ),
            )
        }
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), factory)
        val tempos = { music.calls.filter { it.startsWith("tempo") } }

        // When the game is played until the stack left the danger
        playUntil(viewModel, "the stack left the danger") { tempos().size == 2 }

        // Then
        assertThat(tempos()).containsExactly("tempo 1.25", "tempo 1.0")
    }

    @Test
    fun `when the stack enters the danger the music speeds up`() {
        // Given a game whose stack grows until it is over, through the danger
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When
        playUntilGameOver(viewModel)

        // Then: the danger tempo once the stack is high (the play at the start is at the normal tempo)
        assertThat(music.calls.filter { it.startsWith("tempo") }).containsExactly("tempo 1.25")
    }

    @Test
    fun `the game over stops the music`() {
        // Given a game playing Korobeiniki
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When
        playUntilGameOver(viewModel)

        // Then
        assertThat(music.calls.last()).isEqualTo("stop")
    }

    @Test
    fun `the next game after a game over in danger plays its music at the normal tempo`() {
        // Given a game over in danger, its stack high: the music was asked to go a quarter faster
        val first = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        playUntilGameOver(first)
        assertThat(music.tempo).isEqualTo(1.25)

        // When the player starts the next game (Rejouer)
        createViewModel(GameRoute(startLevel = 0, seed = 2L))

        // Then its music plays, at the normal tempo
        assertThat(music.calls.last()).isEqualTo("play ${Scores.korobeiniki.id}")
        assertThat(music.tempo).isEqualTo(1.0)
    }

    @Test
    fun `a game paused in danger goes on at the danger tempo when it resumes`() {
        // Given a game paused while its stack is in danger
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        playUntilInDanger(viewModel)
        viewModel.onPause()

        // When
        viewModel.onResume()

        // Then
        assertThat(music.tempo).isEqualTo(1.25)
    }

    @Test
    fun `a pause stops the music, and resuming goes on where it stopped`() {
        // Given a game playing Korobeiniki
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.calls.clear()

        // When / Then
        viewModel.onPause()
        assertThat(music.calls).containsExactly("stop")
        viewModel.onResume()
        assertThat(music.calls).containsExactly("stop", "resume ${Scores.korobeiniki.id}")
    }

    @Test
    fun `a music that stops by itself is stopped, to give the audio focus back, and the game goes on`() {
        // Given a game playing Korobeiniki
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.calls.clear()

        // When its music stops by itself (the sequencer fails)
        music.fail()

        // Then its playback is stopped, which gives the focus back, and the game still runs
        assertThat(music.calls).containsExactly("failure", "stop")
        assertThat(viewModel.uiState.value.running).isTrue()
    }

    @Test
    fun `a music that stopped by itself plays again when the game resumes after a pause`() {
        // Given a game whose music stopped by itself, then paused
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.fail()
        viewModel.onPause()
        music.calls.clear()

        // When
        viewModel.onResume()

        // Then the music tries again, where it stopped
        assertThat(music.calls).containsExactly("resume ${Scores.korobeiniki.id}")
        assertThat(music.playing).isEqualTo(Scores.korobeiniki.id)
    }

    @Test
    fun `a music that stopped by itself again after the resume is stopped again`() {
        // Given a game whose music stopped by itself, played again by a pause and a resume
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.fail()
        viewModel.onPause()
        viewModel.onResume()
        music.calls.clear()

        // When the music stops by itself once more
        music.fail()

        // Then its new playback is stopped too, which gives the focus back
        assertThat(music.calls).containsExactly("failure", "stop")
    }

    @Test
    fun `entering the danger while the music is stopped by itself plays nothing, and the resume is faster`() {
        // Given a game whose music stopped by itself
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.fail()
        music.calls.clear()

        // When its stack enters the danger
        playUntilInDanger(viewModel)

        // Then the danger tempo is kept for later, and no music plays
        assertThat(music.calls).containsExactly("tempo 1.25")
        assertThat(music.playing).isNull()

        // And once the game is paused and resumed, the music plays again at the danger tempo
        viewModel.onPause()
        viewModel.onResume()
        assertThat(music.playing).isEqualTo(Scores.korobeiniki.id)
        assertThat(music.tempo).isEqualTo(1.25)
    }

    @Test
    fun `resuming a game that is not paused plays nothing again`() {
        // Given a game playing Korobeiniki, never paused
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        music.calls.clear()

        // When
        viewModel.onResume()

        // Then
        assertThat(music.calls).isEmpty()
    }

    @Test
    fun `resuming once the game is over plays no music`() {
        // Given a game over
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        playUntilGameOver(viewModel)
        music.calls.clear()

        // When
        viewModel.onResume()

        // Then
        assertThat(music.calls).isEmpty()
    }

    @Test
    fun `a game paused before its music started starts it from the start when it resumes`() {
        // Given a game paused before the settings answered, which then answer
        preferences.state.value = null
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onPause()
        preferences.state.value = UserPreferences.DEFAULT
        music.calls.clear()

        // When
        viewModel.onResume()

        // Then
        assertThat(music.calls).containsExactly("play ${Scores.korobeiniki.id}")
    }

    @Test
    fun `at the start of the game the music prepares the danger tempo`() {
        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then the music has its danger tempo ready before any danger
        assertThat(music.playing).isEqualTo(Scores.korobeiniki.id)
        assertThat(music.prepared).containsExactly(1.25)
    }

    @Test
    fun `a game paused before its music started prepares the danger tempo when its music starts`() {
        // Given a game paused before the settings answered, which then answer
        preferences.state.value = null
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onPause()
        preferences.state.value = UserPreferences.DEFAULT

        // When
        viewModel.onResume()

        // Then
        assertThat(music.prepared).containsExactly(1.25)
    }

    @Test
    fun `a game paused before the settings answered starts no music once they answer`() {
        // Given settings that have not answered yet
        preferences.state.value = null
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // When the game is paused, then the settings answer
        viewModel.onPause()
        preferences.state.value = UserPreferences.DEFAULT

        // Then
        assertThat(music.calls).noneMatch { it.startsWith("play") || it.startsWith("resume") }
    }

    @Test
    fun `with no music chosen nothing plays`() {
        // Given
        preferences.state.value = UserPreferences.DEFAULT.copy(music = MusicChoice.NONE)

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(music.calls).noneMatch { it.startsWith("play") }
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

    @Test
    fun `a game recreated after the process died starts paused, with no music`() {
        // Given a game under way, whose process dies: only its saved state is left
        val savedState = SavedStateHandle()
        createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState)
        music.calls.clear()

        // When the navigation restores its entry: a new view model, with what was saved
        val recreated = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState.restored())

        // Then the game waits for the player, paused, and no music plays
        assertThat(recreated.uiState.value.paused).isTrue()
        assertThat(music.calls).isEmpty()
    }

    @Test
    fun `a first game, with nothing saved, is not paused and plays its music`() {
        // When a game starts with the empty saved state of a new navigation entry
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = SavedStateHandle())

        // Then
        assertThat(viewModel.uiState.value.paused).isFalse()
        assertThat(music.playing).isEqualTo(Scores.korobeiniki.id)
    }

    @Test
    fun `a game recreated after the process died plays its music from the start when the player resumes it`() {
        // Given a game recreated, paused, after its process died
        val savedState = SavedStateHandle()
        val store = ViewModelStore()
        createViewModelIn(store, savedState)
        // The process dies: its view model, and so the following of its music, goes with it. Clearing the store calls
        // onCleared, which a real death of the process does not; but there the music player dies too, while here it
        // is shared: a view model left alive would still follow its music, and stop it once the recreated game
        // replaced it, a stop the fake notes.
        store.clear()
        val recreated = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState.restored())
        music.calls.clear()

        // When the player resumes it
        recreated.onResume()

        // Then it goes on, and its music starts
        assertThat(recreated.uiState.value.paused).isFalse()
        assertThat(music.calls).containsExactly("play ${Scores.korobeiniki.id}")
    }

    @Test
    fun `a game over whose process died comes back as a new game, paused, its score not submitted again`() {
        // Given a game over, whose process dies
        val savedState = SavedStateHandle()
        playUntilGameOver(createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState))

        // When the navigation restores its entry
        val recreated = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState.restored())

        // Then a new game waits for the player, paused, and the records got the score once
        assertThat(recreated.uiState.value.paused).isTrue()
        assertThat(recreated.uiState.value.gameOver).isNull()
        assertThat(highScores.submitted).hasSize(1)
    }

    @Test
    fun `clearing one line vibrates the short pulse`() {
        // Given a game whose first three pieces fill row 19
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), oneLineGame)

        // When the game is played until something vibrates
        playUntil(viewModel, "something vibrates") { haptics.played.isNotEmpty() }

        // Then it is the pulse of one to three lines, once
        assertThat(haptics.played).containsExactly(Haptic.LINES_CLEARED)
    }

    @Test
    fun `clearing four lines at once vibrates the stronger pulse`() {
        // Given a game whose first ten pieces fill rows 16 to 19, the last one completing all four
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), fourLinesGame)

        // When the game is played until something vibrates
        playUntil(viewModel, "something vibrates") { haptics.played.isNotEmpty() }

        // Then it is the pulse of four lines, once
        assertThat(haptics.played).containsExactly(Haptic.FOUR_LINES_CLEARED)
    }

    @Test
    fun `the game over vibrates its pattern once, and the frames after it nothing more`() {
        // Given a game played until its game over shows: the test game clears no line
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        val lastFrame = playUntilGameOver(viewModel)

        // When frames still come
        repeat(10) { viewModel.onFrame(lastFrame + (it + 1) * ONE_SIXTIETH_NANOS) }

        // Then
        assertThat(haptics.played).containsExactly(Haptic.GAME_OVER)
    }

    @Test
    fun `with the vibrations turned off in the settings nothing vibrates`() {
        // Given the vibrations turned off on the home screen
        preferences.state.value = UserPreferences.DEFAULT.copy(vibrations = false)

        // When a game clears a line, then another one is played until it is over
        val clearing = createViewModel(GameRoute(startLevel = 0, seed = 1L), oneLineGame)
        playUntil(clearing, "a line is cleared") { clearing.uiState.value.lines == Lines(1) }
        playUntilGameOver(createViewModel(GameRoute(startLevel = 0, seed = 1L)))

        // Then
        assertThat(haptics.played).isEmpty()
    }

    @Test
    fun `until the settings answer nothing vibrates`() {
        // Given settings that never answer
        val silentPreferences = FakeUserPreferencesRepository(initial = null)

        // When a game is played until it is over
        playUntilGameOver(createViewModel(GameRoute(startLevel = 0, seed = 1L), userPreferences = silentPreferences))

        // Then
        assertThat(haptics.played).isEmpty()
    }

    @Test
    fun `a game recreated paused after the process died vibrates nothing while it stays paused`() {
        // Given a game recreated, paused, after its process died
        val savedState = SavedStateHandle()
        createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState)
        val recreated = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState.restored())

        // When frames come for longer than the whole test game lasts
        generateSequence(0L) { it + ONE_SIXTIETH_NANOS }.take(MAX_TICKS).forEach(recreated::onFrame)

        // Then
        assertThat(haptics.played).isEmpty()
    }

    @Test
    fun `clearing one line plays the lines cleared effect`() {
        // Given a game whose first three pieces fill row 19
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), oneLineGame)

        // When the game is played until an effect plays
        playUntil(viewModel, "an effect plays") { soundEffects.played.isNotEmpty() }

        // Then it is the effect of one to three lines, once
        assertThat(soundEffects.played).containsExactly(SoundEffects.linesCleared)
    }

    @Test
    fun `clearing four lines at once plays the four lines effect`() {
        // Given a game whose first ten pieces fill rows 16 to 19, the last one completing all four
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L), fourLinesGame)

        // When the game is played until an effect plays
        playUntil(viewModel, "an effect plays") { soundEffects.played.isNotEmpty() }

        // Then it is the effect of four lines, once
        assertThat(soundEffects.played).containsExactly(SoundEffects.fourLinesCleared)
    }

    @Test
    fun `a piece turned plays the rotation effect on the frame of its tick`() {
        // Given a game whose first frame set the time reference
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        viewModel.onFrame(frameNanos = 0L)

        // When A is pressed, then the next frame comes
        viewModel.onControl(ControlChange.Pressed(ControlButton.A))
        assertThat(soundEffects.played).isEmpty()
        viewModel.onFrame(frameNanos = ONE_SIXTIETH_NANOS)

        // Then
        assertThat(soundEffects.played).containsExactly(SoundEffects.rotation)
    }

    @Test
    fun `the game over plays its effect once, and the frames after it nothing more`() {
        // Given a game played until its game over shows: the test game clears no line
        val viewModel = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        val lastFrame = playUntilGameOver(viewModel)

        // When frames still come
        repeat(10) { viewModel.onFrame(lastFrame + (it + 1) * ONE_SIXTIETH_NANOS) }

        // Then
        assertThat(soundEffects.played).containsExactly(SoundEffects.gameOver)
    }

    @Test
    fun `with the sound effects turned off in the settings no effect plays, the vibrations still do`() {
        // Given the sound effects turned off, the vibrations on
        preferences.state.value = UserPreferences.DEFAULT.copy(vibrations = true, soundEffects = false)

        // When a game clears a line, then another one turns its first piece and is played until it is over
        val clearing = createViewModel(GameRoute(startLevel = 0, seed = 1L), oneLineGame)
        playUntil(clearing, "a line is cleared") { clearing.uiState.value.lines == Lines(1) }
        val turning = createViewModel(GameRoute(startLevel = 0, seed = 1L))
        turning.onFrame(frameNanos = 0L)
        turning.onControl(ControlChange.Pressed(ControlButton.A))
        playUntil(turning, "the screen shows the game over", firstFrame = ONE_SIXTIETH_NANOS) {
            turning.uiState.value.gameOver != null
        }

        // Then
        assertThat(soundEffects.played).isEmpty()
        assertThat(haptics.played).isNotEmpty()
    }

    @Test
    fun `with the vibrations turned off in the settings the sound effects still play`() {
        // Given the vibrations turned off, the sound effects on
        preferences.state.value = UserPreferences.DEFAULT.copy(vibrations = false, soundEffects = true)

        // When a game is played until it is over
        playUntilGameOver(createViewModel(GameRoute(startLevel = 0, seed = 1L)))

        // Then
        assertThat(soundEffects.played).containsExactly(SoundEffects.gameOver)
        assertThat(haptics.played).isEmpty()
    }

    @Test
    fun `with the sound effects on, the game warms them all up as soon as the settings answer, before any plays`() {
        // Given settings that have not answered yet
        val lateSettings = FakeUserPreferencesRepository(initial = null)
        createViewModel(GameRoute(startLevel = 0, seed = 1L), userPreferences = lateSettings)
        assertThat(soundEffects.warmedUp).isEmpty()

        // When they answer, the sound effects on
        lateSettings.state.value = UserPreferences.DEFAULT.copy(soundEffects = true)

        // Then
        assertThat(soundEffects.warmedUp).containsExactlyElementsOf(SoundEffects.all)
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `with the sound effects off, the game warms nothing up`() {
        // Given the sound effects turned off
        preferences.state.value = UserPreferences.DEFAULT.copy(soundEffects = false)

        // When
        createViewModel(GameRoute(startLevel = 0, seed = 1L))

        // Then
        assertThat(soundEffects.warmedUp).isEmpty()
    }

    @Test
    fun `until the settings answer no effect plays`() {
        // Given settings that never answer
        val silentPreferences = FakeUserPreferencesRepository(initial = null)

        // When a game is played until it is over
        playUntilGameOver(createViewModel(GameRoute(startLevel = 0, seed = 1L), userPreferences = silentPreferences))

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    @Test
    fun `a game recreated paused after the process died plays no effect while it stays paused`() {
        // Given a game recreated, paused, after its process died, and A pressed
        val savedState = SavedStateHandle()
        createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState)
        val recreated = createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState.restored())
        recreated.onControl(ControlChange.Pressed(ControlButton.A))

        // When frames come for longer than the whole test game lasts
        generateSequence(0L) { it + ONE_SIXTIETH_NANOS }.take(MAX_TICKS).forEach(recreated::onFrame)

        // Then
        assertThat(soundEffects.played).isEmpty()
    }

    /**
     * A game view model built with test doubles, as the factory would, but without its creation extras: the
     * route and [savedState] (empty by default, as for a new navigation entry) are given directly. The factory
     * itself, which takes them from the creation extras, is covered only by the instrumented tests
     * (`GameViewModelFactoryTest`, `GameEntryRestorationTest`).
     */
    private fun createViewModel(
        route: GameRoute,
        factory: GameEngineFactory = engineFactory,
        savedState: SavedStateHandle = SavedStateHandle(),
        records: FakeHighScoreRepository = highScores,
        userPreferences: FakeUserPreferencesRepository = preferences,
    ): GameViewModel = GameViewModel(
        factory,
        route,
        records,
        userPreferences,
        music,
        haptics,
        soundEffects,
        musicOf,
        gameMusic,
        savedState,
    )

    /** What the system gives back of [this] saved state once the process died: its values, in a new handle. */
    private fun SavedStateHandle.restored(): SavedStateHandle = SavedStateHandle(keys().associateWith { get<Any>(it) })

    /**
     * The exception left uncaught when a game view model is created, or null. `runTest` reports the
     * uncaught exceptions of every coroutine, those of `viewModelScope` included, as the application
     * would crash with them.
     */
    private fun uncaughtFailureOfCreation(): Throwable? =
        runCatching { runTest { createViewModel(GameRoute(startLevel = 0, seed = 1L)) } }.exceptionOrNull()

    /** A game view model kept in [store], as the navigation entry keeps it: cleared when the store is. */
    private fun createViewModelIn(
        store: ViewModelStore,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): GameViewModel = ViewModelProvider.create(
        store,
        viewModelFactory {
            initializer { createViewModel(GameRoute(startLevel = 0, seed = 1L), savedState = savedState) }
        },
    )[GameViewModel::class]

    private companion object {
        const val ONE_SIXTIETH_NANOS = 16_666_667L

        /** The score each music choice plays, written out: not computed by the code under test. */
        @JvmStatic
        fun scoresOfChoices(): List<Arguments> = listOf(
            argumentSet("Korobeiniki", MusicChoice.KOROBEINIKI, Scores.korobeiniki),
            argumentSet("the minuet", MusicChoice.MINUET, Scores.minuet),
        )

        /** Every music the home screen may choose, none included: the music of the player plays instead of any. */
        @JvmStatic
        fun homeChoices(): List<MusicChoice> = MusicChoice.entries

        /** A music of the player, « Mes musiques »: the minuet, under a personal identity. */
        val personalScore: Score = Scores.minuet.copy(id = ScoreId.Personal("mine"))

        /** Enough ticks for the test game to fill the board and end, entry delays included. */
        const val MAX_TICKS = 5_000

        val oneTickPerRow = object : GravityTable {
            override fun ticksPerRow(level: Level): Ticks = Ticks(1)
        }

        /**
         * The classic scoring, plus 100 points for each piece that locks. The engine calls
         * [ScoringRule.pointsForSoftDrop] once per lock, soft drop or not: that call is the hook used here, so that
         * the test game, which neither clears a line nor soft drops, still scores.
         */
        val hundredPointsPerLock = object : ScoringRule by ClassicScoringRule {
            override fun pointsForSoftDrop(rowsDropped: Int): Points = Points(100)
        }

        /** The T as it appears: pivot at (5, 0), spawn orientation. */
        val spawnedT = spawned(PieceType.T)

        /**
         * Engines whose pieces appear as [rotationSystem] says, without turning, and drop one row per tick,
         * [pieces] in turn.
         */
        fun scriptedGame(
            rotationSystem: RotationSystem,
            vararg pieces: PieceType,
        ) = GameEngineFactory {
            GameEngine(
                ClassicRules.copy(rotationSystem = rotationSystem, gravityTable = oneTickPerRow),
                ScriptedRandomizer.cycling(*pieces),
            )
        }

        /** A game whose first three pieces fill row 19 together: columns 0 to 3, 4 to 7, then 8 and 9. */
        val oneLineGame = scriptedGame(
            SpawnAs(
                PieceType.I to Orientation(setOf(Position(-5, 0), Position(-4, 0), Position(-3, 0), Position(-2, 0))),
                PieceType.J to Orientation(setOf(Position(-1, 0), Position(0, 0), Position(1, 0), Position(2, 0))),
                PieceType.O to Orientation(setOf(Position(3, -1), Position(4, -1), Position(3, 0), Position(4, 0))),
            ),
            PieceType.I,
            PieceType.J,
            PieceType.O,
        )

        /**
         * A game whose first ten pieces fill rows 16 to 19 together: two-by-two squares stacked in pairs on columns
         * 0-1 (T), 2-3 (J), 4-5 (L) and 7-8 (O), a vertical bar on column 9 (I), and last a vertical bar on column 6
         * (S), which completes the four rows at once.
         */
        val fourLinesGame = scriptedGame(
            SpawnAs(
                PieceType.T to square(column = 0),
                PieceType.J to square(column = 2),
                PieceType.L to square(column = 4),
                PieceType.S to bar(column = 6),
                PieceType.O to square(column = 7),
                PieceType.I to bar(column = 9),
            ),
            PieceType.I,
            PieceType.O,
            PieceType.O,
            PieceType.T,
            PieceType.T,
            PieceType.J,
            PieceType.J,
            PieceType.L,
            PieceType.L,
            PieceType.S,
        )

        /** A two-by-two square whose left column is [column] on the board, the pivot appearing on column 5. */
        fun square(column: Int): Orientation {
            val left = column - SPAWN_COLUMN
            return Orientation(
                setOf(Position(left, 0), Position(left + 1, 0), Position(left, 1), Position(left + 1, 1)),
            )
        }

        /** A vertical bar of four cells on [column] of the board, the pivot appearing on column 5. */
        fun bar(column: Int): Orientation = Orientation((0..3).map { Position(column - SPAWN_COLUMN, it) }.toSet())

        /** The column the pivot of every piece appears on. */
        const val SPAWN_COLUMN = 5
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

    /** Gives [viewModel] one frame per tick until its stack is in danger, the music faster; the time of that frame. */
    private fun playUntilInDanger(viewModel: GameViewModel): Long =
        playUntil(viewModel, "the stack is in danger") { music.tempo == 1.25 }

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

    /**
     * A rotation system for tests: each type of [spawns] appears in its own orientation, the others in their classic
     * one, and none turns. It answers the same however often it is asked, for every type, as the contract says.
     */
    private class SpawnAs(vararg spawns: Pair<PieceType, Orientation>) : RotationSystem {
        private val spawns = spawns.toMap()

        override fun orientations(type: PieceType): List<Orientation> = listOf(spawnOrientation(type))

        override fun spawnOrientation(type: PieceType): Orientation =
            spawns[type] ?: ClassicRotationSystem.spawnOrientation(type)

        override fun candidates(
            piece: Piece,
            rotation: Rotation,
        ): List<Piece> = emptyList()
    }
}
