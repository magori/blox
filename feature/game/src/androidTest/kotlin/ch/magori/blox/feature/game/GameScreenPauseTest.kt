package ch.magori.blox.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.game.pause.PAUSE_MASCOT_TAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The pause in a curtain (P5.82): a curtain of empty cells comes down over the board and the next piece, then the
 * card shows; Reprendre, or the system back, opens the curtain, and only then does the game go on. A real view model
 * whose pieces fall one row per tick: the loop of the game, stopped through the whole curtain, shows on the piece.
 * The frames are handed by hand (`autoAdvance` off): a frame of the clock of the test lasts 16 ms, a tick 16.7 ms.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenPauseTest {
    @Test
    @DisplayName("the curtain hides the board and the next piece before the card shows")
    fun theCurtainHidesTheBoardBeforeTheCardShows() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way
            val game = showGame()
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When the player pauses it, and the curtain comes down for half its 12 ticks
            runOnUiThread { game.onPause() }
            mainClock.advanceTimeBy(HALF_CURTAIN_MILLIS)

            // Then no card yet: the curtain still comes down
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertDoesNotExist()

            // And once the curtain is down, the card shows
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the game resumes only once the curtain is open")
    fun theGameResumesOnlyOnceTheCurtainIsOpen() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused, its card shown
            val game = showPausedGame()
            val row = runOnIdle { pieceRow(game) }

            // When the player taps Reprendre, and the curtain goes back up for half its 12 ticks
            onNodeWithText(string(R.string.feature_game_pause_resume)).performClick()
            mainClock.advanceTimeBy(HALF_CURTAIN_MILLIS)

            // Then the card is gone, but the game still waits, its piece where it stood
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertDoesNotExist()
            runOnIdle {
                assertThat(game.uiState.value.paused).isTrue()
                assertThat(pieceRow(game)).isEqualTo(row)
            }

            // And once the curtain is open, the game goes on
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            runOnIdle { assertThat(game.uiState.value.paused).isFalse() }
        }
    }

    @Test
    @DisplayName("in reduced effects the curtain fades in 8 ticks at most")
    fun inReducedEffectsTheCurtainFadesIn8TicksAtMost() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way, in reduced effects
            val game = showGame(effects = BloxEffects(reduced = true, forcedBySystem = false))
            mainClock.advanceTimeBy(FRAME_MILLIS * 5)

            // When the player pauses it, and 8 ticks pass, with the frame that starts the curtain: less than its 12
            runOnUiThread { game.onPause() }
            mainClock.advanceTimeBy(REDUCED_FADE_MILLIS)

            // Then the curtain is shut: the card comes in, by a fade too
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertExists()
        }
    }

    @Test
    @DisplayName("the system back resumes as Reprendre does")
    fun theSystemBackResumesAsReprendreDoes() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused, its card shown
            val game = showPausedGame()

            // When the player goes back
            runOnUiThread { checkNotNull(activity).onBackPressedDispatcher.onBackPressed() }
            mainClock.advanceTimeBy(HALF_CURTAIN_MILLIS)

            // Then the curtain opens: the card is gone, the game still waits
            onNodeWithText(string(R.string.feature_game_pause_resume)).assertDoesNotExist()
            runOnIdle { assertThat(game.uiState.value.paused).isTrue() }

            // And once the curtain is open, the game goes on
            mainClock.advanceTimeBy(CURTAIN_MILLIS)
            runOnIdle { assertThat(game.uiState.value.paused).isFalse() }
        }
    }

    @Test
    @DisplayName("the card shows the Classic mascot asleep above Pause, a decoration TalkBack does not read")
    fun theCardShowsTheMascotAsleepAbovePause() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused, its card shown
            showPausedGame()

            // Then the mascot stands above the title, and is not read
            val mascot = onNode(hasTestTag(PAUSE_MASCOT_TAG), useUnmergedTree = true)
            val title = onNodeWithText(string(R.string.feature_game_pause_title)).getUnclippedBoundsInRoot()
            assertThat(mascot.getUnclippedBoundsInRoot().bottom).isLessThanOrEqualTo(title.top)
            mascot.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        }
    }

    @Test
    @DisplayName("TalkBack reads the card once it shows, by its title, and nothing of the game under it")
    fun talkBackReadsTheCardOnceItShows() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused, its card shown
            showPausedGame()

            // Then the card is announced by its title, and the board, under the curtain, is no longer read
            val title = string(R.string.feature_game_pause_title)
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)).assertExists()
            onAllNodes(hasBoardDescription).assertCountEquals(0)
        }
    }

    /** A game under way, shown by a real view model whose pieces fall one row per tick, in [effects]. */
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
                GameScreen(viewModel = game, onBack = {}, onReplay = {})
            }
        }
        return game
    }

    /** A game paused once under way, the curtain down, the card shown. */
    private fun AndroidComposeUiTest<ComponentActivity>.showPausedGame(): GameViewModel {
        val game = showGame()
        mainClock.advanceTimeBy(FRAME_MILLIS * 5)
        runOnUiThread { game.onPause() }
        mainClock.advanceTimeBy(CURTAIN_MILLIS * 2)
        onNodeWithText(string(R.string.feature_game_pause_resume)).assertIsDisplayed()
        check(onAllNodesWithText(string(R.string.feature_game_pause_resume)).fetchSemanticsNodes().size == 1)
        return game
    }

    private fun AndroidComposeUiTest<ComponentActivity>.string(id: Int): String = checkNotNull(activity).getString(id)

    private fun pieceRow(game: GameViewModel): Int = checkNotNull(game.uiState.value.piece).pivot.row

    private companion object {
        /** The duration of a frame of the clock of the test. */
        const val FRAME_MILLIS = 16L

        /** About 6 ticks of the 12 of the curtain (`motion-curtain`, 200 ms). */
        const val HALF_CURTAIN_MILLIS = FRAME_MILLIS * 6

        /** More than the 12 ticks of the curtain, with the frame that starts it. */
        const val CURTAIN_MILLIS = FRAME_MILLIS * 16

        /**
         * The 8 ticks of `motion-reduced-fade`, 133 ms, with the frames that start the fade: under the 12 ticks of
         * the curtain, which needs 16 frames from the pause.
         */
        const val REDUCED_FADE_MILLIS = FRAME_MILLIS * 13

        /** What TalkBack reads of the board, whatever the stack. */
        val hasBoardDescription = SemanticsMatcher("is the board") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { it.startsWith("Plateau") }
        }
    }
}
