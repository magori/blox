package ch.magori.blox.feature.daily

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.designsystem.text.textLayout
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.PieceType
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class DailyScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val beforeAnyTry = DailyUiState(
        date = LocalDate.of(2026, 10, 26),
        firstPieces = "ZTIZOZJ".map { PieceType.valueOf("$it") },
        startLevel = 5,
        tries = 0,
        firstTry = null,
        best = null,
    )

    @Test
    @DisplayName("the title gives the day in letters")
    fun theTitleGivesTheDayInLetters() = extension.use {
        // Given / When
        showDaily(beforeAnyTry)

        // Then
        onNodeWithText(string(R.string.feature_daily_title, "26 octobre")).assertIsDisplayed()
    }

    @Test
    @DisplayName("TalkBack reads each of the seven first pieces with its rank, and none can be touched")
    fun talkBackReadsEachOfTheSevenFirstPieces() = extension.use {
        // Given
        showDaily(beforeAnyTry)

        // Then
        "ZTIZOZJ".forEachIndexed { index, letter ->
            onNodeWithContentDescription(string(R.string.feature_daily_piece, index + 1, "$letter"))
                .assertExists()
                .assertHasNoClickAction()
        }
    }

    @Test
    @DisplayName("before the first try, the tiles and the best of the day show a dash, and copying is disabled")
    fun beforeTheFirstTryCopyingIsDisabled() = extension.use {
        // Given / When
        showDaily(beforeAnyTry)

        // Then
        onNodeWithContentDescription(string(R.string.feature_daily_best_of_the_day_none)).assertExists()
        onNodeWithContentDescription(
            string(R.string.feature_daily_tile_description_none, string(R.string.feature_daily_best)),
        ).assertExists()
        onNode(hasTestTag(DAILY_COPY_TAG)).performScrollTo().assertIsNotEnabled()
        onNodeWithText(string(R.string.feature_daily_copy_score_later)).assertExists()
    }

    @Test
    @DisplayName("after a try, the tiles read the results and Copier mon score hands the copy to app")
    fun afterATryCopyingIsHandedToApp() = extension.use {
        // Given
        val copies = mutableListOf<Unit>()
        showDaily(
            beforeAnyTry.copy(tries = 5, firstTry = 9_800, best = 18_200),
            DailyActions(onChangeGame = {}, onSettings = {}, onPlay = { _ -> }, onCopyScore = { copies += Unit }),
        )
        onNodeWithContentDescription(
            string(R.string.feature_daily_tile_description, string(R.string.feature_daily_tries), 5),
        ).assertExists()
        onNodeWithContentDescription(string(R.string.feature_daily_best_of_the_day_description, 18_200)).assertExists()
        onNodeWithText(string(R.string.feature_daily_copy_score_later)).assertDoesNotExist()

        // When
        onNode(hasTestTag(DAILY_COPY_TAG)).performScrollTo().assertIsEnabled().performClick()

        // Then
        runOnIdle { assertThat(copies).hasSize(1) }
    }

    @Test
    @DisplayName("on 360 dp, in a normal font, the reminder under Jouer holds whole on one line, without an ellipsis")
    fun onANarrowPhoneTheReminderUnderJouerHoldsWhole() = extension.use {
        // Given the entry on a phone of 360 dp
        showDaily(beforeAnyTry, width = NARROW_PHONE)

        // Then
        val layout = textLayout(string(R.string.feature_daily_play_reminder, 5))
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isLineEllipsized(0)).isFalse()
    }

    @Test
    @DisplayName("Jouer, read with the level received and its reminder in words, hands the date shown to app")
    fun playHandsTheDateShownToApp() = extension.use {
        // Given the entry of the 26th of October
        val played = mutableListOf<LocalDate>()
        showDaily(
            beforeAnyTry,
            DailyActions(onChangeGame = {}, onSettings = {}, onPlay = { played += it }, onCopyScore = {}),
        )

        // Then the reminder gives the level of the daily game, on two digits, read in words
        onNodeWithText(string(R.string.feature_daily_play_reminder, 5), useUnmergedTree = true).assertExists()
        assertThat(string(R.string.feature_daily_play_reminder, 5)).isEqualTo("Niveau 05 · Vivante")
        assertThat(string(R.string.feature_daily_play_description, 5)).isEqualTo("Jouer, niveau 5, Vivante")

        // When
        onNodeWithContentDescription(string(R.string.feature_daily_play_description, 5))
            .performScrollTo()
            .performClick()

        // Then the game of the pieces shown, even if the phone passed midnight since
        runOnIdle { assertThat(played).containsExactly(LocalDate.of(2026, 10, 26)) }
    }

    @Test
    @DisplayName("Jouer, Changer de jeu and Paramètres count only while the entry is resumed")
    fun tapsCountOnlyWhileTheEntryIsResumed() = extension.use {
        // Given the entry no longer resumed, as once a first tap opened the game (its transition under way)
        val touched = mutableListOf<String>()
        val owner = PausedLifecycleOwner()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    DailyScreen(
                        uiState = beforeAnyTry,
                        actions = DailyActions(
                            onChangeGame = { touched += "change game" },
                            onSettings = { touched += "settings" },
                            onPlay = { touched += "play" },
                            onCopyScore = {},
                        ),
                    )
                }
            }
        }

        // When
        onNodeWithContentDescription(string(R.string.feature_daily_play_description, 5))
            .performScrollTo()
            .performClick()
        onNodeWithContentDescription(string(R.string.feature_daily_change_game)).performScrollTo().performClick()
        onNodeWithContentDescription(string(R.string.feature_daily_settings)).performScrollTo().performClick()

        // Then nothing is handed to app
        runOnIdle { assertThat(touched).isEmpty() }
    }

    /** A screen started but not resumed: paused, as during the transition to the next one. */
    private class PausedLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.STARTED }
    }

    @Test
    @DisplayName("the bar changes the game and opens the settings through app")
    fun theBarChangesTheGameAndOpensTheSettings() = extension.use {
        // Given
        val touched = mutableListOf<String>()
        showDaily(
            beforeAnyTry,
            DailyActions(
                onChangeGame = { touched += "change game" },
                onSettings = { touched += "settings" },
                onPlay = { _ -> },
                onCopyScore = {},
            ),
        )

        // When
        onNodeWithContentDescription(string(R.string.feature_daily_change_game)).performClick()
        onNodeWithContentDescription(string(R.string.feature_daily_settings)).performClick()

        // Then
        runOnIdle { assertThat(touched).containsExactly("change game", "settings") }
    }

    private fun ComposeContext.showDaily(
        uiState: DailyUiState,
        actions: DailyActions = DailyActions.None,
        width: Dp = Dp.Unspecified,
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                DailyScreen(uiState = uiState, actions = actions, modifier = Modifier.width(width))
            }
        }
    }

    private fun string(
        @StringRes id: Int,
        vararg arguments: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *arguments)
}

/** A phone of 360 dp wide, as on the mockup: the narrowest of those BloX is drawn for. */
private val NARROW_PHONE = 360.dp
