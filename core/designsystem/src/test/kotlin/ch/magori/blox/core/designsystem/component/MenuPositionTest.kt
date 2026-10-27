package ch.magori.blox.core.designsystem.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MenuPositionTest {
    @Test
    fun `a menu opens under its anchor, its end at the end of the anchor`() {
        // Given « … » at the top right of a window of 1080 × 2400
        val anchor = IntRect(left = 940, top = 100, right = 1080, bottom = 240)

        // When
        val position = menuPosition(anchor, WINDOW, PANEL)

        // Then
        assertThat(position).isEqualTo(IntOffset(x = 1080 - 600, y = 240))
    }

    @Test
    fun `a menu without room under its anchor opens above it`() {
        // Given « … » at the bottom of the window
        val anchor = IntRect(left = 940, top = 2200, right = 1080, bottom = 2340)

        // When
        val position = menuPosition(anchor, WINDOW, PANEL)

        // Then
        assertThat(position).isEqualTo(IntOffset(x = 1080 - 600, y = 2200 - 800))
    }

    @Test
    fun `a menu stays within the window`() {
        // Given « … » at the left of the window, in a window too low for the menu both under and above it
        val anchor = IntRect(left = 0, top = 300, right = 140, bottom = 440)
        val window = IntSize(width = 1080, height = 1000)

        // When
        val position = menuPosition(anchor, window, PANEL)

        // Then against the left edge, and as low as the window lets it, its bottom on the bottom of the window
        assertThat(position).isEqualTo(IntOffset(x = 0, y = 1000 - 800))
    }

    companion object {
        private val WINDOW = IntSize(width = 1080, height = 2400)
        private val PANEL = IntSize(width = 600, height = 800)
    }
}
