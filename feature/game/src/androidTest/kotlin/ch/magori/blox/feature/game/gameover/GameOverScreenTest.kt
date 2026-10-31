package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.GameLabels
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class GameOverScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName(
        "in Original, on a phone of 320 dp in a font twice as large, NOUVEAU RECORD shows whole, RECORD included",
    )
    fun theNewRecordBadgeShowsWholeOnANarrowPhoneInALargeFont() = extension.use {
        // Given a game over whose score beat the record, in Original, 320 dp wide, in a font twice as large
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    Box(Modifier.width(320.dp)) {
                        GameOverScreen(
                            state = NEW_RECORD,
                            onReplay = {},
                            onMenu = {},
                            labels = GameLabels.Classic,
                            onCopy = {},
                        )
                    }
                }
            }
        }

        // Then the badge lays out all of its text: RECORD too, on a line of its own if need be
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("NOUVEAU RECORD").fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertThat(layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)).isEqualTo("NOUVEAU RECORD".length)
        assertThat(layout.hasVisualOverflow).isFalse()
    }

    @Test
    @DisplayName(
        "in Couleurs, on a phone of 360 dp in a font twice as large, the best before the game shows whole, centred",
    )
    fun theBestBeforeTheGameShowsWholeCentredInALargeFont() = extension.use {
        // Given the card of a daily game over whose score beat a best of 98 760, 360 dp wide, in a font twice as large
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                BloxTheme(kind = BloxThemeKind.COLORFUL) {
                    Box(Modifier.width(360.dp)) {
                        GameOverScreen(
                            state = NEW_RECORD.copy(previousBest = 98_760),
                            onReplay = {},
                            onMenu = {},
                            labels = GameLabels.Daily,
                            onCopy = {},
                        )
                    }
                }
            }
        }

        // Then its label and its value are laid out whole, without overflow, each line centred under the score
        val text = "Ancien meilleur 098760"
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertThat(layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)).isEqualTo(text.length)
        assertThat(layout.hasVisualOverflow).isFalse()
        val width = layout.size.width.toFloat()
        (0 until layout.lineCount).forEach { line ->
            assertThat(layout.getLineLeft(line)).isCloseTo(width - layout.getLineRight(line), within(1f))
        }
    }

    @Test
    @DisplayName("a new record shows the old record as « Ancien record »")
    fun aNewRecordShowsTheOldRecordAsAncienRecord() = extension.use {
        // Given the card of a game over whose score beat a record of 98 760
        showCard(NEW_RECORD.copy(previousBest = 98_760))

        // Then the card shows NOUVEAU RECORD, and the record it beat under the score
        onNodeWithText("NOUVEAU RECORD").assertIsDisplayed()
        onNodeWithText("Ancien record 098760", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @DisplayName("a first positive score is shown as a new record, without an old record")
    fun aFirstPositiveScoreIsShownAsANewRecordWithoutAnOldRecord() = extension.use {
        // Given the card of the game over of a first game, its score above 0: there was no record (0)
        showCard(NEW_RECORD.copy(previousBest = 0))

        // Then the card shows NOUVEAU RECORD, and no record under the score
        onNodeWithText("NOUVEAU RECORD").assertIsDisplayed()
        onNodeWithText("Ancien", substring = true, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("000000", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @DisplayName("without a new record the record is shown as « Record »")
    fun withoutANewRecordTheRecordIsShownAsRecord() = extension.use {
        // Given the card of a game over below a record of 150 000
        showCard(NEW_RECORD.copy(newBest = false, previousBest = 150_000))

        // Then no badge, and the record under the score
        onNodeWithText("NOUVEAU RECORD").assertDoesNotExist()
        onNodeWithText("Record 150000", useUnmergedTree = true).assertIsDisplayed()
    }

    /** The card of the game over of [state], in Couleurs, its score whole. */
    private fun ComposeContext.showCard(state: GameOverUiState) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                GameOverScreen(state = state, onReplay = {}, onMenu = {}, labels = GameLabels.Classic, onCopy = {})
            }
        }
    }

    private companion object {
        val NEW_RECORD = GameOverUiState(
            score = Points(123_450),
            lines = Lines(87),
            level = Level(9),
            fourLineClears = 3,
            ticksPlayed = 60 * 65,
            newBest = true,
            resultsKept = true,
            previousBest = 0,
            practice = false,
        )
    }
}
