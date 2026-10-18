package ch.magori.blox.feature.game.topbar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PauseGlyphTest {
    @Test
    fun `the pause glyph is two upright bars, a quarter of the width each, with a quarter between them`() {
        val bars = pauseBars(Size(width = 24f, height = 24f))

        assertThat(bars).containsExactly(
            Rect(left = 3f, top = 3f, right = 9f, bottom = 21f),
            Rect(left = 15f, top = 3f, right = 21f, bottom = 21f),
        )
    }

    @Test
    fun `the bars keep their proportions in a wider space`() {
        val bars = pauseBars(Size(width = 48f, height = 24f))

        assertThat(bars).containsExactly(
            Rect(left = 6f, top = 3f, right = 18f, bottom = 21f),
            Rect(left = 30f, top = 3f, right = 42f, bottom = 21f),
        )
    }

    @Test
    fun `the bars have whole-pixel edges, so the glyph is sharp`() {
        val bars = pauseBars(Size(width = 66f, height = 66f))

        bars.flatMap { listOf(it.left, it.top, it.right, it.bottom) }.forEach { edge ->
            assertThat(edge).isEqualTo(Math.round(edge).toFloat())
        }
    }
}
