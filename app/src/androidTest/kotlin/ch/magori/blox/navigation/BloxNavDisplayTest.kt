package ch.magori.blox.navigation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeMusicFileRepository
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ch.magori.blox.feature.game.R as GameR
import ch.magori.blox.feature.home.R as HomeR

/**
 * The journeys that cross the features, on the navigation of the application ([BloxNavDisplay]) and the shared fakes:
 * home, game, game over, and the system back. Only `app` knows every feature (ADR 0011).
 *
 * The frames are handed by hand (`autoAdvance` off): a game under way asks for a frame at each frame, and would keep
 * Compose from ever being idle. [settle] lets the clock run long enough for a change of screen and its transition.
 * The games that must end use [OneRowPerTickGameEngineFactory]: over within seconds of the clock, without a move.
 */
@OptIn(ExperimentalTestApi::class)
internal class BloxNavDisplayTest {
    @Test
    @DisplayName("Jouer starts a game at the start level chosen on the home screen")
    fun playStartsAGameAtTheChosenLevel() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given the home screen, level 7 chosen in place of the default level, 0 (UserPreferences.DEFAULT)
            showApp()
            onNode(hasTestTag(START_LEVEL_TAG) and hasText("0")).assertIsSelected()
            chooseStartLevel(7)

            // When
            play()

