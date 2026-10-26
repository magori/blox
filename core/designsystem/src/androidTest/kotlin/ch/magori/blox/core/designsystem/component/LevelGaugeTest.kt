package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
internal class LevelGaugeTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("minus is disabled at the lowest step and plus at the highest")
    fun minusIsDisabledAtTheLowestStepAndPlusAtTheHighest() = extension.use {
        // Given the gauge at its lowest level
        var level by mutableIntStateOf(0)
        setGauge { level }

        // Then minus cannot be touched, plus can
        onNodeWithContentDescription(MINUS).assertIsNotEnabled()
        onNodeWithContentDescription(PLUS).assertIsEnabled()

        // When it is at its highest level, Then it is the other way round
        level = 9
        onNodeWithContentDescription(MINUS).assertIsEnabled()
        onNodeWithContentDescription(PLUS).assertIsNotEnabled()
    }

    @Test
    @DisplayName("minus and plus move the gauge one step down and up")
    fun minusAndPlusMoveTheGaugeOneStep() = extension.use {
        // Given the gauge at the level 5, which asks for the levels touched
        val asked = mutableListOf<Int>()
        setGauge(onLevelChange = { asked += it }) { 5 }

        // When
        onNodeWithContentDescription(MINUS).performClick()
        onNodeWithContentDescription(PLUS).performClick()

        // Then
        assertThat(asked).containsExactly(4, 6)
    }

    @Test
    @DisplayName(
        "a touch on a bar or a drag along the gauge gives the level of the nearest bar, never out of its range",
    )
    fun aTouchOrADragGivesTheLevelOfTheNearestBar() = extension.use {
        // Given the gauge at the level 5
        val asked = mutableListOf<Int>()
        setGauge(onLevelChange = { asked += it }) { 5 }
        val bars = onNodeWithTag(LEVEL_GAUGE_BARS_TAG, useUnmergedTree = true)

        // When the bar of the level 7 is touched: bars of 8 dp, 4 dp apart, its centre 4 + 7 × 12 dp from the start
        bars.performTouchInput { click(Offset(88.dp.toPx(), centerY)) }
        // And a finger drags from the middle to well past the start
        bars.performTouchInput { swipe(center, Offset(left - width, centerY)) }

        // Then the level 7, then levels down to 0, never below
        assertThat(asked.first()).isEqualTo(7)
        assertThat(asked.drop(1)).isNotEmpty().allSatisfy { assertThat(it).isBetween(0, 5) }.endsWith(0)
    }

    @Test
    @DisplayName("TalkBack reads one slider with its value and moves it one step at a time")
    fun talkBackReadsOneSliderAndMovesItOneStepAtATime() = extension.use {
        // Given the gauge at the level 5
        val asked = mutableListOf<Int>()
        setGauge(onLevelChange = { asked += it }) { 5 }

        // Then one slider, read by its description and its level, whose steps are the levels: TalkBack moves it by
        // its range over its steps plus one, a level
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(1)
        val slider = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(DESCRIPTION)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "5"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(current = 5f, range = 0f..9f, steps = 8),
                ),
            )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.HideFromAccessibility))
        // And minus and plus, there for a finger, are left out
        listOf(MINUS, PLUS).forEach { button ->
            onNodeWithContentDescription(button)
                .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility))
        }

        // When TalkBack moves it up, as its steps say, then asks for a value between two levels
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(3.4f) }

        // Then
        assertThat(asked).containsExactly(6, 3)
    }

    @Test
    @DisplayName("in a language read from right to left, the leftmost bar is the highest level")
    fun inALanguageReadFromRightToLeftTheLeftmostBarIsTheHighestLevel() = extension.use {
        // Given the gauge at the level 5, read from right to left
        val asked = mutableListOf<Int>()
        setGauge(onLevelChange = { asked += it }, layoutDirection = LayoutDirection.Rtl) { 5 }

        // When the bar on the far left is touched, at its centre, 4 dp from the left
        onNodeWithTag(LEVEL_GAUGE_BARS_TAG, useUnmergedTree = true)
            .performTouchInput { click(Offset(4.dp.toPx(), centerY)) }

        // Then
        assertThat(asked).containsExactly(9)
    }

    /** Shows a gauge of the levels 0 to 9 at [level], in Couleurs, that keeps the levels it is asked for. */
    private fun ComposeContext.setGauge(
        onLevelChange: (Int) -> Unit = {},
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        level: () -> Int,
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    LevelGauge(level = level(), levels = 0..9, onLevelChange = onLevelChange, description = DESCRIPTION)
                }
            }
        }
    }

    private companion object {
        const val DESCRIPTION = "Niveau de départ"
        const val MINUS = "Moins"
        const val PLUS = "Plus"
    }
}
