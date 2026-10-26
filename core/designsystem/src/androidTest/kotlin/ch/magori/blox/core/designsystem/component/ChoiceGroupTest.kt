package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class ChoiceGroupTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val themes = listOf(Choice("Original"), Choice("Couleurs"), Choice("Brume"))
    private val modes = listOf(Choice("Classique"), Choice("Moderne", available = false))

    @Test
    @DisplayName("exactly one segment is selected, checked and in accent")
    fun exactlyOneSegmentIsSelectedCheckedAndInAccent() = extension.use {
        // Given
        showGroup(themes, selected = 1)

        // Then
        onNodeWithText("Couleurs").assertIsSelected()
        onNodeWithText("Original").assertIsNotSelected()
        onNodeWithText("Brume").assertIsNotSelected()
        onAllNodes(hasTestTag(CHOICE_CHECK_TAG), useUnmergedTree = true).assertCountEquals(1)
        onAllNodes(hasTestTag(CHOICE_CHECK_TAG) and hasParent(isSelected()), useUnmergedTree = true)
            .assertCountEquals(1)
        // And in accent: its colours are those of segmentLook (ChoiceLookTest), drawn by the screenshots
    }

    @Test
    @DisplayName("an unavailable segment reads « Moderne, bientôt, indisponible » and cannot be chosen")
    fun anUnavailableSegmentCannotBeChosen() = extension.use {
        // Given
        val chosen = mutableListOf<Int>()
        showGroup(listOf(Choice("Classique"), Choice("Moderne", available = false)), selected = 0, onChosen = {
            chosen +=
                it
        })

        // When
        val modern = onNodeWithContentDescription("Moderne, bientôt, indisponible")
        modern.performClick()

        // Then TalkBack reads it whole, once, and it cannot be chosen
        modern
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Moderne, bientôt, indisponible"),
                ),
            )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .assertIsNotEnabled()
            .assertIsNotSelected()
        assertThat(chosen).isEmpty()
        // And it shows BIENTÔT, within its segment, which TalkBack does not read twice
        val segment = modern.getUnclippedBoundsInRoot()
        val soon = onNode(hasTestTag(CHOICE_SOON_TAG), useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .getUnclippedBoundsInRoot()
        assertThat(soon.left).isGreaterThan(segment.left)
        assertThat(soon.right).isLessThan(segment.right)
    }

    @Test
    @DisplayName("a group too wide for its line puts its label above and takes the whole width")
    fun aGroupTooWideForItsLinePutsItsLabelAbove() = extension.use {
        // Given a line too narrow for the label and two themes side by side, wide enough for the themes alone
        showGroup(themes.take(2), selected = 1, width = 250.dp)

        // Then
        val label = onNodeWithText("Thème").getUnclippedBoundsInRoot()
        val group = groupBounds(last = 1)
        assertThat(group.top).isGreaterThanOrEqualTo(label.bottom)
        assertThat(group.left).isEqualTo(label.left)
        assertThat((group.right - group.left).value).isCloseTo(250f, within(PIXEL))
    }

    @Test
    @DisplayName("a group that fits on the line of its label stands beside it, at the end of the line")
    fun aGroupThatFitsStandsBesideItsLabel() = extension.use {
        // Given
        showGroup(themes.take(2), selected = 1, width = 360.dp)

        // Then the label at the start, the group at the end, on one line
        val label = onNodeWithText("Thème").getUnclippedBoundsInRoot()
        val group = groupBounds(last = 1)
        val line = onNodeWithTag(GROUP_TAG).getUnclippedBoundsInRoot()
        assertThat(label.left).isEqualTo(line.left)
        assertThat(group.right).isEqualTo(line.right)
        assertThat(group.left).isGreaterThan(label.right)
        assertThat(label.top).isGreaterThanOrEqualTo(group.top)
        assertThat(label.bottom).isLessThanOrEqualTo(group.bottom)
    }

    @Test
    @DisplayName("each segment is 48 dp high as a target")
    fun eachSegmentIs48DpHighAsATarget() = extension.use {
        // Given, in a normal font
        showGroup(themes, selected = 1)

        // Then each whole segment may be touched, frame of the group included: 48 dp, no more
        val segments = onAllNodes(hasClickAction())
        segments.assertCountEquals(themes.size)
        repeat(themes.size) { index ->
            segments[index].assertHeightIsEqualTo(
                BloxSizes.minTouchTarget,
            ).assertWidthIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    @Test
    @DisplayName("touching a segment chooses it, by its place in the group")
    fun touchingASegmentChoosesIt() = extension.use {
        // Given
        val chosen = mutableListOf<Int>()
        showGroup(themes, selected = 1, onChosen = { chosen += it })

        // When
        onNodeWithText("Brume").performClick()

        // Then
        assertThat(chosen).containsExactly(2)
    }

    @Test
    @DisplayName("TalkBack reads the label as a heading, then the segments as one group of radio buttons")
    fun talkBackReadsTheLabelThenOneGroupOfRadioButtons() = extension.use {
        // Given
        showGroup(themes, selected = 1)

        // Then
        onNodeWithText("Thème").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val group = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        val segments = onAllNodes(hasParent(group)).fetchSemanticsNodes()
        assertThat(segments.map { it.config[SemanticsProperties.Text].joinToString() })
            .containsExactly("Original", "Couleurs", "Brume")
        assertThat(segments.map { it.config[SemanticsProperties.Role] }).containsOnly(Role.RadioButton)
    }

    @Test
    @DisplayName(
        "a group too wide for the whole line, even with its labels shrunk, stands its choices one under the other",
    )
    fun aGroupTooWideForTheWholeLineStandsItsChoicesOneUnderTheOther() = extension.use {
        // Given a font twice as large, on 360 dp, in the pixel font of Original, where Classique and Moderne cannot
        // stand side by side, even shrunk by a quarter
        showGroup(modes, selected = 0, fontScale = 2f, kind = BloxThemeKind.ORIGINAL)

        // Then
        val classic = onNodeWithText("Classique").getUnclippedBoundsInRoot()
        val modern = onNodeWithContentDescription("Moderne, bientôt, indisponible").getUnclippedBoundsInRoot()
        assertThat(modern.top).isGreaterThanOrEqualTo(classic.bottom)
        // And Classique is shown whole, on one line
        val layout = textLayout("Classique")
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.getLineRight(0)).isLessThanOrEqualTo(layout.size.width.toFloat())
        // And Moderne keeps its room beside BIENTÔT, or above it
        val name = onNode(hasTestTag(CHOICE_LABEL_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val soon = onNode(hasTestTag(CHOICE_SOON_TAG), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(name.right - name.left).isGreaterThan(soon.right - soon.left)
        assertThat(soon.left >= name.right || soon.top >= name.bottom).isTrue()
    }

    /** The text [text], as laid out: the layout of its own node, not merged into its segment. */
    private fun ComposeContext.textLayout(text: String): TextLayoutResult {
        val node = onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single()
    }

    /** Where the segments stand, from the first to the one of index [last] among the themes. */
    private fun ComposeContext.groupBounds(last: Int): DpRect {
        val first = onNodeWithText("Original").getUnclippedBoundsInRoot()
        val end = onNodeWithText(themes[last].label).getUnclippedBoundsInRoot()
        return DpRect(first.left, first.top, end.right, end.bottom)
    }

    private fun ComposeContext.showGroup(
        choices: List<Choice>,
        selected: Int,
        width: Dp = 360.dp,
        fontScale: Float = 1f,
        onChosen: (Int) -> Unit = {},
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
    ) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = kind) {
                    ChoiceGroup(
                        label = "Thème",
                        choices = choices,
                        selected = selected,
                        onChosen = onChosen,
                        // Clear of the bars of the system, which a touch at the top of the screen would open.
                        modifier = Modifier.safeDrawingPadding().width(width).testTag(GROUP_TAG),
                    )
                }
            }
        }
    }
}

/** The tag of the group under test, its label included. */
private const val GROUP_TAG = "group"

/** A pixel, in dp, at most: the rounding of an edge. */
private const val PIXEL = 0.5f
