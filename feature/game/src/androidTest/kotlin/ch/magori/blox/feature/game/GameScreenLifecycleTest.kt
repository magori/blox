package ch.magori.blox.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.MutableCreationExtras
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
 * handed by hand (`autoAdvance` off), unless a test lets the clock run to see whether Compose becomes idle.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenLifecycleTest {
    private val lifecycle = TestLifecycle()

    @Test
    @DisplayName("a screen no longer in front (ON_PAUSE) pauses the game")
    fun aScreenNoLongerInFrontPausesTheGame() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way, in front
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When another window comes in front of it: ON_PAUSE, the frames still drawn (STARTED)
            runOnUiThread { lifecycle.registry.currentState = Lifecycle.State.STARTED }

            // Then the game waits for the player, paused, as the next frame shows
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }
            mainClock.advanceTimeByFrame()
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
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GameScreen(viewModel = game, onBack = onBack, onReplay = onReplay)
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

    private fun AndroidComposeUiTest<ComponentActivity>.string(id: Int): String = checkNotNull(activity).getString(id)

    private fun pieceRow(game: GameViewModel): Int = checkNotNull(game.uiState.value.piece).pivot.row

    /** A lifecycle held by hand, from any thread: what shows the screen, in front or not. */
    private class TestLifecycle : LifecycleOwner {
        val registry: LifecycleRegistry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private companion object {
        /** The duration of a frame of the clock of the test. */
        const val FRAME_MILLIS = 16L

        /** Ten seconds in pause: far more than the four ticks a frame may give. */
        const val PAUSE_MILLIS = 10_000L

        /** Between two searches of the game over screen. */
        const val CHECK_MILLIS = 500L

        /** A minute of play: far more than a game whose rows are ticks lasts. */
        const val MAX_PLAY_MILLIS = 60_000L
    }
}
