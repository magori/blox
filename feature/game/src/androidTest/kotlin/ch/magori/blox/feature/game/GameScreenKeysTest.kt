package ch.magori.blox.feature.game

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.gameover.GameOverUiState
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The keyboard and the pad on the game screen: the screen takes the focus as it shows, hands the keys of
 * the game to the view model as a finger would (`KeyActionTest` gives the table), and keeps the keys of the
 * game from the system on the pause and game over screens (`MenuKeyActionTest`).
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenKeysTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val underWay = GameUiState(
        board = Board.parse(
            """
            IIII.ZZSSL
            """,
        ),
        piece = null,
        next = nextPieceCells(PieceType.I, ClassicRotationSystem),
        level = Level(0),
        lines = Lines(0),
        score = Points(0),
    )

    private val over = underWay.copy(
        gameOver = GameOverUiState(
            score = Points(12_340),
            lines = Lines(52),
            level = Level(5),
            fourLineClears = 1,
            ticksPlayed = 3_600,
            newBest = false,
            resultsKept = true,
            previousBest = 0,
            practice = false,
        ),
    )

    private var uiState by mutableStateOf(underWay)
    private val changes = CopyOnWriteArrayList<ControlChange>()
    private var backs by mutableIntStateOf(0)
    private var menus by mutableIntStateOf(0)
    private lateinit var left: String

    @Test
    @DisplayName("a key held down holds its button, once despite the repeats of the keyboard, until it comes back up")
    fun aKeyHeldDownHoldsItsButtonUntilItComesBackUp() = extension.use {
        // Given
        showGameScreen()

        // When ◀ is held long enough for the keyboard to repeat it, then comes back up
        onRoot().performKeyInput {
            keyDown(Key.DirectionLeft)
            advanceEventTime(1_000)
            keyUp(Key.DirectionLeft)
        }

        // Then
        runOnIdle {
            assertThat(changes).containsExactly(
                ControlChange.Pressed(ControlButton.LEFT),
                ControlChange.Released(ControlButton.LEFT),
            )
        }
    }

    @Test
    @DisplayName("Escape pauses the game, and the keys still held are released with the controls")
    fun escapePausesTheGameAndTheKeysHeldAreReleased() = extension.use {
        // Given
        showGameScreen()

        // When ◀ is held as Escape is pressed
        onRoot().performKeyInput {
            keyDown(Key.DirectionLeft)
            pressKey(Key.Escape)
        }

        // Then
        runOnIdle {
            assertThat(uiState.paused).isTrue()
            assertThat(changes).containsExactly(
                ControlChange.Pressed(ControlButton.LEFT),
                ControlChange.Released(ControlButton.LEFT),
            )
        }
    }

    @Test
    @DisplayName("a key held across the pause does nothing once the game resumes")
    fun aKeyHeldAcrossThePauseDoesNothingOnceTheGameResumes() = extension.use {
        // Given ◀ held as Escape pauses the game, which then resumes
        showGameScreen()
        onRoot().performKeyInput {
            keyDown(Key.DirectionLeft)
            pressKey(Key.Escape)
        }
        runOnIdle { uiState = uiState.copy(paused = false) }
        waitForIdle()

        // When ◀ comes back up only now
        onRoot().performKeyInput { keyUp(Key.DirectionLeft) }

        // Then it was released with the pause, and is not released a second time
        runOnIdle {
            assertThat(changes).containsExactly(
                ControlChange.Pressed(ControlButton.LEFT),
                ControlChange.Released(ControlButton.LEFT),
            )
        }
    }

    @Test
    @DisplayName("once the game resumes, the screen takes the focus again and the keys play")
    fun onceTheGameResumesTheKeysPlayAgain() = extension.use {
        // Given a game paused, then resumed
        showGameScreen()
        runOnIdle { uiState = uiState.copy(paused = true) }
        runOnIdle { uiState = uiState.copy(paused = false) }

        // When
        onRoot().performKeyInput { pressKey(Key.ButtonA) }

        // Then
        runOnIdle {
            assertThat(changes).containsExactly(
                ControlChange.Pressed(ControlButton.A),
                ControlChange.Released(ControlButton.A),
            )
        }
    }

    @Test
    @DisplayName("after a touch on a control, the keys still play")
    fun afterATouchOnAControlTheKeysStillPlay() = extension.use {
        // Given ◀ touched, then let go
        showGameScreen()
        onNodeWithContentDescription(left).performTouchInput {
            down(center)
            up()
        }
        runOnIdle { changes.clear() }

        // When
        onRoot().performKeyInput { pressKey(Key.ButtonA) }

        // Then
        runOnIdle {
            assertThat(changes).containsExactly(
                ControlChange.Pressed(ControlButton.A),
                ControlChange.Released(ControlButton.A),
            )
        }
    }

    @Test
    @DisplayName("in pause, Start resumes the game")
    fun inPauseStartResumesTheGame() = extension.use {
        // Given
        uiState = underWay.copy(paused = true)
        showGameScreen()

        // When
        onRoot().performKeyInput { pressKey(Key.ButtonStart) }

        // Then
        runOnIdle { assertThat(uiState.paused).isFalse() }
    }

    @Test
    @DisplayName("in pause, the cross sent by the system still moves the focus to a button")
    fun inPauseTheCrossStillMovesTheFocusToAButton() = extension.use {
        // Given
        uiState = underWay.copy(paused = true)
        showGameScreen()

        // When the cross goes down, sent by the system; leaving the touch mode may take the first press
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(times = 2) {
            if (onAllNodes(isFocused() and hasClickAction()).fetchSemanticsNodes().isEmpty()) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
                waitForIdle()
            }
        }

        // Then
        onNode(isFocused() and hasClickAction()).assertExists()
    }

    @Test
    @DisplayName("under the card of the pause, the arrows and Tab keep the focus on its buttons, and play nothing")
    fun underTheCardOfThePauseTheKeysStayOnItsButtons() = extension.use {
        // Given a game paused, its card shown, the focus on one of its buttons
        uiState = underWay.copy(paused = true)
        showGameScreen()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(times = 2) {
            if (onAllNodes(isFocused() and hasClickAction()).fetchSemanticsNodes().isEmpty()) {
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
                waitForIdle()
            }
        }

        // When the arrows and Tab are pressed, sent by the system, many times over
        repeat(times = 4) {
            listOf(
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_TAB,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_UP,
            ).forEach { instrumentation.sendKeyDownUpSync(it) }
        }
        waitForIdle()

        // Then the focus is still on Reprendre or Menu, nothing went to the game under the card, which still waits
        val context = instrumentation.targetContext
        val buttons = hasText(context.getString(R.string.feature_game_pause_resume)) or
            hasText(context.getString(R.string.feature_game_pause_menu))
        onNode(isFocused() and hasClickAction() and buttons).assertExists()
        runOnIdle {
            assertThat(changes).isEmpty()
            assertThat(uiState.paused).isTrue()
        }
    }

    @Test
    @DisplayName("once the game is over, B and Escape hammered through the system neither go back nor leave the screen")
    fun onceTheGameIsOverBAndEscapeDoNotGoBack() = extension.use {
        // Given
        uiState = over
        showGameScreen()

        // When the player still hammers B and Escape, sent by the system as by a real pad or keyboard
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(times = 3) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BUTTON_B)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ESCAPE)
        }

        // Then
        runOnIdle {
            assertThat(backs).isZero()
            assertThat(menus).isZero()
        }
    }

    private fun ComposeContext.showGameScreen() {
        setContent {
            left = stringResource(R.string.feature_game_control_left)
            // The system back, as the navigation would handle it: counted, so that a key that goes back shows.
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                onBackCompleted = { backs++ },
            )
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                GameScreen(
                    uiState = { uiState },
                    onBack = { menus++ },
                    onReplay = {},
                    onControl = { changes += it },
                    onPause = { uiState = uiState.copy(paused = true) },
                    onResume = { uiState = uiState.copy(paused = false) },
                    labels = GameLabels.Classic,
                    onCopy = {},
                )
            }
        }
        waitForIdle()
    }
}
