package ch.magori.blox.feature.game.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** The width of the top bar in the narrowest multi-window, 220 dp, without the margins of 24 dp of the screen. */
private val NARROWEST = (220 - 2 * 24).dp

/** The width of the top bar on a phone of 360 dp, without the margins. */
private val PHONE = (360 - 2 * 24).dp

/**
 * The top bar in the narrowest multi-window (P2.26), and on a phone: measured as each theme draws it, with the normal
 * font and the font twice as large, as the system scales it.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameTopBarTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("in the narrowest window the title is shown whole, no word cut, in both themes, font enlarged")
    fun inTheNarrowestWindowTheTitleIsShownWhole(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 98_760)

        // Then
        val title = textLayout(hasText("MODE CLASSIQUE"))
        assertThat(title.hasVisualOverflow).isFalse()
        assertThat(wordsOnLines(title)).containsExactly("MODE", "CLASSIQUE")
    }

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("in the narrowest window the record is shown whole, in both themes, font enlarged")
    fun inTheNarrowestWindowTheRecordIsShownWhole(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 98_760)

        // Then its label and its value, as laid out, are not cut
        assertThat(textLayout(hasText("RECORD")).hasVisualOverflow).isFalse()
        assertThat(textLayout(hasText("098760")).hasVisualOverflow).isFalse()
    }

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("in the narrowest window the record is no taller than the pause button: the board keeps its height")
    fun inTheNarrowestWindowTheRecordIsNoTallerThanThePauseButton(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 98_760)

        // Then
        val pause = onNodeWithContentDescription("Mettre en pause").fetchSemanticsNode()
        val record = onNodeWithContentDescription("Record : 98760").fetchSemanticsNode()
        assertThat(record.size.height).isLessThanOrEqualTo(pause.size.height)
    }

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("in the narrowest window the pause button keeps its 48 dp, in both themes, font enlarged")
    fun inTheNarrowestWindowThePauseButtonKeepsItsSize(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 98_760)

        // Then
        onNodeWithContentDescription("Mettre en pause")
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("in the narrowest window TalkBack reads the real record, beyond the 999 999 shown")
    fun inTheNarrowestWindowTalkBackReadsTheRealRecord(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 1_234_567)

        // Then TalkBack reads the real value, and not the texts shown, capped: they are not in the tree it reads
        onNodeWithContentDescription("Record : 1234567").assertExists()
        listOf("RECORD", "999999").forEach { shown ->
            onNode(hasText(shown)).assertDoesNotExist()
        }
    }

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("on a phone of 360 dp the bar stays on one row, the title right of the pause button, font enlarged")
    fun onAPhoneTheBarStaysOnOneRow(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(kind, fontScale, record = 98_760, width = PHONE)

        // Then the title is right of the pause button, beside it, not above
        val pause = onNodeWithContentDescription("Mettre en pause").fetchSemanticsNode().boundsInRoot
        val title = onNode(hasText("MODE CLASSIQUE"), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(title.left).isGreaterThanOrEqualTo(pause.right)
        assertThat(title.center.y).isBetween(pause.top, pause.bottom)
    }

    /** The top bar for [record], [width] wide, in [kind], with the system font scaled by [fontScale]. */
    private fun ComposeContext.showTopBar(
        kind: BloxThemeKind,
        fontScale: Float,
        record: Int,
        width: Dp = NARROWEST,
    ) {
        setContent {
            val density = Density(LocalDensity.current.density, fontScale)
            CompositionLocalProvider(LocalDensity provides density) {
                BloxTheme(kind = kind) {
                    Box {
                        GameTopBar(record = record, onPause = {}, modifier = Modifier.requiredWidth(width))
                    }
                }
            }
        }
    }

    /** The text found by [matcher], as laid out. */
    private fun ComposeContext.textLayout(matcher: SemanticsMatcher): TextLayoutResult {
        val node = onNode(matcher, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    /** The words of [layout], line by line: a word cut from one line to the next shows as two. */
    private fun wordsOnLines(layout: TextLayoutResult): List<String> {
        val text = layout.layoutInput.text.text
        return List(layout.lineCount) { line ->
            text.substring(layout.getLineStart(line), layout.getLineEnd(line)).split(' ').filter { it.isNotEmpty() }
        }.flatten()
    }
}
