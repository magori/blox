package ch.magori.blox.navigation

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeMusicFileRepository
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores
import ch.magori.blox.daily.DailySeed
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.daily.DailyViewModel
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.SettingsViewModel
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import ch.magori.blox.core.designsystem.R as DesignR
import ch.magori.blox.feature.cartridges.R as CartridgesR
import ch.magori.blox.feature.daily.R as DailyR
import ch.magori.blox.feature.game.R as GameR
import ch.magori.blox.feature.home.R as HomeR
import ch.magori.blox.feature.locksmith.R as LocksmithR

/**
 * The journeys that cross the features, on the navigation of the application ([BloxNavDisplay]) and the shared fakes:
 * choice of the game, home, game, game over, and the system back. Only `app` knows every feature (ADR 0011, 0022).
 *
 * The frames are handed by hand (`autoAdvance` off): a game under way asks for a frame at each frame, and would keep
 * Compose from ever being idle. [settle] lets the clock run long enough for a change of screen and its transition.
 * The games that must end use [OneRowPerTickGameEngineFactory]: over within seconds of the clock, without a move.
 */
@OptIn(ExperimentalTestApi::class)
internal class BloxNavDisplayTest {
    /** The system backs that went through the navigation down to the activity. */
    private var backsToTheSystem = 0

    /** The dates whose daily score the navigation asked to copy. */
    private val copiedDailyScores = mutableListOf<LocalDate>()

