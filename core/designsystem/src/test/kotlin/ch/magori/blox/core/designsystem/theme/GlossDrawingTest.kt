package ch.magori.blox.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** How a gloss is drawn: its bands built once for their inputs, inside the frame and around the shape as in CSS. */
class GlossDrawingTest {
    @Test
    fun `two calls with the same gloss, shape and frame give the same modifier, so a recomposition keeps its bands`() {
        // Given
        val gloss = BloxGlossSets.ColorfulLight.button

        // When
        val first = Modifier.gloss(gloss, BloxShapeSets.Colorful.pill, frame = 2.dp)
        val second = Modifier.gloss(gloss, BloxShapeSets.Colorful.pill, frame = 2.dp)

        // Then
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `the bands are built once while their inputs stay the same`() {
        // Given a cache that counts what it builds
        var built = 0
        val cache = GlossCache { built++ }

        // When it is asked three times for the same inputs, as on three frames
        repeat(3) { cache.get(KEY) }

        // Then
        assertThat(built).isEqualTo(1)
    }

    @Test
    fun `the bands are built again when the size, the density, the direction or a value of the gloss changes`() {
        // Given a cache that counts what it builds
        var built = 0
        val cache = GlossCache { built++ }
        cache.get(KEY)

        // When each input changes, one after the other
        listOf(
            KEY.copy(size = Size(120f, 56f)),
            KEY.copy(density = Density(3f)),
            KEY.copy(layoutDirection = LayoutDirection.Rtl),
            KEY.copy(gloss = BloxGlossSets.ColorfulLight.buttonPressed),
            KEY.copy(shape = BloxShapeSets.Colorful.md),
            KEY.copy(frame = 0f),
        ).forEach { key -> cache.get(key) }

        // Then
        assertThat(built).isEqualTo(7)
    }

    @Test
    fun `the inner bands of a rounded shape follow the inside of its frame, their corners narrower by the frame`() {
        // When the inside of a frame of 2 px, at a density of 1
        val md = grownOutline(BloxShapeSets.Colorful.md, Size(100f, 60f), -2f, LayoutDirection.Ltr, Density(1f))
        val pill = grownOutline(BloxShapeSets.Colorful.pill, Size(100f, 60f), -2f, LayoutDirection.Ltr, Density(1f))
        val small = grownOutline(RoundedCornerShape(1.dp), Size(100f, 60f), -2f, LayoutDirection.Ltr, Density(1f))

        // Then corners of 16 - 2, a pill inside a pill, and corners narrower than the frame square
        assertThat((md as Outline.Rounded).roundRect).isEqualTo(rounded(Rect(2f, 2f, 98f, 58f), 14f))
        assertThat((pill as Outline.Rounded).roundRect).isEqualTo(rounded(Rect(2f, 2f, 98f, 58f), 28f))
        assertThat((small as Outline.Rounded).roundRect).isEqualTo(rounded(Rect(2f, 2f, 98f, 58f), 0f))
    }

    @Test
    fun `the ring of a rounded shape grows its corners by its width`() {
        // When a ring of 1 px around a cell of 22 px, at a density of 1
        val ring = grownOutline(BloxShapeSets.Colorful.cell, Size(22f, 22f), 1f, LayoutDirection.Ltr, Density(1f))

        // Then corners of 6 + 1
        assertThat((ring as Outline.Rounded).roundRect).isEqualTo(rounded(Rect(-1f, -1f, 23f, 23f), 7f))
    }

    @Test
    fun `a square shape stays square, inside its frame and around it`() {
        // When
        val inside = grownOutline(RectangleShape, Size(100f, 60f), -2f, LayoutDirection.Ltr, Density(1f))

        // Then
        assertThat((inside as Outline.Rectangle).rect).isEqualTo(Rect(2f, 2f, 98f, 58f))
    }

    private fun rounded(
        rect: Rect,
        radius: Float,
    ) = RoundRect(rect, CornerRadius(radius))

    private companion object {
        val KEY = GlossKey(
            gloss = BloxGlossSets.ColorfulLight.button,
            shape = BloxShapeSets.Colorful.pill,
            frame = 2f,
            size = Size(100f, 56f),
            layoutDirection = LayoutDirection.Ltr,
            density = Density(2f),
        )
    }
}
