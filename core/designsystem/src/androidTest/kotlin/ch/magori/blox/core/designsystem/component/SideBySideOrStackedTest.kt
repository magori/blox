package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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

    /** Two items, the second higher than the first, whose labels are [labelDp] wide, on a line of [LINE] dp. */
    private fun ComposeContext.showItems(labelDp: Int) {
        setContent {
            val label = with(LocalDensity.current) { labelDp.dp.roundToPx() }
            Box(Modifier.width(LINE.dp)) {
                SideBySideOrStacked(labelWidths = listOf(label, label), padding = 10.dp, gap = 8.dp) { index, sizing ->
                    Box(
                        sizing
                            .testTag(if (index == 0) FIRST else SECOND)
                            .height(if (index == 0) 40.dp else 56.dp),
                    )
                }
            }
        }
    }
}

private const val FIRST = "first"
private const val SECOND = "second"
private const val LINE = 200
