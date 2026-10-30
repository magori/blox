package ch.magori.blox.feature.game.gameover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
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
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
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
