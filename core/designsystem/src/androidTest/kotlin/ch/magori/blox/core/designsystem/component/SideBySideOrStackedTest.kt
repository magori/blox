package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class SideBySideOrStackedTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("items whose labels hold in their half stand side by side, as wide and as high as each other")
    fun itemsWhoseLabelsHoldStandSideBySide() = extension.use {
        // Given 200 dp, 8 apart and 10 on each side of a label: 76 dp for each label
        showItems(labelDp = 76)

        // Then
        val first = onNodeWithTag(FIRST).getUnclippedBoundsInRoot()
        val second = onNodeWithTag(SECOND).getUnclippedBoundsInRoot()
        assertThat(second.top).isEqualTo(first.top)
        assertThat(second.left).isGreaterThan(first.right)
        assertThat(second.right - second.left).isEqualTo(first.right - first.left)
        assertThat(second.bottom - second.top).isEqualTo(first.bottom - first.top)
    }

    @Test
    @DisplayName("items whose labels do not hold in their half stand one above the other, each as wide as the line")
    fun itemsWhoseLabelsDoNotHoldAreStacked() = extension.use {
        // Given a label a dp wider than its room
        showItems(labelDp = 77)

        // Then
        val first = onNodeWithTag(FIRST).getUnclippedBoundsInRoot()
        val second = onNodeWithTag(SECOND).getUnclippedBoundsInRoot()
        assertThat(second.top).isGreaterThan(first.bottom)
        assertThat(first.right - first.left).isEqualTo(LINE.dp)
        assertThat(second.right - second.left).isEqualTo(LINE.dp)
    }

    @Test
    @DisplayName("given only the width it asks, as beside another part, the items stand one above the other, as wide")
    fun givenOnlyTheWidthItAsksTheItemsAreStacked() = extension.use {
        // Given items that ask 60 and 80 dp, in a box as wide as they ask: 80 dp
        setContent {
            Box(Modifier.width(IntrinsicSize.Max)) {
                SideBySideOrStacked(gap = 8.dp) {
                    Box(Modifier.testTag(FIRST).width(60.dp).height(40.dp))
                    Box(Modifier.testTag(SECOND).width(80.dp).height(40.dp))
                }
            }
        }

        // Then
        val first = onNodeWithTag(FIRST).getUnclippedBoundsInRoot()
        val second = onNodeWithTag(SECOND).getUnclippedBoundsInRoot()
        assertThat(second.top).isGreaterThan(first.bottom)
        assertThat(first.right - first.left).isEqualTo(80.dp)
        assertThat(second.right - second.left).isEqualTo(80.dp)
    }

    @Test
    @DisplayName("with no label widths, items stand side by side while each holds in its share as wide as it asks")
    fun withNoLabelWidthsItemsAskTheirOwnWidth() = extension.use {
        // Given 200 dp, 8 apart: a share of 96 dp, for items that ask 96 dp
        var firstAsks by mutableIntStateOf(96)
        var secondAsks by mutableIntStateOf(96)
        setContent {
            Box(Modifier.width(LINE.dp)) {
                SideBySideOrStacked(gap = 8.dp) {
                    Box(Modifier.testTag(FIRST).width(firstAsks.dp).height(40.dp))
                    Box(Modifier.testTag(SECOND).width(secondAsks.dp).height(40.dp))
                }
            }
        }

        // Then
        assertThat(standSideBySide()).isTrue()

        // When the first asks less, 60 dp, and the second a dp more than its share, 97 dp
        firstAsks = 60
        secondAsks = 97

        // Then
        assertThat(standSideBySide()).isFalse()
    }

    private fun ComposeContext.standSideBySide(): Boolean {
        val first = onNodeWithTag(FIRST).getUnclippedBoundsInRoot()
        val second = onNodeWithTag(SECOND).getUnclippedBoundsInRoot()
        return second.top == first.top
    }

    /** Two items, the second higher than the first, whose labels are [labelDp] wide, on a line of [LINE] dp. */
    private fun ComposeContext.showItems(labelDp: Int) {
        setContent {
            val label = with(LocalDensity.current) { labelDp.dp.roundToPx() }
            Box(Modifier.width(LINE.dp)) {
                SideBySideOrStacked(gap = 8.dp, labelWidths = listOf(label, label), padding = 10.dp) {
                    Box(Modifier.testTag(FIRST).height(40.dp))
                    Box(Modifier.testTag(SECOND).height(56.dp))
                }
            }
        }
    }
}

private const val FIRST = "first"
private const val SECOND = "second"
private const val LINE = 200
