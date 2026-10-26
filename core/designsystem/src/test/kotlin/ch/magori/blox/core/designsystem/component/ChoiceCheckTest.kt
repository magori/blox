package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

internal class ChoiceCheckTest {
    @Test
    fun `the check is a stroke down then up, drawn wholly within its square`() {
        // Given
        val side = 48f

        // When
        val check = checkStroke(side)

        // Then a short stroke down to the right, then a longer one up to the right
        val (start, bottom, end) = check.points
        assertThat(bottom.x).isGreaterThan(start.x)
        assertThat(bottom.y).isGreaterThan(start.y)
        assertThat(end.x).isGreaterThan(bottom.x)
        assertThat(end.y).isLessThan(start.y)
        // And it never passes beyond its square onto the label: a square end, as a pointed join, may reach up to the
        // half of its width times the square root of 2 from a point
        val margin = check.width / 2 * sqrt(2f)
        check.points.forEach { point ->
            assertThat(point.x - margin).isGreaterThanOrEqualTo(0f)
            assertThat(point.x + margin).isLessThanOrEqualTo(side)
            assertThat(point.y - margin).isGreaterThanOrEqualTo(0f)
            assertThat(point.y + margin).isLessThanOrEqualTo(side)
        }
    }

    @Test
    fun `only a selected choice makes room for its check and its gap before its label`() {
        // Given a label of a normal font
        val label = 14.dp

        // When
        val selected = choiceInsets(selected = true, label = label)
        val unselected = choiceInsets(selected = false, label = label)

        // Then the inner margin of a choice, space-3, on each side
        assertThat(selected).isEqualTo(ChoiceInsets(start = 12.dp, check = 14.dp, gap = 4.dp, end = 12.dp))
        assertThat(unselected).isEqualTo(ChoiceInsets(start = 12.dp, check = 0.dp, gap = 0.dp, end = 12.dp))
    }

    @Test
    fun `the check is as large as its label, 24 dp at most, a quarter of the label away from it, 4 dp at least`() {
        // When
        val normal = choiceInsets(selected = true, label = 14.dp)
        val larger = choiceInsets(selected = true, label = 20.dp)
        val doubled = choiceInsets(selected = true, label = 28.dp)

        // Then
        assertThat(normal.check to normal.gap).isEqualTo(14.dp to 4.dp)
        assertThat(larger.check to larger.gap).isEqualTo(20.dp to 5.dp)
        assertThat(doubled.check to doubled.gap).isEqualTo(24.dp to 7.dp)
    }
}
