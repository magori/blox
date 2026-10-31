package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.assertIsFocused
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
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.buttonColorsOf
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

    @Test
    @DisplayName("TalkBack reads one slider with its value and moves it one step at a time")
    fun talkBackReadsOneSliderAndMovesItOneStepAtATime() = extension.use {
        // Given the slider at 50 %
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = { 50 })

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
        setSlider(onPercentChange = { asked += it }, percent = { 50 })

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
        setSlider(onPercentChange = { asked += it }, percent = { 50 })
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
        setSlider(onPercentChange = { asked += it }, percent = { 50 })

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
        setSlider(onPercentChange = { asked += it }, percent = { 30 }, enabled = false)

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
        setSlider(onPercentChange = { asked += it }, percent = { 50 }, layoutDirection = LayoutDirection.Rtl)

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
        setSlider(onPercentChange = { asked += it }, percent = { 50 })

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
        var percent by mutableIntStateOf(100)
        setSlider(onPercentChange = { asked += it }, percent = { percent }, keyboard = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When right asks for more at the top, and left for less
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        slider.requestFocus()
        slider.performKeyInput { pressKey(Key.DirectionLeft) }
        // And at 0 %, left asks for less, then right for more
        percent = 0
        waitForIdle()
        slider.performKeyInput { pressKey(Key.DirectionLeft) }
        slider.requestFocus()
        slider.performKeyInput { pressKey(Key.DirectionRight) }

        // Then nothing out of the range, and a step for each of the others
        assertThat(asked).containsExactly(90, 10)
    }

    @Test
    @DisplayName("two arrows pressed before the value comes back move the slider two steps")
    fun twoArrowsPressedBeforeTheValueComesBackMoveTheSliderTwoSteps() = extension.use {
        // Given the slider at 50 %, focused, whose value does not come back (a write that lags)
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = { 50 }, keyboard = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When right is pressed twice
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        slider.performKeyInput { pressKey(Key.DirectionRight) }

        // Then each press asks for one step more than the last
        assertThat(asked).containsExactly(60, 70)
    }

    @Test
    @DisplayName("an arrow pressed while the value comes back late goes on from the last step asked")
    fun anArrowPressedWhileTheValueComesBackLateGoesOnFromTheLastStepAsked() = extension.use {
        // Given the slider at 50 %, focused, right pressed twice before the value comes back
        val asked = mutableListOf<Int>()
        var percent by mutableIntStateOf(50)
        setSlider(onPercentChange = { asked += it }, percent = { percent }, keyboard = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        slider.performKeyInput { pressKey(Key.DirectionRight) }

        // When the first step asked for comes back, late, then right is pressed again
        percent = 60
        waitForIdle()
        slider.performKeyInput { pressKey(Key.DirectionRight) }

        // Then the third press goes on from the last step asked for, not from the value come back
        assertThat(asked).containsExactly(60, 70, 80)
    }

    @Test
    @DisplayName("a held arrow stops at the end of the range and a new press leaves it")
    fun aHeldArrowStopsAtTheEndOfTheRangeAndANewPressLeavesIt() = extension.use {
        // Given the slider at 90 %, whose value comes back, focused, a target of the focus after it on its line
        val asked = mutableListOf<Int>()
        var percent by mutableIntStateOf(90)
        setSlider(
            onPercentChange = {
                asked += it
                percent = it
            },
            percent = { percent },
            keyboard = true,
            neighbour = true,
        )
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When right is held long enough to repeat, past the end of the range
        slider.performKeyInput {
            keyDown(Key.DirectionRight)
            advanceEventTime(1_000)
            keyUp(Key.DirectionRight)
        }

        // Then the slider stops at 100 %, and keeps the focus
        slider.assertIsFocused()
        assertThat(asked).containsExactly(100)

        // And when right is pressed again, the focus goes on to the target
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag(NEXT).assertIsFocused()
        assertThat(asked).containsExactly(100)
    }

    @Test
    @DisplayName("in a language read from right to left, the left arrow raises the slider")
    fun inALanguageReadFromRightToLeftTheLeftArrowRaisesTheSlider() = extension.use {
        // Given the slider at 50 %, read from right to left, focused: its track starts on the right
        val asked = mutableListOf<Int>()
        setSlider(
            onPercentChange = { asked += it },
            percent = { 50 },
            layoutDirection = LayoutDirection.Rtl,
            keyboard = true,
        )
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When left is pressed
        slider.performKeyInput { pressKey(Key.DirectionLeft) }

        // Then it moves the knob the way it points, up
        assertThat(asked).containsExactly(60)
    }

    @Test
    @DisplayName("a disabled slider cannot take the focus, and its arrows ask for nothing")
    fun aDisabledSliderCannotTakeTheFocusAndItsArrowsAskForNothing() = extension.use {
        // Given the slider at 30 %, disabled, the focus on the target after it
        val asked = mutableListOf<Int>()
        setSlider(
            onPercentChange = { asked += it },
            percent = { 30 },
            enabled = false,
            keyboard = true,
            neighbour = true,
        )
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        val next = onNodeWithTag(NEXT)
        next.requestFocus()

        // When the arrow towards it is pressed
        next.performKeyInput { pressKey(Key.DirectionLeft) }

        // Then it offers no way to take the focus, the focus stays where it was, and nothing is asked for
        slider.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.RequestFocus))
        next.assertIsFocused()
        assertThat(asked).isEmpty()
    }

    @Test
    @DisplayName("an arrow at the end of the range leaves the slider to the focus")
    fun anArrowAtTheEndOfTheRangeLeavesTheSliderToTheFocus() = extension.use {
        // Given the slider at 100 %, focused, a target of the focus after it on its line
        val asked = mutableListOf<Int>()
        setSlider(onPercentChange = { asked += it }, percent = { 100 }, keyboard = true, neighbour = true)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.requestFocus()

        // When right is pressed, the slider being unable to go further
        slider.performKeyInput { pressKey(Key.DirectionRight) }

        // Then the focus goes on to the target, and nothing is asked for
        onNodeWithTag(NEXT).assertIsFocused()
        assertThat(asked).isEmpty()
    }

    @Test
    @DisplayName("a focused slider bears the focus ring")
    fun aFocusedSliderBearsTheFocusRing() = extension.use {
        // Given the slider, the keyboard in use, the focus on the target after it
        setSlider(percent = { 50 }, keyboard = true, neighbour = true)
        onNodeWithTag(NEXT).requestFocus()
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        val unfocused = colorOnFocusRingOf(slider)

        // When it takes the focus
        slider.requestFocus()

        // Then the ring of the buttons shows around it, where nothing was drawn before
        assertThat(colorOnFocusRingOf(slider))
            .isEqualTo(buttonColorsOf(BloxThemeVariant.COLORFUL_LIGHT).focus)
            .isNotEqualTo(unfocused)
    }

    /** The point of the track [fraction] of the way the centre of the knob, of 28 dp, goes: from 14 dp to 14 dp off. */
    private fun TouchInjectionScope.along(fraction: Float): Offset {
        val inset = with(this) { 14.dp.toPx() }
        return Offset(inset + (width - 2 * inset) * fraction, centerY)
    }

    /**
     * Shows a volume slider at [percent], in Couleurs by day, off the edges of the screen, that keeps the volumes it is
     * asked for; with a [neighbour], a target of the focus, [NEXT], after it on its line.
     */
    private fun ComposeContext.setSlider(
        onPercentChange: (Int) -> Unit = {},
        percent: () -> Int,
        enabled: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        keyboard: Boolean = false,
        neighbour: Boolean = false,
    ) {
        setContent {
            if (keyboard) KeyboardInUse()
            BloxTheme(kind = BloxThemeKind.COLORFUL, darkSystem = false) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Row {
                        VolumeSlider(
                            percent = percent(),
                            onPercentChange = onPercentChange,
                            description = DESCRIPTION,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            enabled = enabled,
                        )
                        if (neighbour) FocusStop(NEXT)
                    }
                }
            }
        }
    }

    private companion object {
        const val DESCRIPTION = "Volume de la musique"
        const val NEXT = "next"
    }
}
