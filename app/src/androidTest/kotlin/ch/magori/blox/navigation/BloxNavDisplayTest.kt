package ch.magori.blox.navigation

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeMusicFileRepository
import ch.magori.blox.core.data.FakeMusicLibrary
import ch.magori.blox.core.data.FakePadlockProgressRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.component.tapScrim
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.engine.OneRowPerTickGameEngineFactory
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Scale
import ch.magori.blox.core.music.ScaleType
import ch.magori.blox.core.music.Score
import ch.magori.blox.core.music.Scores
import ch.magori.blox.daily.DailySeed
import ch.magori.blox.daily.NewCartridges
import ch.magori.blox.feature.cartridges.navigation.CartridgesRoute
import ch.magori.blox.feature.composer.ComposerViewModel
import ch.magori.blox.feature.daily.DailyViewModel
import ch.magori.blox.feature.daily.navigation.DailyRoute
import ch.magori.blox.feature.game.GameViewModel
import ch.magori.blox.feature.game.navigation.GameRoute
import ch.magori.blox.feature.garden.GardenViewModel
import ch.magori.blox.feature.garden.navigation.GardenRoute
import ch.magori.blox.feature.home.HomeViewModel
import ch.magori.blox.feature.home.navigation.HomeRoute
import ch.magori.blox.feature.locksmith.ClearedProgressRelay
import ch.magori.blox.feature.locksmith.LocksmithViewModel
import ch.magori.blox.feature.locksmith.PadlocksViewModel
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import ch.magori.blox.feature.locksmith.navigation.PadlocksRoute
import ch.magori.blox.feature.musics.DeletedMusicRelay
import ch.magori.blox.feature.musics.MyMusicsViewModel
import ch.magori.blox.feature.settings.SettingsViewModel
import ch.magori.blox.locksmith.PadlockFrontier
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
import ch.magori.blox.feature.garden.R as GardenR
import ch.magori.blox.feature.home.R as HomeR
import ch.magori.blox.feature.locksmith.R as LocksmithR
import ch.magori.blox.feature.settings.R as SettingsR

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
    @DisplayName("with no padlock open, the Locksmith cartridge names padlock 1 and opens it")
    fun theLocksmithCartridgeOpensPadlock1() {
        runJourney {
            // Given the choice of the game, on a new device
            showApp(openClassic = false)
            onNodeWithText(string(CartridgesR.string.feature_cartridges_padlock, 1)).assertIsDisplayed()

            // When
            tapLocksmithCartridge()

            // Then
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("the Locksmith cartridge names the frontier and opens it: padlock 3, once padlocks 1 and 2 are open")
    fun theLocksmithCartridgeOpensTheFrontier() {
        runJourney {
            // Given the choice of the game, padlocks 1 and 2 open
            showApp(openClassic = false, openPadlocks = 2)
            onNodeWithText(string(CartridgesR.string.feature_cartridges_padlock, 3)).assertIsDisplayed()

            // When
            tapLocksmithCartridge(frontier = 3)

            // Then
            onNodeWithText(padlockTitle(3)).assertIsDisplayed()
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
            // Given padlock 2 on top, as the system restores the stack after the death of the process
            showApp(
                restoredStack = listOf(CartridgesRoute, LocksmithRoute(level = 2)),
                restoredShows = padlockTitle(2),
            )

            // Then
            onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 0)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the screen of the padlocks over a padlock shows the progress, and back goes back to the padlock")
    fun theScreenOfThePadlocksShowsTheProgressAndBackGoesBack() {
        runJourney {
            // Given the screen of the padlocks over padlock 2, nothing open yet
            showApp(
                restoredStack = listOf(CartridgesRoute, LocksmithRoute(level = 2), PadlocksRoute(current = 2)),
                restoredShows = string(LocksmithR.string.feature_locksmith_padlocks_title).uppercase(),
            )
            onNodeWithText(string(LocksmithR.string.feature_locksmith_padlocks_empty)).assertIsDisplayed()

            // When
            pressSystemBack()

            // Then
            onNodeWithText(padlockTitle(2)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("the disc of a padlock opens the screen of the padlocks over it, and back finds its piece placed")
    fun theDiscOfAPadlockOpensThePadlocksAndBackKeepsThePadlock() {
        runJourney {
            // Given padlock 1, opened from the cartridges, a piece placed
            showApp(openClassic = false)
            tapLocksmithCartridge()
            placeAPiece(level = 1)

            // When the disc of the bar opens the padlocks, then back
            tapPadlocksDisc()
            onNodeWithText(padlockTitle(1)).assertDoesNotExist()
            pressSystemBack()

            // Then the same padlock, its piece placed
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 1)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("a padlock chosen on the screen of the padlocks takes the place of the padlock, back then leaves it")
    fun aPadlockChosenOnThePadlocksTakesThePlaceOfThePadlock() {
        runJourney {
            // Given padlock 3, the frontier, padlocks 1 and 2 open, and the screen of the padlocks over it
            showApp(openClassic = false, openPadlocks = 2)
            tapLocksmithCartridge(frontier = 3)
            tapPadlocksDisc()

            // When padlock 1 is chosen there
            onNode(hasTestTag(padlockTileTag(1))).performScrollTo().performClick()
            settle()

            // Then padlock 1, and back goes to the cartridges: padlock 3 is no longer under it
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            pressSystemBack()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("choosing on the screen of the padlocks the padlock one comes from goes back to it, its piece placed")
    fun choosingThePadlockOneComesFromGoesBackToIt() {
        runJourney {
            // Given padlock 1, a piece placed, and the screen of the padlocks over it
            showApp(openClassic = false)
            tapLocksmithCartridge()
            placeAPiece(level = 1)
            tapPadlocksDisc()

            // When padlock 1 is chosen there
            onNode(hasTestTag(padlockTileTag(1))).performScrollTo().performClick()
            settle()

            // Then the same padlock, its piece placed: the game is not started over
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 1)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName(
        "starting over on the screen of the padlocks opened over padlock 3 puts padlock 1 in its place, " +
            "which tells it, over the cartridges, which name padlock 1",
    )
    fun startingOverFromPadlock3GoesBackToPadlock1() {
        runJourney {
            // Given padlock 3, the frontier, padlocks 1 and 2 open, and the screen of the padlocks over it
            showApp(openClassic = false, openPadlocks = 2)
            tapLocksmithCartridge(frontier = 3)
            tapPadlocksDisc()

            // When the player starts over from the beginning, and confirms
            startOver()

            // Then padlock 1, whose message tells the progress cleared, to TalkBack as it comes
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(LocksmithR.string.feature_locksmith_message_cleared))
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            // And back goes to the cartridges, which name padlock 1 again
            pressSystemBack()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_padlock, 1)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName(
        "starting over on the screen of the padlocks opened over padlock 1 starts padlock 1 over, nothing placed, " +
            "and tells it",
    )
    fun startingOverFromPadlock1StartsItOver() {
        runJourney {
            // Given padlock 1 open, then chosen again on the screen of the padlocks, a piece placed, and the screen of
            // the padlocks over it
            showApp(openClassic = false, openPadlocks = 1)
            tapLocksmithCartridge(frontier = 2)
            tapPadlocksDisc()
            onNode(hasTestTag(padlockTileTag(1))).performScrollTo().performClick()
            settle()
            placeAPiece(level = 1)
            tapPadlocksDisc()

            // When the player starts over from the beginning, and confirms
            startOver()

            // Then padlock 1 again, started over: nothing placed, and its message tells the progress cleared
            onNodeWithText(padlockTitle(1)).assertIsDisplayed()
            onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 1)).assertDoesNotExist()
            onNodeWithText(string(LocksmithR.string.feature_locksmith_message_cleared)).assertIsDisplayed()
        }
    }

    /** On the screen of the padlocks: « Recommencer depuis le début », then « Effacer » in the window. */
    private fun AndroidComposeUiTest<ComponentActivity>.startOver() {
        onNodeWithText(string(LocksmithR.string.feature_locksmith_padlocks_start_over)).performScrollTo().performClick()
        settle()
        onNodeWithText(string(LocksmithR.string.feature_locksmith_clear_confirm)).performClick()
        settle()
    }

    @Test
    @DisplayName("the Graine du jour cartridge, new before the first try of the day, opens its entry")
    fun theGraineDuJourCartridgeNewOpensItsEntry() {
        runJourney {
            // Given the choice of the game, no try of the Graine du jour kept
            showApp(openClassic = false)
            settle()
            onNode(hasTestTag(DAILY_CARTRIDGE_TAG))
                .assert(hasStateDescription(string(CartridgesR.string.feature_cartridges_new)))

            // When
            onNode(hasTestTag(DAILY_CARTRIDGE_TAG)).performScrollTo().performClick()
            advanceUntilShown(dailyTitle())

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("back on the cartridges, the Graine du jour bears its sticker from the first frame")
    fun backOnTheCartridgesTheStickerShowsFromTheFirstFrame() {
        runJourney {
            // Given the home of the Classic, opened from the cartridges, no try of the Graine du jour kept
            showApp()

            // When the system back brings the cartridges back, up to the first frame that shows them
            val activity = checkNotNull(activity)
            runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
            repeat(FIRST_FRAMES) {
                if (onAllNodes(hasTestTag(DAILY_CARTRIDGE_TAG)).fetchSemanticsNodes().isEmpty()) {
                    mainClock.advanceTimeByFrame()
                }
            }

            // Then the sticker is already there: it does not blink in
            onNode(hasTestTag(DAILY_CARTRIDGE_TAG))
                .assert(hasStateDescription(string(CartridgesR.string.feature_cartridges_new)))
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
    @DisplayName(
        "the Jardin musical cartridge opens the musical garden, and the system back goes back to the cartridges",
    )
    fun theJardinMusicalCartridgeOpensTheMusicalGarden() {
        runJourney {
            // Given the choice of the game
            showApp(openClassic = false)

            // When: the third cartridge, below the edge on a small screen
            onNode(hasTestTag(GARDEN_CARTRIDGE_TAG)).performScrollTo().performClick()
            advanceUntilShown(gardenTitle())

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertDoesNotExist()
            pressSystemBack()
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
        }
    }

    @Test
    @DisplayName("changing the game from the musical garden goes back to the cartridges")
    fun changingTheGameFromTheMusicalGardenGoesBackToTheCartridges() {
        runJourney {
            // Given the garden on top of the cartridges, as the system restores the stack after the death of the process
            showApp(restoredStack = listOf(CartridgesRoute, GardenRoute), restoredShows = gardenTitle())

            // When
            onNodeWithContentDescription(string(GardenR.string.feature_garden_change_game)).performClick()
            settle()

            // Then
            onNodeWithText(string(CartridgesR.string.feature_cartridges_title)).assertIsDisplayed()
            onNodeWithText(gardenTitle()).assertDoesNotExist()
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
            onNodeWithContentDescription(string(DesignR.string.core_designsystem_settings)).performClick()
            settle()
            onNodeWithText(string(SettingsR.string.feature_settings_vibrations)).performClick()
            settle()

            // Then they are saved, for the next game
            assertThat(preferences.state.value?.vibrations).isTrue()

            // When
            onNodeWithContentDescription(string(SettingsR.string.feature_settings_back)).performClick()
            settle()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(SettingsR.string.feature_settings_vibrations)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("on the settings, the system back goes back to the home screen")
    fun onTheSettingsTheSystemBackGoesHome() {
        runJourney {
            // Given the settings, opened from the home screen
            showApp()
            onNodeWithContentDescription(string(DesignR.string.core_designsystem_settings)).performClick()
            settle()

            // When
            pressSystemBack()

            // Then
            onNodeWithText(string(HomeR.string.feature_home_play)).assertExists()
            onNodeWithText(string(SettingsR.string.feature_settings_vibrations)).assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("while the saved settings are not read, the settings show their back arrow and title, and no switch")
    fun whileTheSettingsAreNotReadTheyShowTheirHeaderAndNoSwitch() {
        runJourney {
            // Given the home screen, and settings of the page that have not answered yet
            showApp(settingsPreferences = FakeUserPreferencesRepository(initial = null))

            // When
            onNodeWithContentDescription(string(DesignR.string.core_designsystem_settings)).performClick()
            settle()

            // Then
            onNodeWithContentDescription(string(SettingsR.string.feature_settings_back)).assertExists()
            onNodeWithText(string(SettingsR.string.feature_settings_title)).assertExists()
            onNodeWithText(string(SettingsR.string.feature_settings_vibrations)).assertDoesNotExist()
            onNodeWithText(string(SettingsR.string.feature_settings_sound_effects)).assertDoesNotExist()
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
            onNodeWithContentDescription(string(DesignR.string.core_designsystem_settings))
                .performScrollTo()
                .performClick()
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
            onNodeWithContentDescription(string(DesignR.string.core_designsystem_settings))
                .performScrollTo()
                .performClick()
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

    /**
     * Chooses the minuet on the home screen, in the sheet Musique, which a touch on its scrim closes, then listens to it on
     * the row Musique of the card.
     */
    private fun AndroidComposeUiTest<ComponentActivity>.listenToTheMinuet() {
        val minuet = string(HomeR.string.feature_home_music_minuet)
        val korobeiniki = string(HomeR.string.feature_home_music_korobeiniki)
        val openSheet = checkNotNull(activity).getString(HomeR.string.feature_home_music_open, korobeiniki)
        onNodeWithContentDescription(openSheet).performScrollTo().performClick()
        settle()
        onNode(hasText(minuet) and hasAnyAncestor(isDialog())).performClick()
        settle()
        onNode(isDialog()).tapScrim()
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
        openPadlocks: Int = 0,
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
        // Padlocks 1 to [openPadlocks] open, as a player who went that far: the frontier is the one after.
        val padlockProgress = FakePadlockProgressRepository()
        runBlocking { (1..openPadlocks).forEach { padlockProgress.record(it, PADLOCK_OPENING) } }
        val padlockFrontier = PadlockFrontier(padlockProgress.progress, MainScope())
        val clearedPadlocks = ClearedProgressRelay()
        val locksmithFactory =
            LocksmithViewModel.Factory(
                FakeSoundEffectPlayer(),
                preferences,
                padlockProgress,
                MainScope(),
                clearedPadlocks,
            )
        val padlocksFactory = PadlocksViewModel.Factory(padlockProgress, clearedPadlocks, MainScope())
        val newCartridges = NewCartridges(dailyChallenge.results, MainScope()) { DAILY_DATE }
        val gardenFactory =
            GardenViewModel.Factory(
                FakeGardenRepository(),
                FakeGardenLibrary(),
                MainScope(),
                music,
                FakeSoundEffectPlayer(),
                preferences,
            )
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
                        newCartridges,
                        gardenFactory,
                        padlockFrontier,
                        padlocksFactory,
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
                        newCartridges,
                        gardenFactory,
                        padlockFrontier,
                        padlocksFactory,
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

    /** Taps the cartridge of the Locksmith on the choice of the game, and waits for its [frontier], padlock 1 at first. */
    private fun AndroidComposeUiTest<ComponentActivity>.tapLocksmithCartridge(frontier: Int = 1) {
        onNode(hasTestTag(LOCKSMITH_CARTRIDGE_TAG)).performClick()
        advanceUntilShown(padlockTitle(frontier))
    }

    /** Taps the disc « Toutes les serrures » of the bar of a padlock, and waits for the screen of the padlocks. */
    private fun AndroidComposeUiTest<ComponentActivity>.tapPadlocksDisc() {
        onNodeWithContentDescription(string(LocksmithR.string.feature_locksmith_padlocks)).performClick()
        advanceUntilShown(string(LocksmithR.string.feature_locksmith_padlocks_title).uppercase())
    }

    /**
     * Places the first piece given by padlock [level] where its solution has it: turned as the solution has it, aimed
     * by its pivot, and placed. One move counted.
     */
    private fun AndroidComposeUiTest<ComponentActivity>.placeAPiece(level: Int) {
        val start = PadlockGame.start(Padlock.ofLevel(level))
        val piece = start.padlock.solution.first { it.type == start.given.first() }
        val turns = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }
            .indexOfFirst { it.orientationOf(0) == piece.orientation }
        repeat(turns) {
            onNodeWithContentDescription(string(LocksmithR.string.feature_locksmith_turn_right)).performClick()
        }
        val cell = string(LocksmithR.string.feature_locksmith_cell, piece.pivot.column, piece.pivot.row, "")
        onNode(hasContentDescription(cell, substring = true)).performScrollTo().performClick()
        onNodeWithText(string(LocksmithR.string.feature_locksmith_place)).performClick()
        settle()
        onNodeWithText(string(LocksmithR.string.feature_locksmith_moves, 1)).assertIsDisplayed()
    }

    /** The tag of the tile of padlock [level] on the screen of the padlocks (`padlockTileTag`, internal to its feature). */
    private fun padlockTileTag(level: Int): String = "padlocks_tile_$level"

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

    /** The title of the bar of the musical garden, as the screen shows it: in capitals. */
    private fun AndroidComposeUiTest<ComponentActivity>.gardenTitle(): String =
        string(GardenR.string.feature_garden_title).uppercase()

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
        const val GARDEN_CARTRIDGE_TAG = "cartridge_garden"
        const val DAILY_CARTRIDGE_TAG = "cartridge_daily"

        /** The most frames a screen brought back by the system back takes to show its first frame. */
        const val FIRST_FRAMES = 30

        /** The day of the entry of the daily seed, in the journeys. */
        val DAILY_DATE: LocalDate = LocalDate.of(2026, 10, 26)

        /** Between two searches of the game over screen. */
        const val CHECK_MILLIS = 500L

        /** A minute of play: far more than a game whose rows are ticks lasts. */
        const val MAX_PLAY_MILLIS = 60_000L

        /** An opening of a padlock, the same for each one the journeys hold open. */
        val PADLOCK_OPENING = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
    }
}
