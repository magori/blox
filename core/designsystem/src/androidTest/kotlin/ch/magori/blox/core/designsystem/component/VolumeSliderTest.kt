package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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

    /** The point of the track [fraction] of the way the centre of the knob, of 28 dp, goes: from 14 dp to 14 dp off. */
    private fun TouchInjectionScope.along(fraction: Float): Offset {
        val inset = with(this) { 14.dp.toPx() }
        return Offset(inset + (width - 2 * inset) * fraction, centerY)
    }

    /** Shows a volume slider at [percent], in Couleurs, that keeps the volumes it is asked for. */
    private fun ComposeContext.setSlider(
        onPercentChange: (Int) -> Unit = {},
        percent: Int,
        enabled: Boolean = true,
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                VolumeSlider(
                    percent = percent,
                    onPercentChange = onPercentChange,
                    description = DESCRIPTION,
                    enabled = enabled,
                )
            }
        }
    }

    private companion object {
        const val DESCRIPTION = "Volume de la musique"
    }
}
