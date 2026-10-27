package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class VolumeSliderTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    /** The colour of the ring of the focus in the theme of the slider shown. */
    private var focusColor = Color.Unspecified

    @Test
    @DisplayName("TalkBack reads one slider with its value and moves it one step at a time")
    fun talkBackReadsOneSliderAndMovesItOneStepAtATime() = extension.use {
        // Given the slider at 50 %
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50)

        // Then one slider, read by its description and its value in percent, by steps of 10: TalkBack moves it by its
        // range over its steps plus one, 10 %
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(1)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(DESCRIPTION)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50 %"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(current = 50f, range = 0f..100f, steps = 9),
                ),
            )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.HideFromAccessibility))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))

        // When TalkBack moves it up, as its steps say, then asks for a value between two steps
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(23f) }

        // Then
        assertThat(asked).containsExactly(60, 20)
    }

    @Test
    @DisplayName("a touch on the track gives the nearest step")
    fun aTouchOnTheTrackGivesTheNearestStep() = extension.use {
        // Given the slider at 50 %
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50)

        // When the track is touched a third of the way, then a little more, along the way of the centre of the knob
        val track = onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true)
        track.performTouchInput { click(along(0.34f)) }
        track.performTouchInput { click(along(0.36f)) }

        // Then
        assertThat(asked).containsExactly(30, 40)
    }

    @Test
    @DisplayName("a drag asks for each step once, and the knob follows the finger while the value has not come back")
    fun aDragAsksForEachStepOnceAndTheKnobFollowsTheFinger() = extension.use {
        // Given the slider at 50 %, whose value does not come back while the finger moves (a write that lags)
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50)
        val track = onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

        // When a finger drags the knob to 60 %, by several moves within that step
        track.performTouchInput {
            down(along(0.5f))
            moveTo(along(0.58f))
            moveTo(along(0.6f))
            moveTo(along(0.61f))
            moveTo(along(0.62f))
        }

        // Then 60 % is asked for once, and the slider shows it under the finger
        assertThat(asked).containsExactly(60)
        slider.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "60 %"))

        // When the finger lifts, the value never having come back
        track.performTouchInput { up() }

        // Then the slider shows the value it is given
        slider.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50 %"))
    }

    @Test
    @DisplayName("a drag never takes the value out of its range")
    fun aDragNeverTakesTheValueOutOfItsRange() = extension.use {
        // Given the slider at 50 %
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50)

        // When a finger drags the knob past the end of the track, then past its start
        val track = onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true)
        track.performTouchInput { swipe(center, Offset(right + width, centerY)) }
        val toTheEnd = asked.toList()
        track.performTouchInput { swipe(center, Offset(left - width, centerY)) }

        // Then it stops at 100 %, then at 0 %
        assertThat(toTheEnd).isNotEmpty().allSatisfy { assertThat(it).isBetween(50, 100) }.endsWith(100)
        assertThat(asked.drop(toTheEnd.size)).isNotEmpty().allSatisfy { assertThat(it).isBetween(0, 50) }.endsWith(0)
    }

    @Test
    @DisplayName("a disabled slider shows its value but cannot be moved, and TalkBack reads it unavailable")
    fun aDisabledSliderCannotBeMoved() = extension.use {
        // Given the volume of the effects at 30 %, the effects off
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 30, enabled = false)

        // Then its value shows, and TalkBack reads it unavailable, with no way to move it
        onNodeWithText("30 %", useUnmergedTree = true).assertExists()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))

        // When a finger touches the track and drags along it
        val track = onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true)
        track.performTouchInput { click(along(0.9f)) }
        track.performTouchInput { swipe(center, Offset(right, centerY)) }

        // Then nothing is asked for
        assertThat(asked).isEmpty()
    }

    @Test
    @DisplayName("in a language read from right to left, the track starts on its right")
    fun inALanguageReadFromRightToLeftTheTrackStartsOnItsRight() = extension.use {
        // Given the slider at 50 %, read from right to left
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50, layoutDirection = LayoutDirection.Rtl)

        // When the track is touched a third of the way from its left
        onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true).performTouchInput { click(along(0.34f)) }

        // Then two thirds of the way from its start, on the right
        assertThat(asked).containsExactly(70)
    }

    @Test
    @DisplayName("a touch or TalkBack on the current value asks for nothing")
    fun aTouchOrTalkBackOnTheCurrentValueAsksForNothing() = extension.use {
        // Given the slider at 50 %
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 50)

        // When the track is touched at 50 %, and TalkBack asks for 50 % and for 52 %, nearest to 50 %
        onNodeWithTag(VOLUME_SLIDER_TRACK_TAG, useUnmergedTree = true).performTouchInput { click(along(0.5f)) }
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(50f) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(52f) }

        // Then
        assertThat(asked).isEmpty()
    }

    @Test
    @DisplayName("an arrow key moves the slider one step, never out of its range")
    fun anArrowKeyMovesTheSliderOneStepNeverOutOfItsRange() = extension.use {
        // Given the slider at 100 %, focused by the keyboard
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = 100, keyboard = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When the arrows right and up ask for more, then left and down for less
        listOf(Key.DirectionRight, Key.DirectionUp, Key.DirectionLeft, Key.DirectionDown).forEach { arrow ->
            slider.performKeyInput { pressKey(arrow) }
        }

        // Then nothing past 100 %, and a step down for each of the others
        assertThat(asked).containsExactly(90, 90)
    }

    @Test
    @DisplayName("a focused slider bears the focus ring")
    fun aFocusedSliderBearsTheFocusRing() = extension.use {
        // Given the slider, the keyboard in use
        setSlider(percent = 50, keyboard = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        val unfocused = colorOnFocusRingOf(slider)

        // When it takes the focus
        slider.requestFocus()

        // Then the ring of the buttons shows around it, where nothing was drawn before
        assertThat(colorOnFocusRingOf(slider)).isEqualTo(focusColor).isNotEqualTo(unfocused)
    }

    /** The point of the track [fraction] of the way the centre of the knob, of 28 dp, goes: from 14 dp to 14 dp off. */
    private fun TouchInjectionScope.along(fraction: Float): Offset {
        val inset = with(this) { 14.dp.toPx() }
        return Offset(inset + (width - 2 * inset) * fraction, centerY)
    }

    /**
     * Shows a volume slider at [percent], in Couleurs, off the edges of the screen, that keeps the volumes it is asked
     * for; [focusColor] is then the colour of the ring of the focus of its theme.
     */
    private fun ComposeContext.setSlider(
        onPercentChange: (Int) -> Unit = {},
        percent: Int,
        enabled: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        keyboard: Boolean = false,
    ) {
        setContent {
            if (keyboard) KeyboardInUse()
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                focusColor = BloxTheme.buttons.focus
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    VolumeSlider(
                        percent = percent,
                        onPercentChange = onPercentChange,
                        description = DESCRIPTION,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        enabled = enabled,
                    )
                }
            }
        }
    }

    private companion object {
        const val DESCRIPTION = "Volume de la musique"
    }
}
