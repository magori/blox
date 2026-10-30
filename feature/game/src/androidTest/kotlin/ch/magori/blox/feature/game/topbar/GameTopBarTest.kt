package ch.magori.blox.feature.game.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.feature.game.GameLabels
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

private const val BAR = "top_bar"

/**
 * The top bar in the narrowest multi-window (P2.26), and on a phone: measured as each theme draws it, with the normal
 * font and the font twice as large, as the system scales it.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameTopBarTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    /** Whether the record shown is beaten, a new record: not unless a test says so. */
    private var beaten by mutableStateOf(false)

    /**
     * The game whose labels the bar shows: the Classic, in both themes, or the daily game (V39), its longer title of three
     * words and its sticker NOUVEAU MEILLEUR, in the Colorful theme only (ADR 0026). [title] is shown in capitals, [label]
     * and [sticker] as written, read by TalkBack before the value.
     */
    internal enum class Variant(val labels: GameLabels, val title: String, val label: String, val sticker: String) {
        CLASSIC(GameLabels.Classic, "MODE CLASSIQUE", "Record", "Nouveau record"),
        DAILY(GameLabels.Daily, "GRAINE DU JOUR", "Meilleur", "Nouveau meilleur"),
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window the title is shown whole, no word cut, in both themes, font enlarged")
    fun inTheNarrowestWindowTheTitleIsShownWhole(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 98_760)

        // Then
        val title = textLayout(hasText(variant.title))
        assertThat(title.hasVisualOverflow).isFalse()
        assertThat(wordsOnLines(title)).containsExactlyElementsOf(variant.title.split(' '))
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window the record is shown whole, in both themes, font enlarged")
    fun inTheNarrowestWindowTheRecordIsShownWhole(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 98_760)

        // Then its label and its value, as laid out, are not cut
        assertThat(textLayout(hasText(variant.label.uppercase())).hasVisualOverflow).isFalse()
        assertThat(textLayout(hasText("098760")).hasVisualOverflow).isFalse()
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window the record is no taller than the pause button: the board keeps its height")
    fun inTheNarrowestWindowTheRecordIsNoTallerThanThePauseButton(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 98_760)

        // Then
        val pause = onNodeWithContentDescription("Mettre en pause").fetchSemanticsNode()
        val record = onNodeWithContentDescription("${variant.label} : 98760").fetchSemanticsNode()
        assertThat(record.size.height).isLessThanOrEqualTo(pause.size.height)
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window the pause button keeps its 48 dp, in both themes, font enlarged")
    fun inTheNarrowestWindowThePauseButtonKeepsItsSize(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 98_760)

        // Then
        onNodeWithContentDescription("Mettre en pause")
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window TalkBack reads the real record, beyond the 999 999 shown")
    fun inTheNarrowestWindowTalkBackReadsTheRealRecord(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 1_234_567)

        // Then TalkBack reads the real value, and not the texts shown, capped: they are not in the tree it reads
        onNodeWithContentDescription("${variant.label} : 1234567").assertExists()
        listOf(variant.label.uppercase(), "999999").forEach { shown ->
            onNode(hasText(shown)).assertDoesNotExist()
        }
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("on a phone of 360 dp the bar stays on one row, the title right of the pause button, font enlarged")
    fun onAPhoneTheBarStaysOnOneRow(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        showTopBar(variant, kind, fontScale, record = 98_760, width = PHONE)

        // Then the title is right of the pause button, beside it, not above
        assertTitleBesidePauseButton(variant)
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("in the narrowest window the new record is shown whole, in both themes, font enlarged")
    fun inTheNarrowestWindowTheNewRecordIsShownWhole(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given a record beaten, the score of 98 790 the new record
        beaten = true
        showTopBar(variant, kind, fontScale, record = 98_790)

        // Then the sticker and the score, as laid out, are not cut
        assertThat(textLayout(hasText(variant.sticker.uppercase())).hasVisualOverflow).isFalse()
        assertThat(textLayout(hasText("098790")).hasVisualOverflow).isFalse()
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}, {3} dp")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1, 172",
        "CLASSIC, ORIGINAL, 2, 172",
        "CLASSIC, COLORFUL, 1, 172",
        "CLASSIC, COLORFUL, 2, 172",
        "DAILY, COLORFUL, 1, 172",
        "DAILY, COLORFUL, 2, 172",
        "CLASSIC, ORIGINAL, 1, 312",
        "CLASSIC, ORIGINAL, 2, 312",
        "CLASSIC, COLORFUL, 1, 312",
        "CLASSIC, COLORFUL, 2, 312",
        "DAILY, COLORFUL, 1, 312",
        "DAILY, COLORFUL, 2, 312",
    )
    @DisplayName("once the record is beaten, the bar keeps its height and the record its place: nothing moves")
    fun onceTheRecordIsBeatenNothingMoves(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
        widthDp: Int,
    ) = extension.use {
        // Given the record of 98 760 shown
        showTopBar(variant, kind, fontScale, record = 98_760, width = widthDp.dp)
        val bar = onNodeWithTag(BAR).fetchSemanticsNode().size
        val record = onNodeWithContentDescription("${variant.label} : 98760").fetchSemanticsNode().boundsInRoot

        // When the score beats it
        runOnIdle { beaten = true }

        // Then the new record takes the place of the record, the bar as high as before
        val newBest =
            onNodeWithContentDescription("${variant.sticker} : 98760").fetchSemanticsNode().boundsInRoot
        assertThat(onNodeWithTag(BAR).fetchSemanticsNode().size).isEqualTo(bar)
        assertThat(newBest).isEqualTo(record)
    }

    @ParameterizedTest(name = "{0}, {1}, font × {2}")
    @CsvSource(
        "CLASSIC, ORIGINAL, 1",
        "CLASSIC, ORIGINAL, 2",
        "CLASSIC, COLORFUL, 1",
        "CLASSIC, COLORFUL, 2",
        "DAILY, COLORFUL, 1",
        "DAILY, COLORFUL, 2",
    )
    @DisplayName("on a phone of 360 dp the bar stays on one row once the record is beaten, font enlarged")
    fun onAPhoneTheBarStaysOnOneRowOnceTheRecordIsBeaten(
        variant: Variant,
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        // Given
        beaten = true
        showTopBar(variant, kind, fontScale, record = 98_790, width = PHONE)

        // Then the title is right of the pause button, beside it, not above
        assertTitleBesidePauseButton(variant)
    }

    /** The title of [variant] is right of the pause button, beside it, not above: the bar is on one row. */
    private fun ComposeContext.assertTitleBesidePauseButton(variant: Variant) {
        val pause = onNodeWithContentDescription("Mettre en pause").fetchSemanticsNode().boundsInRoot
        val title = onNode(hasText(variant.title), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(title.left).isGreaterThanOrEqualTo(pause.right)
        assertThat(title.center.y).isBetween(pause.top, pause.bottom)
    }

    /**
     * The top bar of [variant] for [record], a new record if [beaten], [width] wide, in [kind], with the system font
     * scaled by [fontScale].
     */
    private fun ComposeContext.showTopBar(
        variant: Variant,
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
                        GameTopBar(
                            record = ShownRecord(record, beaten),
                            onPause = {},
                            labels = variant.labels,
                            modifier = Modifier.requiredWidth(width).testTag(BAR),
                        )
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