    @Test
    @DisplayName("the application opens on the choice of the game")
    fun theApplicationOpensOnTheChoiceOfTheGame() {
        runJourney {
            // Given / When
            showApp(openClassic = false)

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            onNode(hasTestTag(CLASSIC_CARTRIDGE_TAG)).assertIsDisplayed()
            onNodeWithText(string(HomeR.string.feature_home_play)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("the Classic cartridge opens the Classic home")
    fun theClassicCartridgeOpensTheClassicHome() {
        runJourney {
            // Given the choice of the game
            showApp(openClassic = false)

            // When
            tapClassicCartridge()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("the system back on the cartridges leaves the application")
    fun theSystemBackOnTheCartridgesLeavesTheApplication() {
        runJourney {
            // Given the choice of the game
            showApp(openClassic = false)

            // When / Then nothing of the application lies under the cartridges
            assertTheSystemBackLeavesTheApplication()
        }
    }

    @Test
    @DisplayName("the Locksmith cartridge opens padlock 1")
    fun theLocksmithCartridgeOpensPadlock1() {
        runJourney {
            // Given the choice of the game
            showApp(openClassic = false)

            // When
            tapLocksmithCartridge()

            // Then
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("on the Locksmith, the system back goes back to the cartridges, and then leaves")
    fun onTheLocksmithTheSystemBackGoesBackToTheCartridges() {
        runJourney {
            // Given padlock 1, opened from the cartridges
            showApp(openClassic = false)
            tapLocksmithCartridge()

            // When
            pressSystemBack()

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            assertTheSystemBackLeavesTheApplication()
        }
    }

    @Test
    @DisplayName("on the Locksmith, the back button of the bar goes back to the cartridges")
    fun onTheLocksmithTheBackButtonGoesBackToTheCartridges() {
        runJourney {
            // Given padlock 1, opened from the cartridges
            showApp(openClassic = false)
            tapLocksmithCartridge()

            // When
            onNodeWithContentDescription(string(LocksmithR.string.feature_locksmith_back)).performClick()
            settle()

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            onNodeWithText(padlockTitle(1)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("a restored stack with a padlock on top shows that padlock again, nothing placed")
    fun aRestoredStackWithAPadlockOnTopShowsItAgain() {
        runJourney {
            // Given padlock 2 of seed 7 on top, as the system restores the stack after the death of the process
            showApp(
                restoredStack = listOf(CartridgesRoute, LocksmithRoute(level = 2, seed = 7)),
                restoredShows = padlockTitle(2),
            )

            // Then
            onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 0)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the entry of the daily seed shows the day and its first pieces")
    fun theEntryOfTheDailySeedShowsTheDayAndItsFirstPieces() {
        runJourney {
            // Given / When the entry on top of the cartridges, on the 26th of October 2026
            showApp(restoredStack = listOf(CartridgesRoute, DailyRoute), restoredShows = dailyTitle())

            // Then the first of the seven first pieces of seed 20261026
            onNodeWithContentDescription(string(DailyR.string.feature_daily_piece, 1, "Z")).assertExists()
        }
    }

    @Test
    @DisplayName("Jouer on the entry of the daily seed opens the daily game, at level 5")
    fun playOnTheEntryOfTheDailySeedOpensTheDailyGame() {
        runJourney {
            // Given
            showApp(restoredStack = listOf(CartridgesRoute, DailyRoute), restoredShows = dailyTitle())

            // When
            onNodeWithContentDescription(string(DailyR.string.feature_daily_play_description, 5))
                .performScrollTo()
                .performClick()
            settle()

            // Then a game of level 5, whatever the level chosen on the home of the Classic
            onNodeWithContentDescription(levelDescription(5)).assertIsDisplayed()
            onNodeWithText(dailyTitle()).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("changing the game from the entry of the daily seed goes back to the cartridges")
    fun changingTheGameFromTheEntryOfTheDailySeedGoesBackToTheCartridges() {
        runJourney {
            // Given
            showApp(restoredStack = listOf(CartridgesRoute, DailyRoute), restoredShows = dailyTitle())

            // When
            onNodeWithContentDescription(string(DailyR.string.feature_daily_change_game)).performClick()
            settle()

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            onNodeWithText(dailyTitle()).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("Copier mon score on the entry of the daily seed copies the score of the date shown")
    fun copyOnTheEntryOfTheDailySeedCopiesTheScoreOfTheDateShown() {
        runJourney {
            // Given the entry of the 26th of October, after a try that day
            showApp(
                restoredStack = listOf(CartridgesRoute, DailyRoute),
                restoredShows = dailyTitle(),
                dailyResults = DailyResults(DAILY_DATE, tries = 1, firstTry = 300, best = 300, firstTryOver = true),
            )

            // When
            onNodeWithText(string(DailyR.string.feature_daily_copy_score)).performScrollTo().performClick()
            settle()

            // Then the copy of the score of that date is asked for
            assertThat(copiedDailyScores).containsExactly(DAILY_DATE)
        }
    }

    @Test
    @DisplayName("Copier at the game over of a daily game copies the score of the date of the game")
    fun copyAtTheGameOverOfADailyGameCopiesTheScoreOfItsDate() {
        runJourney {
            // Given a daily game of the 26th of October, played from its entry until its game over
            showApp(restoredStack = listOf(CartridgesRoute, DailyRoute), restoredShows = dailyTitle())
            onNodeWithContentDescription(string(DailyR.string.feature_daily_play_description, 5))
                .performScrollTo()
                .performClick()
            advanceUntilShown(string(GameR.string.feature_game_over_replay))

            // When
            onNodeWithContentDescription(string(GameR.string.feature_game_over_copy_description))
                .performScrollTo()
                .performClick()
            settle()

            // Then the copy of the score of the date of the game is asked for
            assertThat(copiedDailyScores).containsExactly(DAILY_DATE)
        }
    }

    @Test
    @DisplayName("changing the game from the home goes back to the cartridges, and the system back then leaves")
    fun changingTheGameFromTheHomeGoesBackToTheCartridges() {
        runJourney {
            // Given the home of the Classic, opened from the cartridges
            showApp()

            // When
            tapChangeGame()

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            onNodeWithText(string(HomeR.string.feature_home_play)).assertDoesNotExist()

            // And the home did not stay under them
            assertTheSystemBackLeavesTheApplication()
        }
    }

    @Test
    @DisplayName("changing the game from a restored stack without cartridges puts them at its root")
    fun changingTheGameFromARestoredStackWithoutCartridgesPutsThemAtItsRoot() {
        runJourney {
            // Given the home alone, as a stack saved before the cartridges came (ADR 0022) is restored
            showApp(restoredStack = listOf(HomeRoute))

            // When
            tapChangeGame()

            // Then the cartridges show, alone on the stack: the system back leaves the application
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            assertTheSystemBackLeavesTheApplication()
        }
    }

    @Test
    @DisplayName("changing the game stops the music listened to on the home screen at once, before the transition")
    fun changingTheGameStopsTheMusicListenedToAtOnce() {
        runJourney {
            // Given the minuet listened to on the home screen
            val music = FakeMusicPlayer()
            showApp(music = music, scoreOf = mapOf(MusicChoice.MINUET to Scores.minuet)::get)
            listenToTheMinuet()
            assertThat(music.playing).isNotNull()

            // When the disc is tapped, the clock held: the home is still on screen, its transition not begun
            onNodeWithContentDescription(string(HomeR.string.feature_home_change_game)).performScrollTo().performClick()

            // Then
            assertThat(music.playing).isNull()
            assertThat(music.calls.last()).isEqualTo("stop")
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
        }
    }

    @Test
    @DisplayName("Jouer starts a game at the start level chosen on the home screen")
    fun playStartsAGameAtTheChosenLevel() {
        runJourney {
            // Given the home screen, level 7 chosen in place of the default level, 0 (UserPreferences.DEFAULT)
            showApp()
            onNode(startLevelGauge()).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0"))
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
        runJourney {
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
        runJourney {
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
        runJourney {
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
        runJourney {
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
    @DisplayName("the game over of a classic game offers no Copier, and asks for no copy")
    fun theGameOverOfAClassicGameAsksForNoCopy() {
        runJourney {
            // Given / When a classic game played until its game over screen shows
            showApp(OneRowPerTickGameEngineFactory)
            play()
            advanceUntilShown(string(GameR.string.feature_game_over_replay))

            // Then nothing to copy
            onNodeWithContentDescription(string(GameR.string.feature_game_over_copy_description)).assertDoesNotExist()
            assertThat(copiedDailyScores).isEmpty()
        }
    }

    @Test
    @DisplayName("once the game is over, Menu goes back to the home screen")
    fun onceOverMenuGoesHome() {
        runJourney {
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

    @Test
    @DisplayName("the gear of the home screen opens the settings, whose switches save, and Retour goes back home")
    fun theGearOpensTheSettingsAndRetourGoesBackHome() {
        runJourney {
            // Given the home screen, the vibrations off by default
            val preferences = showApp()

            // When the player opens the settings and turns the vibrations on
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings)).performClick()
            settle()
            onNodeWithText(string(HomeR.string.feature_home_vibrations)).performClick()
            settle()

            // Then they are saved, for the next game
            assertThat(preferences.state.value?.vibrations).isTrue()

            // When
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings_back)).performClick()
            settle()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(HomeR.string.feature_home_vibrations)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("on the settings, the system back goes back to the home screen")
    fun onTheSettingsTheSystemBackGoesHome() {
        runJourney {
            // Given the settings, opened from the home screen
            showApp()
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings)).performClick()
            settle()

            // When
            pressSystemBack()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(HomeR.string.feature_home_vibrations)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("while the saved settings are not read, the settings show their back arrow and title, and no switch")
    fun whileTheSettingsAreNotReadTheyShowTheirHeaderAndNoSwitch() {
        runJourney {
            // Given the home screen, and settings of the page that have not answered yet
            showApp(settingsPreferences = FakeUserPreferencesRepository(initial = null))

            // When
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings)).performClick()
            settle()

            // Then
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings_back)).assertExists()
            onNodeWithText(string(HomeR.string.feature_home_settings)).assertExists()
            onNodeWithText(string(HomeR.string.feature_home_vibrations)).assertDoesNotExist()
            onNodeWithText(string(HomeR.string.feature_home_sound_effects)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("opening the settings stops the music listened to on the home screen")
    fun openingTheSettingsStopsTheMusicListenedTo() {
        runJourney {
            // Given the minuet listened to on the home screen
            val music = FakeMusicPlayer()
            showApp(music = music, scoreOf = mapOf(MusicChoice.MINUET to Scores.minuet)::get)
            listenToTheMinuet()
            assertThat(music.playing).isNotNull()

            // When
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings)).performScrollTo().performClick()
            settle()

            // Then
            assertThat(music.playing).isNull()
            assertThat(music.calls.last()).isEqualTo("stop")
        }
    }

    @Test
    @DisplayName("opening the settings stops the music of the player listened to on the home screen")
    fun openingTheSettingsStopsTheMusicOfThePlayerListenedTo() {
        runJourney {
            // Given a music of the player chosen for the games, listened to on the home screen
            val music = FakeMusicPlayer()
            val library = FakeMusicLibrary()
            runBlocking {
                val id = library.newId()
                val score = Scores.minuet.copy(id = id)
                library.save(SavedMusic(id, GAME_MUSIC, null, Scale(Pitch.parse("C4"), ScaleType.MAJOR), score))
                library.setGameMusic(id)
            }
            showApp(music = music, library = library)
            onNodeWithContentDescription(listenDescription(GAME_MUSIC)).performScrollTo().performClick()
            settle()
            assertThat(music.playing).isNotNull()

            // When
            onNodeWithContentDescription(string(HomeR.string.feature_home_settings)).performScrollTo().performClick()
            settle()

            // Then
            assertThat(music.playing).isNull()
            assertThat(music.calls.last()).isEqualTo("stop")
        }
    }

    /**
     * Runs a journey of [block] with more than the minute a Compose test is given by default: on the emulator shared by
     * several test processes, a journey through three screens may take longer, without hanging.
     */
    private fun runJourney(block: suspend AndroidComposeUiTest<ComponentActivity>.() -> Unit) =
        runAndroidComposeUiTest<ComponentActivity>(testTimeout = JOURNEY_TIMEOUT, block = block)

    /** Chooses the minuet on the home screen, then listens to it. */
    private fun AndroidComposeUiTest<ComponentActivity>.listenToTheMinuet() {
        val minuet = string(HomeR.string.feature_home_music_minuet)
        onNodeWithText(minuet).performScrollTo().performClick()
        settle()
        onNodeWithContentDescription(listenDescription(minuet)).performScrollTo().performClick()
        settle()
    }

    /** « Écouter » and the name of the music [name], as TalkBack reads the listen button of the home screen. */
    private fun AndroidComposeUiTest<ComponentActivity>.listenDescription(name: String): String =
        checkNotNull(activity).getString(DesignR.string.core_designsystem_listen_named, name)

    /**
     * The navigation of the application, with the shared fakes: no record, the default settings, no music file; the
     * games made by [engineFactory], the musics played by [music], the scores of the music choices given by [scoreOf],
     * the musics of the player in [library]; the settings page reads [settingsPreferences], the settings of the
     * application unless given. It starts on the choice of the game and opens the home of the Classic, unless
     * [openClassic] is false; or on [restoredStack], as a stack saved by an older version comes back. Gives the
     * settings of the application.
     */
    private fun AndroidComposeUiTest<ComponentActivity>.showApp(
        engineFactory: GameEngineFactory = ClassicGameEngineFactory,
        music: FakeMusicPlayer = FakeMusicPlayer(),
        library: FakeMusicLibrary = FakeMusicLibrary(),
        scoreOf: (MusicChoice) -> Score? = { null },
        settingsPreferences: FakeUserPreferencesRepository? = null,
        openClassic: Boolean = true,
        restoredStack: List<NavKey>? = null,
        restoredShows: String? = null,
        dailyResults: DailyResults? = null,
    ): FakeUserPreferencesRepository {
        val preferences = FakeUserPreferencesRepository()
        val highScores = FakeHighScoreRepository()
        val deletedMusics = DeletedMusicRelay()
        val dailyChallenge = FakeDailyChallengeRepository(dailyResults)
        val homeFactory = HomeViewModel.Factory(
            preferences,
            highScores,
            ClassicRules.levelProgression,
            music,
            scoreOf = scoreOf,
            musicFiles = FakeMusicFileRepository(),
            library = library,
        )
        val gameFactory = GameViewModel.Factory(
            engineFactory,
            highScores,
            preferences,
            music,
            FakeHaptics(),
            FakeSoundEffectPlayer(),
            musicOf = { null },
            gameMusic = { null },
            dailyChallenge = dailyChallenge,
            appScope = MainScope(),
        )
        // Below every screen: a system back that reaches it is one the navigation let go, to leave the application.
        val activity = checkNotNull(activity)
        runOnUiThread {
            activity.onBackPressedDispatcher.addCallback(
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        backsToTheSystem++
                    }
                },
            )
        }
        mainClock.autoAdvance = false
        val composerFactory = ComposerViewModel.Factory(music, library)
        val myMusicsFactory = MyMusicsViewModel.Factory(library, music, deletedMusics)
        val locksmithFactory = LocksmithViewModel.Factory(FakeSoundEffectPlayer(), preferences)
        val dailyFactory = DailyViewModel.Factory(
            dailyChallenge,
            GameRoute.DAILY_START_LEVEL,
            today = { DAILY_DATE },
            seedOf = DailySeed::of,
        )
        val settingsFactory = SettingsViewModel.Factory(
            settingsPreferences ?: preferences,
            highScores,
            FakeSoundEffectPlayer(),
            FakeHaptics(),
        )
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                // The stack of the application unless one is restored: the first screen is the one it gives.
                if (restoredStack == null) {
                    BloxNavDisplay(
                        homeFactory,
                        gameFactory,
                        composerFactory,
                        myMusicsFactory,
                        settingsFactory,
                        deletedMusics,
                        locksmithFactory,
                        dailyFactory,
                        { date -> copiedDailyScores += date },
                    )
                } else {
                    BloxNavDisplay(
                        homeFactory,
                        gameFactory,
                        composerFactory,
                        myMusicsFactory,
                        settingsFactory,
                        deletedMusics,
                        locksmithFactory,
                        dailyFactory,
                        { date -> copiedDailyScores += date },
                        backStack = rememberNavBackStack(*restoredStack.toTypedArray()),
                    )
                }
            }
        }
        // A restored stack shows its top screen, the home once its view model read the settings, unless told another.
        if (restoredStack != null) {
            advanceUntilShown(restoredShows ?: string(HomeR.string.feature_home_play))
            return preferences
        }
        advanceUntilShown(string(CartridgesR.string.feature_cartridges_title))
        if (openClassic) tapClassicCartridge()
        return preferences
    }

    /** The gauge of the start level of the home screen, read « Niveau de départ ». */
    private fun AndroidComposeUiTest<ComponentActivity>.startLevelGauge() =
        hasContentDescription(string(HomeR.string.feature_home_start_level))

    /** Moves the gauge of the start level of the home screen to [level], as TalkBack does. */
    private fun AndroidComposeUiTest<ComponentActivity>.chooseStartLevel(level: Int) {
        onNode(startLevelGauge()).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) {
            it(level.toFloat())
        }
        settle()
    }

    /** Taps the Classic cartridge on the choice of the game, and waits for its home. */
    private fun AndroidComposeUiTest<ComponentActivity>.tapClassicCartridge() {
        onNode(hasTestTag(CLASSIC_CARTRIDGE_TAG)).performClick()
        // The home shows once its view model read the settings, which may answer after a first advance.
        advanceUntilShown(string(HomeR.string.feature_home_play))
    }

    /** Taps the cartridge of the Locksmith on the choice of the game, and waits for its padlock 1. */
    private fun AndroidComposeUiTest<ComponentActivity>.tapLocksmithCartridge() {
        onNode(hasTestTag(LOCKSMITH_CARTRIDGE_TAG)).performClick()
        advanceUntilShown(padlockTitle(1))
    }

    /** Taps the disc « Changer de jeu » of the home screen, and waits for the choice of the game. */
    private fun AndroidComposeUiTest<ComponentActivity>.tapChangeGame() {
        onNodeWithContentDescription(string(HomeR.string.feature_home_change_game)).performScrollTo().performClick()
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

    /**
     * The system back leaves the application: no screen of the navigation takes it, and it reaches the activity, as the
     * last callback of its dispatcher, the one [showApp] registered before any screen. The activity would then leave on
     * its own, by a request to the system that answers later (finish, or move a root task to the back): what it does
     * is the system's, not the application's, and is not awaited here.
     */
    private fun AndroidComposeUiTest<ComponentActivity>.assertTheSystemBackLeavesTheApplication() {
        val before = backsToTheSystem
        pressSystemBack()
        assertThat(backsToTheSystem).isEqualTo(before + 1)
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

    /** The title of the bar of padlock [level], as the screen shows it: in capitals. */
    private fun AndroidComposeUiTest<ComponentActivity>.padlockTitle(level: Int): String =
        string(LocksmithR.string.feature_locksmith_title, level).uppercase()

    /** The title of the entry of the daily seed on [DAILY_DATE]. */
    private fun AndroidComposeUiTest<ComponentActivity>.dailyTitle(): String =
        string(DailyR.string.feature_daily_title, "26 octobre")

    private fun AndroidComposeUiTest<ComponentActivity>.string(
        @StringRes id: Int,
        vararg arguments: Any,
    ): String = checkNotNull(activity).getString(id, *arguments)

    private companion object {
        /** The longest a journey may take on a busy emulator. */
        val JOURNEY_TIMEOUT = 3.minutes

        /** A change of screen and its transition, well within: a second of the clock, sixty ticks of a game. */
        const val SETTLE_MILLIS = 1_000L

        /** A music of the player, chosen for the games. */
        const val GAME_MUSIC = "Thème du soir"

        /** The tag of the Classic cartridge (`Cartridge.tag` of feature:cartridges, internal to it). */
        const val CLASSIC_CARTRIDGE_TAG = "cartridge_classic"
        const val LOCKSMITH_CARTRIDGE_TAG = "cartridge_locksmith"

        /** The day of the entry of the daily seed, in the journeys. */
        val DAILY_DATE: LocalDate = LocalDate.of(2026, 10, 26)

        /** Between two searches of the game over screen. */
        const val CHECK_MILLIS = 500L

        /** A minute of play: far more than a game whose rows are ticks lasts. */
        const val MAX_PLAY_MILLIS = 60_000L
    }
}
