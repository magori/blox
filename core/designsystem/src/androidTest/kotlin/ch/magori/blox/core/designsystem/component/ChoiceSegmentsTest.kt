package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxPalettes
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
internal class ChoiceSegmentsTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val tracks = listOf(ChoiceItem("Mélodie"), ChoiceItem("Basse"))
    private val tools =
        listOf(ChoiceItem("Poser", icon = BloxIcon.PLACE), ChoiceItem("Allonger", icon = BloxIcon.EXTEND))

    @Test
    @DisplayName(
        "segments without a visible label are one group of radio buttons, read under the name given to TalkBack",
    )
    fun segmentsWithoutAVisibleLabelAreOneGroupOfRadioButtons() = extension.use {
        // Given
        showSegments(tracks, selected = 0, name = "Piste")

        // Then one group, named, and no heading shown above it
        val groups = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .fetchSemanticsNodes()
        assertThat(groups).hasSize(1)
        assertThat(groups.single().config[SemanticsProperties.ContentDescription]).containsExactly("Piste")
        assertThat(onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).fetchSemanticsNodes())
            .isEmpty()
        // And its segments, radio buttons
        val group = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        val segments = onAllNodes(hasParent(group)).fetchSemanticsNodes()
        assertThat(segments.map { it.config[SemanticsProperties.Text].joinToString() })
            .containsExactly("Mélodie", "Basse")
        assertThat(segments.map { it.config[SemanticsProperties.Role] }).containsOnly(Role.RadioButton)
    }

    @Test
    @DisplayName("a segment bears its icon before its label, and the selected one its check in its place")
    fun aSegmentBearsItsIconBeforeItsLabelAndTheSelectedOneItsCheck() = extension.use {
        // Given the tools of the editor, Poser retained
        showSegments(tools, selected = 0, name = "Outil")

        // Then Allonger bears its icon, decorative, before its label, and no check
        val extend = onNodeWithText("Allonger").getUnclippedBoundsInRoot()
        val icon = onNode(hasTestTag(CHOICE_ICON_TAG) and hasAnySibling(hasText("Allonger")), useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .getUnclippedBoundsInRoot()
        val extendLabel = onNode(hasText("Allonger"), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertThat(icon.right).isLessThanOrEqualTo(extendLabel.left)
        assertThat(icon.left).isGreaterThanOrEqualTo(extend.left)
        onAllNodes(
            hasTestTag(CHOICE_CHECK_TAG) and hasAnySibling(hasText("Allonger")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        // And Poser, its check where its icon was, as large, and no icon
        val place = onNodeWithText("Poser").getUnclippedBoundsInRoot()
        val check = onNode(hasTestTag(CHOICE_CHECK_TAG) and hasAnySibling(hasText("Poser")), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val placeLabel = onNode(hasText("Poser"), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertThat(check.right).isLessThanOrEqualTo(placeLabel.left)
        assertThat(check.left).isGreaterThanOrEqualTo(place.left)
        assertThat((check.right - check.left).value).isCloseTo((icon.right - icon.left).value, within(PIXEL))
        onAllNodes(
            hasTestTag(CHOICE_ICON_TAG) and hasAnySibling(hasText("Poser")),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test
    @DisplayName("a selected segment is filled in the colours given to its choice")
    fun aSelectedSegmentIsFilledInTheColoursGivenToItsChoice() = extension.use {
        // Given the tracks of the editor, by day in Couleurs, each in its own colours
        val palette = BloxPalettes.ColorfulLight
        val choices = listOf(
            ChoiceItem("Mélodie", selectedColors = ChoiceColors(palette.melody.tab, palette.melody.tabInk)),
            ChoiceItem("Basse", selectedColors = ChoiceColors(palette.bass.tab, palette.bass.tabInk)),
        )
        showSegments(choices, selected = 1, kind = BloxThemeKind.COLORFUL)

        // Then the bass, retained, is filled in its colour: halfway between the edge of its segment and its check, at
        // the height of the middle of its check
        val segment = onNodeWithText("Basse").getUnclippedBoundsInRoot()
        val check = onNode(hasTestTag(CHOICE_CHECK_TAG) and hasAnySibling(hasText("Basse")), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val image = onNodeWithText("Basse").captureToImage().toPixelMap()
        val (x, y) = with(density) {
            ((check.left - segment.left) / 2).roundToPx() to ((check.top + check.bottom) / 2 - segment.top).roundToPx()
        }
        assertThat(image[x, y]).isEqualTo(palette.bass.tab)
    }

    @Test
    @DisplayName(
        "right to left, a segment bears its icon, or the selected one its check, at the start of its line, its right",
    )
    fun rightToLeftTheIconAndTheCheckStandAtTheRightOfTheLabel() = extension.use {
        // Given the tools of the editor, Poser retained, right to left
        showSegments(
            tools,
            selected = 0,
            name = "Outil",
            kind = BloxThemeKind.COLORFUL,
            direction = LayoutDirection.Rtl,
        )

        // Then the first tool stands at the right, Allonger at the left
        val place = onNodeWithText("Poser").getUnclippedBoundsInRoot()
        val extend = onNodeWithText("Allonger").getUnclippedBoundsInRoot()
        assertThat(extend.right).isLessThanOrEqualTo(place.left)
        // And the icon of Allonger and the check of Poser stand at the right of their labels, within their segments
        val icon = onNode(hasTestTag(CHOICE_ICON_TAG) and hasAnySibling(hasText("Allonger")), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val extendLabel = onNode(hasText("Allonger"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(icon.left).isGreaterThanOrEqualTo(extendLabel.right)
        assertThat(icon.right).isLessThanOrEqualTo(extend.right)
        val check = onNode(hasTestTag(CHOICE_CHECK_TAG) and hasAnySibling(hasText("Poser")), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val placeLabel = onNode(hasText("Poser"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertThat(check.left).isGreaterThanOrEqualTo(placeLabel.right)
        assertThat(check.right).isLessThanOrEqualTo(place.right)
    }

    @Test
    @DisplayName("each segment is 48 dp high as a target")
    fun eachSegmentIs48DpHighAsATarget() = extension.use {
        // Given the tools, with their icons
        showSegments(tools, selected = 0, name = "Outil")

        // Then each whole segment may be touched, frame of the group included: 48 dp, no more
        val segments = onAllNodes(hasClickAction())
        segments.assertCountEquals(tools.size)
        repeat(tools.size) { index ->
            segments[index]
                .assertHeightIsEqualTo(BloxSizes.minTouchTarget)
                .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    private fun ComposeContext.showSegments(
        choices: List<ChoiceItem>,
        selected: Int,
        name: String = "Piste",
        onChosen: (Int) -> Unit = {},
        kind: BloxThemeKind = BloxThemeKind.ORIGINAL,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                BloxTheme(kind = kind, darkSystem = false) {
                    ChoiceSegments(
                        name = name,
                        choices = choices,
                        selected = selected,
                        onChosen = onChosen,
                        // Clear of the bars of the system, which a touch at the top of the screen would open.
                        modifier = Modifier.safeDrawingPadding().width(360.dp),
                    )
                }
            }
        }
    }
}

/** A pixel, in dp, at most: the rounding of an edge. */
private const val PIXEL = 0.5f
