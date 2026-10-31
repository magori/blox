package ch.magori.blox.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.feature.game.navigation.GameRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The game over of Couleurs (P5.83, V30): the stack goes out row by row, from the bottom, then the card shows over the
 * game. A real view model whose pieces fall one row per tick, played to its end. The frames are handed by hand
 * (`autoAdvance` off): a frame of the clock of the test lasts 16 ms, a tick 16.7 ms.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenGameOverTest {
    @Test
    @DisplayName("the card of the game over shows once the stack has gone out, 2 ticks a row")
    fun theCardShowsOnceTheStackHasGoneOut() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played to its end
            showGameOver(showGame())

            // When the stack goes out for 36 frames, under 36 ticks: less than its 20 rows of 2 ticks
            mainClock.advanceTimeBy(FRAME_MILLIS * 36)

            // Then no card yet
            onNodeWithText(string(R.string.feature_game_over_replay)).assertDoesNotExist()

            // And once the stack is out, 48 frames in all, some 46 ticks, the card shows
            mainClock.advanceTimeBy(FRAME_MILLIS * 12)
            onNodeWithText(string(R.string.feature_game_over_replay)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the game loop stays stopped while the game over plays")
    fun theGameLoopStaysStoppedWhileTheGameOverPlays() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played to its end
            val game = showGame()
            showGameOver(game)
            val over = game.uiState.value

            // When its game over plays, the stack going out and the score counting up
            mainClock.advanceTimeBy(FRAME_MILLIS * 100)

            // Then the game has played no tick: its state is the one it ended with
            runOnIdle {
                assertThat(game.uiState.value).isSameAs(over)
                assertThat(over.running).isFalse()
            }

            // And once it has played, Compose has no frame left to draw: it becomes idle, the card shown
            mainClock.autoAdvance = true
            waitForIdle()
            onNodeWithText(string(R.string.feature_game_over_replay)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("in reduced effects the card shows as soon as the game is over")
    fun inReducedEffectsTheCardShowsAsSoonAsTheGameIsOver() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played to its end, in reduced effects
            showGameOver(showGame(effects = BloxEffects(reduced = true, forcedBySystem = false)))

            // When a few frames pass, those of the fade of the card (8 ticks)
            mainClock.advanceTimeBy(FRAME_MILLIS * 12)

            // Then the card shows
            onNodeWithText(string(R.string.feature_game_over_replay)).assertIsDisplayed()
        }
    }

    /** A game under way, shown by a real view model whose pieces fall one row per tick, in Couleurs, in [effects]. */
    private fun AndroidComposeUiTest<ComponentActivity>.showGame(
        effects: BloxEffects = BloxEffects.Full,
    ): GameViewModel {
        val activity = checkNotNull(activity)
        val extras = MutableCreationExtras(activity.defaultViewModelCreationExtras).apply {
            set(GameViewModel.RouteKey, GameRoute(startLevel = 0, seed = 1L))
        }
        val factory = silentGameViewModelFactory(OneRowPerTickGameEngineFactory)
        val game =
            runOnUiThread { ViewModelProvider.create(activity.viewModelStore, factory, extras)[GameViewModel::class] }
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL, effects = effects) {
                GameScreen(viewModel = game, onBack = {}, onReplay = {}, onCopy = {})
            }
        }
        return game
    }

    /** [game] played frame by frame, until its game over is known to the screen. */
    private fun AndroidComposeUiTest<ComponentActivity>.showGameOver(game: GameViewModel) {
        var frames = 0
        while (game.uiState.value.gameOver == null) {
            check(frames < MAX_FRAMES) { "No game over after $MAX_FRAMES frames" }
            mainClock.advanceTimeByFrame()
            frames++
        }
    }

    private fun AndroidComposeUiTest<ComponentActivity>.string(id: Int): String = checkNotNull(activity).getString(id)

    private companion object {
        /** The duration of a frame of the clock of the test. */
        const val FRAME_MILLIS = 16L

        /** Far more than a game whose pieces fall one row per tick lasts. */
        const val MAX_FRAMES = 3_600
    }
}
