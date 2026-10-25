package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import ch.magori.blox.core.designsystem.icon.BloxIcon
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
    @DisplayName("a pressed button goes 2 dp down at once, and is back 100 ms after it is released, not before")
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

        // When it is released, Then still down 4 frames later (67 ms), back at rest within 100 ms and a frame
        onNodeWithText("Menu").performTouchInput { up() }
        repeat(4) { mainClock.advanceTimeByFrame() }
        assertThat(labelTop() - atRest).isGreaterThan(0f)
        mainClock.advanceTimeBy(MOTION_PRESS_MS + FRAME_MS)
        assertThat(labelTop()).isEqualTo(atRest)
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

    /** The top of the label Menu, in dp, where the button draws it. */
    private fun ComposeContext.labelTop(): Float {
        val bounds = onNodeWithText("Menu", useUnmergedTree = true).getBoundsInRoot()
        return bounds.top.value
    }

    private companion object {
        const val TAG = "bubble"
        const val MOTION_PRESS_MS = 100L
        const val FRAME_MS = 16L
    }
}
