package ch.magori.blox.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.navigation.GameRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** The frames and the display mode the game screen asks for, with a real view model and the frame clock of the test. */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenFramesTest {
    @Test
    @DisplayName("the game over screen asks for no frame: Compose becomes idle")
    fun theGameOverScreenAsksForNoFrame() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game at the fastest start level (9, the last the rules allow), its frames handed one by one
            val activity = checkNotNull(activity)
            val replay = activity.getString(R.string.feature_game_over_replay)
            val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply {
                set(GameViewModel.RouteKey, GameRoute(startLevel = 9, seed = 1L))
            }
            mainClock.autoAdvance = false
            lateinit var game: GameViewModel
            setContent {
                val viewModel: GameViewModel = viewModel(factory = silentGameViewModelFactory(), extras = extras)
                game = viewModel
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GameScreen(viewModel = viewModel, onBack = {}, onReplay = {}, onCopy = {})
                }
            }

            // When it is played until its game over screen shows, each piece dropped fast (▼ pressed again every
            // FRAMES_BETWEEN_CHECKS frames, since a new piece needs a new press): the shortest way to the end. The
            // clock plays each frame of an advance in turn, without waiting for Compose to be idle after each one,
            // and the screen is searched only between two presses: on a loaded emulator, the test has a minute.
            var frames = 0
            while (onAllNodesWithText(replay).fetchSemanticsNodes().isEmpty()) {
                check(frames < MAX_FRAMES) { "No game over after $MAX_FRAMES frames" }
                runOnUiThread {
                    game.onControl(ControlChange.Released(ControlButton.DOWN))
                    game.onControl(ControlChange.Pressed(ControlButton.DOWN))
                }
                mainClock.advanceTimeBy(FRAMES_BETWEEN_CHECKS * FRAME_MILLIS)
                frames += FRAMES_BETWEEN_CHECKS
            }

            // Then, the clock left to run on its own, Compose has no frame left to draw: it becomes idle
            mainClock.autoAdvance = true
            waitForIdle()
            onNodeWithText(replay).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the game screen asks for the 60 Hz mode of the display, and gives the previous one back once left")
    fun theGameScreenAsksForThe60HzModeAndGivesThePreviousOneBackOnceLeft() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given an activity that asked for a mode of its own, and the mode the game asks for on its display
            val activity = checkNotNull(activity)
            val window = activity.window
            runOnUiThread { window.attributes = window.attributes.apply { preferredDisplayModeId = EARLIER_MODE } }
            val gameMode = checkNotNull(window.decorView.display).gameDisplayMode()
            assumeTrue(gameMode != null && gameMode.id != EARLIER_MODE, "The display has a mode for the game")
            val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply {
                set(GameViewModel.RouteKey, GameRoute(startLevel = 0, seed = 1L))
            }
            var shown by mutableStateOf(true)
            // Frames handed by hand only: the game under way does not keep Compose busy.
            mainClock.autoAdvance = false

            // When the game screen shows
            setContent {
                if (shown) {
                    val viewModel: GameViewModel = viewModel(factory = silentGameViewModelFactory(), extras = extras)
                    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                        GameScreen(viewModel = viewModel, onBack = {}, onReplay = {}, onCopy = {})
                    }
                }
            }

            // Then the window asks for the mode of the game
            runOnIdle { assertThat(window.attributes.preferredDisplayModeId).isEqualTo(checkNotNull(gameMode).id) }

            // When the game screen is left
            shown = false
            mainClock.advanceTimeByFrame()

            // Then the window asks for the mode it asked for before
            runOnIdle { assertThat(window.attributes.preferredDisplayModeId).isEqualTo(EARLIER_MODE) }
        }
    }

    @Test
    @DisplayName("two game screens shown together, as while Rejouer goes on, keep the mode until both are gone")
    fun twoGameScreensShownTogetherKeepTheModeUntilBothAreGone() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given an activity that asked for a mode of its own, and the mode the game asks for on its display
            val window = checkNotNull(activity).window
            runOnUiThread { window.attributes = window.attributes.apply { preferredDisplayModeId = EARLIER_MODE } }
            val gameMode = checkNotNull(window.decorView.display).gameDisplayMode()
            assumeTrue(gameMode != null && gameMode.id != EARLIER_MODE, "The display has a mode for the game")
            var first by mutableStateOf(true)
            var second by mutableStateOf(false)
            setContent {
                if (first) GameDisplayModeEffect()
                if (second) GameDisplayModeEffect()
            }

            // When a second one comes, then the first one goes
            runOnIdle { second = true }
            runOnIdle { first = false }

            // Then the window still asks for the mode of the game
            runOnIdle { assertThat(window.attributes.preferredDisplayModeId).isEqualTo(checkNotNull(gameMode).id) }

            // When the second one goes too
            runOnIdle { second = false }

            // Then the window asks for the mode it asked for before
            runOnIdle { assertThat(window.attributes.preferredDisplayModeId).isEqualTo(EARLIER_MODE) }
        }
    }

    private companion object {
        /** Two minutes of play: far more than a game at level 9 lasts without a single move. */
        const val MAX_FRAMES = 7_200

        /** The frames between two presses of ▼ and two searches of the game over screen. */
        const val FRAMES_BETWEEN_CHECKS = 10

        /** The duration of a frame of the clock of the test, which plays each frame of an advance in turn. */
        const val FRAME_MILLIS = 16L

        /** A mode the activity asked for before the game: any id, the window keeps it as it is given. */
        const val EARLIER_MODE = 4_242
    }
}