            // Then the game under way is at level 7
            onNodeWithContentDescription(levelDescription(7)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("in a game under way, the system back pauses the game, which stays on screen")
    fun inAGameTheSystemBackPausesIt() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game under way
            showApp()
            play()

            // When
            pressSystemBack()

            // Then, the transition over, the pause screen shows, and the home screen did not come back
            onNodeWithText(string(GameR.string.feature_game_pause_title)).assertIsDisplayed()
            onNodeWithText(string(GameR.string.feature_game_pause_resume)).assertIsDisplayed()
            onNodeWithText(string(HomeR.string.feature_home_play)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("in a paused game, the system back resumes it")
    fun inAPausedGameTheSystemBackResumesIt() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game paused by its button
            showApp()
            play()
            onNodeWithContentDescription(string(GameR.string.feature_game_top_pause)).performClick()
            settle()
            onNodeWithText(string(GameR.string.feature_game_pause_resume)).assertIsDisplayed()

            // When
            pressSystemBack()

            // Then the game goes on
            onNodeWithText(string(GameR.string.feature_game_pause_resume)).assertDoesNotExist()
            onNodeWithContentDescription(string(GameR.string.feature_game_top_pause)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("once the game is over, the system back goes back to the home screen")
    fun onceOverTheSystemBackGoesHome() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played until its game over screen shows
            showApp(OneRowPerTickGameEngineFactory)
            play()
            advanceUntilShown(string(GameR.string.feature_game_over_replay))

            // When
            pressSystemBack()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(GameR.string.feature_game_over_replay)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("once the game is over, Rejouer starts a new game at its level, in place of the finished one")
    fun replayStartsANewGameInPlaceOfTheFinishedOne() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game at level 3 played until its game over screen shows
            val replay = string(GameR.string.feature_game_over_replay)
            showApp(OneRowPerTickGameEngineFactory)
            chooseStartLevel(3)
            play()
            advanceUntilShown(replay)

            // When the player taps Rejouer
            onNodeWithText(replay).performClick()
            settle()

            // Then a new game is under way, at level 3
            onNodeWithText(replay).assertDoesNotExist()
            onNodeWithContentDescription(levelDescription(3)).assertIsDisplayed()

            // And it took the place of the finished game: its Menu, from the pause, goes back home, not to the game over
            pressSystemBack()
            onNodeWithText(string(GameR.string.feature_game_pause_menu)).performClick()
            settle()
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(replay).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("once the game is over, Menu goes back to the home screen")
    fun onceOverMenuGoesHome() {
        runAndroidComposeUiTest<ComponentActivity> {
            // Given a game played until its game over screen shows
            val replay = string(GameR.string.feature_game_over_replay)
            showApp(OneRowPerTickGameEngineFactory)
            play()
            advanceUntilShown(replay)

            // When
            onNodeWithText(string(GameR.string.feature_game_over_menu)).performClick()
            settle()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(replay).assertDoesNotExist()
        }
    }

    /**
     * The navigation of the application, on the home screen, with the shared fakes: no record, the default settings, no
     * music; the games made by [engineFactory].
     */
    private fun AndroidComposeUiTest<ComponentActivity>.showApp(
        engineFactory: GameEngineFactory = ClassicGameEngineFactory,
    ) {
        val preferences = FakeUserPreferencesRepository()
        val highScores = FakeHighScoreRepository()
        val music = FakeMusicPlayer()
        val library = FakeMusicLibrary()
        val deletedMusics = DeletedMusicRelay()
        val homeFactory = HomeViewModel.Factory(
            preferences,
            highScores,
            ClassicRules.levelProgression,
            music,
            scoreOf = { null },
            musicFiles = FakeMusicFileRepository(),
            library = library,
        )
        val gameFactory = GameViewModel.Factory(
            engineFactory,
            highScores,
            preferences,
            music,
            FakeHaptics(),
            musicOf = { null },
            gameMusic = { null },
        )
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                BloxNavDisplay(
                    homeViewModelFactory = homeFactory,
                    gameViewModelFactory = gameFactory,
                    composerViewModelFactory = ComposerViewModel.Factory(music, library),
                    myMusicsViewModelFactory = MyMusicsViewModel.Factory(library, music, deletedMusics),
                    deletedMusics = deletedMusics,
                )
            }
        }
        // The home screen shows once its view model read the settings, which may answer after a first advance.
        advanceUntilShown(string(HomeR.string.feature_home_play))
    }

    /** Chooses the start level [level] on the home screen, by its cell: no other text of the screen can be taken for it. */
    private fun AndroidComposeUiTest<ComponentActivity>.chooseStartLevel(level: Int) {
        onNode(hasTestTag(START_LEVEL_TAG) and hasText(level.toString())).performScrollTo().performClick()
        settle()
    }

    /** Taps Jouer on the home screen, and waits for the game screen. */
    private fun AndroidComposeUiTest<ComponentActivity>.play() {
        onNodeWithText(string(HomeR.string.feature_home_play)).performScrollTo().performClick()
        settle()
    }

    /** The system back, as the back gesture or button gives it to the activity, which hands it to the navigation. */
    private fun AndroidComposeUiTest<ComponentActivity>.pressSystemBack() {
        val activity = checkNotNull(activity)
        runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        settle()
    }

    /** Lets the clock run long enough for a change of screen and its transition. */
    private fun AndroidComposeUiTest<ComponentActivity>.settle() {
        mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    /**
     * Hands frames until [text] shows: the home screen once its settings are read, or a game over screen, within seconds
     * of the clock when a row is a tick (`OneRowPerTickTest`).
     */
    private fun AndroidComposeUiTest<ComponentActivity>.advanceUntilShown(text: String) {
        var played = 0L
        while (onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            check(played < MAX_PLAY_MILLIS) { "« $text » not shown after $MAX_PLAY_MILLIS ms" }
            mainClock.advanceTimeBy(CHECK_MILLIS)
            played += CHECK_MILLIS
        }
        settle()
    }

    /** The level beside the board, as TalkBack reads it. */
    private fun AndroidComposeUiTest<ComponentActivity>.levelDescription(level: Int): String {
        val label = string(GameR.string.feature_game_level)
        return checkNotNull(activity).getString(GameR.string.feature_game_value_description, label, "$level")
    }

    private fun AndroidComposeUiTest<ComponentActivity>.string(@StringRes id: Int): String =
        checkNotNull(activity).getString(id)

    private companion object {
        /** A change of screen and its transition, well within: a second of the clock, sixty ticks of a game. */
        const val SETTLE_MILLIS = 1_000L

        /** The tag of a start level cell of the home screen (`START_LEVEL_TAG` of feature:home, internal to it). */
        const val START_LEVEL_TAG = "home_start_level"

        /** Between two searches of the game over screen. */
        const val CHECK_MILLIS = 500L

        /** A minute of play: far more than a game whose rows are ticks lasts. */
        const val MAX_PLAY_MILLIS = 60_000L
    }
}
