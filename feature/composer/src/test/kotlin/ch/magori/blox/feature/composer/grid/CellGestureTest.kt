package ch.magori.blox.feature.composer.grid

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CellGestureTest {
    // Cells 10 px wide, a touch slop of 4 px: the finger goes down in the middle of the third cell.
    private fun gesture() = CellGestureTracker(cellWidth = 10f, touchSlop = 4f, downX = 25f, downY = 5f)

    @Test
    fun `a finger lifted without sliding farther than the touch slop touches its cell`() {
        // Given
        val gesture = gesture()

        // When
        gesture.move(x = 28f, y = 7f)

        // Then
        assertThat(gesture.lift()).isEqualTo(CellGesture.Touch(eighth = 2))
    }

    @Test
    fun `a finger slid along the line slides from its cell to the cell where it is lifted`() {
        // Given
        val gesture = gesture()

        // When
        gesture.move(x = 40f, y = 6f)
        gesture.move(x = 57f, y = 6f)

        // Then
        assertThat(gesture.isSliding).isTrue()
        assertThat(gesture.lift()).isEqualTo(CellGesture.Slide(fromEighth = 2, toEighth = 5))
    }

    @Test
    fun `a finger slid then brought back to its cell does nothing, neither a touch nor a slide`() {
        // Given
        val gesture = gesture()

        // When
        gesture.move(x = 45f, y = 5f)
        gesture.move(x = 26f, y = 5f)

        // Then
        assertThat(gesture.lift()).isNull()
    }

    @Test
    fun `a cancelled gesture does nothing`() {
        // Given
        val gesture = gesture()

        // When the system cancels it, or another gesture takes it
        gesture.cancel()

        // Then
        assertThat(gesture.isAbandoned).isTrue()
        assertThat(gesture.lift()).isNull()
    }

    @Test
    fun `a finger moved up or down before sliding is left to the scroll of the grid, and does nothing`() {
        // Given
        val gesture = gesture()

        // When
        gesture.move(x = 27f, y = 20f)
        gesture.move(x = 50f, y = 20f)

        // Then
        assertThat(gesture.isAbandoned).isTrue()
        assertThat(gesture.lift()).isNull()
    }

    @Test
    fun `a slide past the ends of the line stops at its first or last cell`() {
        // Given
        val left = gesture()
        val right = gesture()

        // When
        left.move(x = -30f, y = 5f)
        right.move(x = 300f, y = 5f)

        // Then
        assertThat(left.lift()).isEqualTo(CellGesture.Slide(fromEighth = 2, toEighth = 0))
        assertThat(right.lift()).isEqualTo(CellGesture.Slide(fromEighth = 2, toEighth = 7))
    }
}
