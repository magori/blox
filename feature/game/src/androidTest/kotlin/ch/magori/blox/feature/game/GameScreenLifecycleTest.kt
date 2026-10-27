package ch.magori.blox.feature.game

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.feature.game.navigation.GameRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The game screen through the lifecycle of what shows it, held by hand ([TestLifecycle]), with a real view model whose
 * pieces fall one row per tick: a row is a tick, so that the ticks the loop gives show on the piece. The frames are
 * handed by hand (`autoAdvance` off), unless a test lets the clock run to see whether Compose becomes idle. The pause of
 * a window too small to play is tested here too: it is an automatic pause, as that of ON_PAUSE.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenLifecycleTest {
    private val lifecycle = TestLifecycle()

    /** The size given to the screen, the whole window when null. */
    private var size by mutableStateOf<DpSize?>(null)

    /** The times the system back reached the navigation, behind the screen: the game was left. */
    private var backs = 0

    @Test
    @DisplayName("a screen no longer in front (ON_PAUSE) pauses the game")
    fun aScreenNoLongerInFrontPausesTheGame() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way, in front
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When another window comes in front of it: ON_PAUSE, the frames still drawn (STARTED)
            runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.STARTED }

            // Then the game waits for the player, paused, as the card shows once the curtain is down
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("a screen no longer in front while the curtain opens keeps the game paused, its card shown again")
    fun aScreenNoLongerInFrontWhileTheCurtainOpensKeepsTheGamePaused() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused, its card shown, then Reprendre, the curtain halfway up
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)
            runOnUiThread { game.onPause() }
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            onNodeWithText(string(R.string.feature_game_pause_resume)).performClick()
            mainClock.advanceTimeBy(FRAME_MILLIS * 6)

            // When another window comes in front of it: ON_PAUSE
            runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.STARTED }
            mainClock.advanceTimeBy(CURTAIN_MILLIS)

            // Then the game still waits for the player, behind its card
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("a paused game asks for no frame: Compose becomes idle")
    fun aPausedGameAsksForNoFrame() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way, its frames handed by hand
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When the player pauses it
            runOnUiThread { game.onPause() }

            // Then, the clock left to run on its own, Compose has no frame left to draw: it becomes idle. A loop still
            // asking for frames fails here by a timeout, not by an assertion: waitForIdle throws ComposeNotIdleException
            // once the idling resource gives up, after about a minute.
            mainClock.autoAdvance = true
            waitForIdle()
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("a game resumed goes on from where it stood, without catching up the time spent paused")
    fun aResumedGameGoesOnWithoutCatchingUp() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused while its piece was falling, then left paused for ten seconds
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)
            runOnUiThread { game.onPause() }
            val row = runOnIdle { pieceRow(game) }
            mainClock.advanceTimeBy(PAUSE_MILLIS)

            // When the player resumes it, and the first frame comes
            runOnUiThread { game.onResume() }
            waitForIdle()
            mainClock.advanceTimeByFrame()

            // Then that frame only sets the time again: the piece has not moved
            runOnIdle { assertThat(pieceRow(game)).isEqualTo(row) }

            // And the frames that follow make it fall again
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)
            runOnIdle { assertThat(pieceRow(game)).isGreaterThan(row) }
        }
    }

    @Test
    @DisplayName("once the game is over, Rejouer and Menu do nothing until the screen is in front")
    fun onceOverReplayAndMenuDoNothingUntilTheScreenIsInFront() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played until its game over screen shows
            var replays = 0
            var menus = 0
            showGame(onBack = { menus++ }, onReplay = { replays++ })
            val replay = string(R.string.feature_game_over_replay)
            val menu = string(R.string.feature_game_over_menu)
            playUntilShown(replay)

            // When the screen is no longer in front, as while the navigation leaves it, and the player taps both
            runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.STARTED }
            onNodeWithText(replay).performClick()
            onNodeWithText(menu).performClick()

            // Then nothing happens
            runOnIdle {
                assertThat(replays).isZero()
                assertThat(menus).isZero()
            }

            // When the screen is in front again
            runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.RESUMED }
            onNodeWithText(replay).performClick()
            onNodeWithText(menu).performClick()

            // Then each tap acts
            runOnIdle {
                assertThat(replays).isEqualTo(1)
                assertThat(menus).isEqualTo(1)
            }
        }
    }

    @Test
    @DisplayName("in a window too small to play, « Agrandissez la fenêtre » replaces the board, the game paused")
    fun inAWindowTooSmallToPlayTheMessageTakesThePlaceOfTheBoardTheGamePaused() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a window of 220 × 560 dp, the narrowest multi-window, too low: cells under 8 dp

            // When a game starts in it
            size = TooSmall
            val game = showGame()
            val piece = runOnIdle { game.uiState.value.piece }

            // Then the game paused before its first frame, its piece still from the first frame on
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
            repeat(times = 30) {
                mainClock.advanceTimeByFrame()
                runOnIdle { assertThat(game.uiState.value.piece).isEqualTo(piece) }
            }

            // And the message in place of the board, announced by TalkBack as it comes; the pause button still there,
            // disabled: the game is already paused
            onNodeWithText(string(R.string.feature_game_too_small))
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            onNodeWithContentDescription(
                string(R.string.feature_game_top_pause),
            ).assertIsDisplayed().assertIsNotEnabled()
        }
    }

    @Test
    @DisplayName("paused, a window that becomes too small to play shows the message in place of the pause screen")
    fun pausedAWindowThatBecomesTooSmallShowsTheMessageInPlaceOfThePauseScreen() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused in a window large enough: the card of the pause, once the curtain is down
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)
            runOnUiThread { game.onPause() }
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()

            // When the window becomes too small to play
            size = TooSmall
            mainClock.advanceTimeByFrame()

            // Then the message in place of the pause screen: Reprendre is no longer there, the game stays paused
            onNodeWithText(string(R.string.feature_game_too_small)).assertIsDisplayed()
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertDoesNotExist()
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
        }
    }

    @Test
    @DisplayName("under the message, the controls of the game are ignored, even once the game resumes")
    fun underTheMessageTheControlsOfTheGameAreIgnored() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused under the message
            size = TooSmall
            val game = showGame()
            mainClock.advanceTimeByFrame()
            val piece = runOnIdle { game.uiState.value.piece }

            // When the player presses the keys of the controls, the touch controls being hidden
            onNodeWithContentDescription(string(R.string.feature_game_control_left)).assertDoesNotExist()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_X).forEach {
                instrumentation.sendKeyDownUpSync(it)
            }
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // Then the piece has not moved, the game still paused
            runOnIdle {
                assertThat(game.uiState.value.paused).isTrue()
                assertThat(game.uiState.value.piece).isEqualTo(piece)
            }

            // And once the window is large enough and the game resumed, the curtain open, its first frame moves nothing
            size = null
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            onNodeWithText(string(R.string.feature_game_pause_resume)).performClick()
            advanceUntilResumed(game)
            mainClock.advanceTimeByFrame()
            runOnIdle { assertThat(game.uiState.value.piece).isEqualTo(piece) }
        }
    }

    @Test
    @DisplayName("once the window is large enough again, the pause screen shows, and Reprendre resumes the game")
    fun onceTheWindowIsLargeEnoughAgainThePauseScreenShowsAndReprendreResumes() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused under the message, in a window too small to play
            size = TooSmall
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When the window grows
            size = null
            mainClock.advanceTimeBy(CURTAIN_MILLIS)

            // Then the card of the pause, the game still paused, until Reprendre has opened the curtain
            onNodeWithText(string(R.string.feature_game_too_small)).assertDoesNotExist()
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
            onNodeWithText(string(R.string.feature_game_pause_resume)).performClick()
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            runOnIdle { assertThat(game.uiState.value.paused).isFalse() }
        }
    }

    @Test
    @DisplayName("while the window is too small to play, the system back leaves the game")
    fun whileTheWindowIsTooSmallToPlayTheSystemBackLeavesTheGame() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused under the message
            size = TooSmall
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When the player goes back
            runOnUiThread { checkNotNull(activity).onBackPressedDispatcher.onBackPressed() }

            // Then the back reaches the navigation, behind the screen, rather than resume the game
            runOnIdle {
                assertThat(backs).isEqualTo(1)
                assertThat(game.uiState.value.paused).isTrue()
            }
        }
    }

    /**
     * The game screen of a new game at level 0, shown by [lifecycle], in front. Frames handed by hand only: the game
     * under way does not keep Compose busy.
     */
    private fun AndroidComposeUiTest<ComponentActivity>.showGame(
        onBack: () -> Unit = {},
        onReplay: () -> Unit = {},
    ): GameViewModel {
        val activity = checkNotNull(activity)
        val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply {
            set(GameViewModel.RouteKey, GameRoute(startLevel = 0, seed = 1L))
        }
        val factory = silentGameViewModelFactory(OneRowPerTickGameEngineFactory)
        val game =
            runOnUiThread { ViewModelProvider.create(activity.viewModelStore, factory, extras)[GameViewModel::class] }
        mainClock.autoAdvance = false
        runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.RESUMED }
        setContent {
            // The system back, as the navigation would handle it behind the screen: counted.
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                onBackCompleted = { backs++ },
            )
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    val shown = size
                    Box(modifier = if (shown == null) Modifier else Modifier.requiredSize(shown)) {
                        GameScreen(viewModel = game, onBack = onBack, onReplay = onReplay, onCopy = {})
                    }
                }
            }
        }
        return game
    }

    /** Hands frames until [text] shows, a game over screen within seconds of the clock when a row is a tick. */
    private fun AndroidComposeUiTest<ComponentActivity>.playUntilShown(text: String) {
        var played = 0L
        while (onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            check(played < MAX_PLAY_MILLIS) { "« $text » not shown after $MAX_PLAY_MILLIS ms" }
            mainClock.advanceTimeBy(CHECK_MILLIS)
            played += CHECK_MILLIS
        }
    }

    /** Hands frames, one at a time, until the curtain of the pause is open and [game] goes on. */
    private fun AndroidComposeUiTest<ComponentActivity>.advanceUntilResumed(game: GameViewModel) {
        var frames = 0
        while (runOnIdle { game.uiState.value.paused }) {
            check(frames++ < CURTAIN_MILLIS / FRAME_MILLIS) { "The game did not resume once the curtain was open" }
            mainClock.advanceTimeByFrame()
        }
    }

    private fun AndroidComposeUiTest<ComponentActivity>.string(id: Int): String = checkNotNull(activity).getString(id)

    private fun pieceRow(game: GameViewModel): Int = checkNotNull(game.uiState.value.piece).pivot.row

    /** A lifecycle held by hand, from any thread: what shows the screen, in front or not. */
    private class TestLifecycle : LifecycleOwner {
        val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        /** The narrowest multi-window, too low to play: cells under 8 dp. */
        val TooSmall = DpSize(220.dp, 560.dp)

        /** The duration of a frame of the clock of the test. */
        const val FRAME_MILLIS = 16L

        /** More than the 12 ticks of the curtain of the pause (`motion-curtain`), with the frame that starts it. */
        const val CURTAIN_MILLIS = FRAME_MILLIS * 16

        /** Ten seconds in pause: far more than the four ticks a frame may give. */
        const val PAUSE_MILLIS = 10_000L

        /** Between two searches of the game over screen. */
        const val CHECK_MILLIS = 500L

        /** A minute of play: far more than a game whose rows are ticks lasts. */
        const val MAX_PLAY_MILLIS = 60_000L
    }
}
