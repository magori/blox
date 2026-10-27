package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.frame.TICKS_PER_SECOND
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class BubbleButtonTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("a pressed button goes 2 dp down at once, stays down while motion press lasts, then is back at rest")
    fun aPressedButtonGoesDownAtOnceAndComesBack() = extension.use {
        // Given a button at rest, on a clock that only moves when told
        mainClock.autoAdvance = false
        setContent { BloxTheme(kind = BloxThemeKind.COLORFUL) { SecondaryButton("Menu", BloxIcon.HOME, onClick = {}) } }
        mainClock.advanceTimeByFrame()
        val atRest = labelTop()

        // When it is pressed, Then it is 2 dp lower on the next frame
        onNodeWithText("Menu").performTouchInput { down(center) }
        mainClock.advanceTimeByFrame()
        assertThat(labelTop() - atRest).isCloseTo(2f, within(0.5f))

        // When it is released, frame by frame
        onNodeWithText("Menu").performTouchInput { up() }
        val returnFrames = (BloxMotion.press * MILLIS_PER_SECOND / TICKS_PER_SECOND / FRAME_MS).toInt()
        val drops = (1..returnFrames + SLACK_FRAMES).map {
            mainClock.advanceTimeByFrame()
            labelTop() - atRest
        }

        // Then still down at each frame of motion press, the last one just before its end, and back at rest after it
        assertThat(drops.take(returnFrames)).allSatisfy { assertThat(it).isPositive() }
        assertThat(drops.last()).isZero()
    }

    @Test
    @DisplayName("in reduced effects a pressed button stays in place, and so does it once released")
    fun inReducedEffectsAPressedButtonStaysInPlace() = extension.use {
        // Given a button at rest, in reduced effects
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL, effects = BloxEffects(reduced = true, forcedBySystem = false)) {
                SecondaryButton("Menu", BloxIcon.HOME, onClick = {})
            }
        }
        mainClock.advanceTimeByFrame()
        val atRest = labelTop()

        // When it is pressed
        onNodeWithText("Menu").performTouchInput { down(center) }
        mainClock.advanceTimeByFrame()
        val pressed = labelTop()

        // And released
        onNodeWithText("Menu").performTouchInput { up() }
        mainClock.advanceTimeByFrame()

        // Then
        assertThat(pressed).isEqualTo(atRest)
        assertThat(labelTop()).isEqualTo(atRest)
    }

    @Test
    @DisplayName("a button tapped briefly in a scrolling column, pressed and released in one frame, still goes down")
    fun aBriefTapInAScrollingColumnStillGoesDown() = extension.use {
        // Given a button in a column that scrolls, as Pause and Fin de partie: its press is held back a little
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SecondaryButton("Menu", BloxIcon.HOME, onClick = {})
                }
            }
        }
        mainClock.advanceTimeByFrame()
        val atRest = labelTop()

        // When it is pressed and released in one frame
        onNodeWithText("Menu").performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeByFrame()

        // Then it is lower on the next frame, on its way back up
        assertThat(labelTop()).isGreaterThan(atRest)
    }

    @Test
    @DisplayName("a disabled button is read as disabled and does nothing when touched")
    fun aDisabledButtonDoesNothing() = extension.use {
        // Given
        var clicks = 0
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                SecondaryButton("Effacer", BloxIcon.TRASH, onClick = { clicks++ }, enabled = false)
            }
        }

        // When
        onNodeWithText("Effacer").performClick()

        // Then
        onNodeWithText("Effacer").assertIsNotEnabled()
        assertThat(clicks).isZero()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a labelled button is read once, as a button of its label, its icon adding nothing")
    fun aLabelledButtonIsReadOnce(kind: BloxThemeKind) = extension.use {
        // Given
        setContent {
            BloxTheme(kind = kind) { PrimaryButton("Reprendre", BloxIcon.PLAY, onClick = {}) }
        }

        // Then
        onNodeWithText("Reprendre")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        onAllNodes(hasText("Reprendre")).assertCountEquals(1)
    }

    @ParameterizedTest
    @EnumSource(ButtonFamily::class)
    @DisplayName("a button of each family is 48 dp high and wide at least, and as high as its family")
    fun aButtonOfEachFamilyIsLargeEnough(family: ButtonFamily) = extension.use {
        // Given a button of nothing but a dot, the least it can be
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BubbleButton(family, onClick = {}, modifier = Modifier.testTag(TAG)) {}
            }
        }

        // Then
        onNodeWithTag(TAG)
            .assertWidthIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(BloxSizes.minTouchTarget)
            .assertHeightIsAtLeast(family.minHeight)
    }

    @ParameterizedTest
    @EnumSource(ButtonFamily::class, names = ["PRIMARY", "SECONDARY"])
    @DisplayName("a compact main or secondary button is 48 dp high")
    fun aCompactButtonIs48DpHigh(family: ButtonFamily) = extension.use {
        // Given a compact button of the family, as wide as a measure
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                BubbleButton(
                    family = family,
                    onClick = {},
                    size = ButtonSize.COMPACT,
                    modifier = Modifier.testTag(TAG).width(160.dp),
                ) { Text("Ok") }
            }
        }

        // Then
        onNodeWithTag(TAG).assertHeightIsEqualTo(BloxSizes.minTouchTarget)
    }

    @Test
    @DisplayName("a selected button is read as selected by TalkBack, a plain one is not")
    fun aSelectedButtonIsReadAsSelected() = extension.use {
        // Given the choice made and another
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Column {
                    BubbleButton(ButtonFamily.SECONDARY, onClick = {
                    }, emphasis = ButtonEmphasis.SELECTED) { Text("Mesure 1") }
                    BubbleButton(ButtonFamily.SECONDARY, onClick = {}) { Text("Mesure 2") }
                }
            }
        }

        // Then
        onNodeWithText("Mesure 1").assertIsSelected()
        onNodeWithText("Mesure 2").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }

    @Test
    @DisplayName("a tile given radius md fills the corner that a pill leaves to the page")
    fun aTileFillsTheCornerThatAPillLeavesToThePage() = extension.use {
        // Given a pill and a tile in radius md, of the same size, on the page
        var page = Color.Unspecified
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                page = BloxTheme.palette.page
                Column(Modifier.background(page).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val size = Modifier.size(200.dp, 56.dp)
                    SecondaryButton("Pilule", BloxIcon.LIST, onClick = {}, modifier = Modifier.testTag(PILL).then(size))
                    BubbleButton(
                        family = ButtonFamily.SECONDARY,
                        onClick = {},
                        shape = BloxTheme.shapes.md,
                        modifier = Modifier.testTag(TILE).then(size),
                    ) { Text("Tuile") }
                }
            }
        }

        // Then 7 dp into the corner, out of a pill of 56 dp (its radius is 28) but within radius md (16)
        assertThat(cornerOf(PILL)).isEqualTo(page)
        assertThat(cornerOf(TILE)).isNotEqualTo(page)
    }

    /** The colour 7 dp right of and under the top left corner of the node tagged [tag]. */
    private fun ComposeContext.cornerOf(tag: String): Color {
        val inset = with(density) { 7.dp.roundToPx() }
        return onNodeWithTag(tag).captureToImage().toPixelMap()[inset, inset]
    }

    /** The top of the label Menu, in dp, where the button draws it. */
    private fun ComposeContext.labelTop(): Float {
        val bounds = onNodeWithText("Menu", useUnmergedTree = true).getBoundsInRoot()
        return bounds.top.value
    }

    private companion object {
        const val TAG = "bubble"
        const val PILL = "pill"
        const val TILE = "tile"
        const val MILLIS_PER_SECOND = 1000L

        /** A frame of the clock of the tests. */
        const val FRAME_MS = 16L

        /**
         * The frames a button may take after motion press to be back: one to start counting, one that only sets the
         * time reference (FrameTicks), and one as a frame of 16 ms is a little less than a tick.
         */
        const val SLACK_FRAMES = 3
    }
}
