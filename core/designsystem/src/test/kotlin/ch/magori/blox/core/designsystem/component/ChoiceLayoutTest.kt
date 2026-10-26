package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

internal class ChoiceLayoutTest {
    @Test
    fun `cells stay side by side while their labels need not shrink below three quarters of their size`() {
        // Given labels of 100 px, and 40 px of margins and check in each cell: 115 px each at three quarters
        val fitting = fittingColumns(columns = 2, labelWidth = 100, fixedWidth = 40, width = 230)

        // Then
        assertThat(fitting).isEqualTo(2)
    }

    @Test
    fun `cells stack when their labels would shrink below three quarters of their size, margins and check kept`() {
        // Given one pixel less than two cells at three quarters of their labels
        val fitting = fittingColumns(columns = 2, labelWidth = 100, fixedWidth = 40, width = 229)

        // Then the margins and the check do not shrink: only the label would, too far
        assertThat(fitting).isEqualTo(1)
    }

    @Test
    fun `a group of five columns too narrow for five keeps as many as fit, one at least`() {
        // When
        val four = fittingColumns(columns = 5, labelWidth = 20, fixedWidth = 45, width = 240)
        val one = fittingColumns(columns = 5, labelWidth = 400, fixedWidth = 45, width = 240)

        // Then
        assertThat(four).isEqualTo(4)
        assertThat(one).isEqualTo(1)
    }

    @Test
    fun `the check in the corner of a cell stays within its fill, whose corners are those of radius-md`() {
        // Given a start level selected, its fill 53 dp by 40 dp, in Couleurs, whose corners are round
        val fill = segmentFillShape(checkInCorner = true, shapes = BloxShapeSets.Colorful)
        val size = Size(53f, 40f)
        val outline = fill.createOutline(size, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded

        // When the check is drawn in its top end corner
        val side = CORNER_CHECK_SIZE.value
        val check = checkStroke(side)
        val origin = Offset(size.width - CORNER_CHECK_INSET.value - side, CORNER_CHECK_INSET.value)

        // Then every point of its stroke, with half its width around it, stands within the fill
        val margin = check.width / 2 * sqrt(2f)
        check.points.map { it + origin }.forEach { point ->
            listOf(Offset(margin, -margin), Offset(margin, margin), Offset(-margin, -margin)).forEach { edge ->
                assertThat(outline.roundRect.contains(point + edge)).describedAs("$point").isTrue()
            }
        }
    }

    @Test
    fun `a selected segment of one row is filled as a pill`() {
        // When
        val fill = segmentFillShape(checkInCorner = false, shapes = BloxShapeSets.Colorful)

        // Then
        assertThat(fill).isEqualTo(BloxShapeSets.Colorful.pill)
    }

    @Test
    fun `a group asks for a selected choice that is available, and one column at least`() {
        // Given
        val choices = listOf(ChoiceItem("Classique"), ChoiceItem("Moderne", available = false))

        // Then
        checkChoices(choices, selected = 0, columns = 2)
        assertThatIllegalArgumentException().isThrownBy { checkChoices(choices, selected = 1, columns = 2) }
        assertThatIllegalArgumentException().isThrownBy { checkChoices(choices, selected = 2, columns = 2) }
        assertThatIllegalArgumentException().isThrownBy { checkChoices(choices, selected = -1, columns = 2) }
        assertThatIllegalArgumentException().isThrownBy { checkChoices(choices, selected = 0, columns = 0) }
    }
}
