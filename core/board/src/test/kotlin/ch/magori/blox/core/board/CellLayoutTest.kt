package ch.magori.blox.core.board

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxGlossSets
import ch.magori.blox.core.designsystem.theme.BloxShapeSets
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.GlossLayer
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class CellLayoutTest {
    @ParameterizedTest(name = "cells of {0} px")
    @ValueSource(floats = [4f, 7f, 13f, 24f, 37f, 96f])
    fun `two flat cells side by side in the Original theme are apart by their two joints, of 1 px each at least`(
        side: Float,
    ) {
        // Given two I cells side by side, at x = 0 and x = side: the same dark fill and border
        val layout = cellLayout(cellStyle(BloxThemeVariant.ORIGINAL, Cell.Filled(PieceType.I)), side, DENSITY)
        val first = layout.fill
        val second = layout.fill.translate(Offset(side, 0f))

        // Then the background shows between them, 2 px at least (1 of 24 per cell), and the border stays inside the fill
        assertThat(second.left - first.right).isGreaterThanOrEqualTo(2f)
        val border = requireNotNull(layout.border)
        assertThat(first.intersect(border)).isEqualTo(border)
        assertThat(Rect(0f, 0f, side, side).intersect(first)).isEqualTo(first)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a cell under 16 dp has 2 dp corners and no gloss`(variant: BloxThemeVariant) {
        // Given a cell of a piece of 31 px at a density of 2: 15,5 dp
        val style = cellStyle(variant, Cell.Filled(PieceType.T))

        // When
        val layout = cellLayout(style, side = 31f, DENSITY)

        // Then: a gloss would weigh too much on so small a cell
        assertThat(layout.shape).isEqualTo(RoundedCornerShape(2.dp))
        assertThat(layout.gloss).isEqualTo(Gloss.None)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a cell of 16 dp and more has the corners of radius cell and the gloss of cell gloss`(
        variant: BloxThemeVariant,
    ) {
        // Given a cell of a piece of 32 px at a density of 2: 16 dp
        val style = cellStyle(variant, Cell.Filled(PieceType.T))

        // When
        val layout = cellLayout(style, side = 32f, DENSITY)

        // Then
        assertThat(layout.shape).isEqualTo(BloxShapeSets.Colorful.cell)
        assertThat(layout.gloss).isEqualTo(
            if (variant == BloxThemeVariant.COLORFUL_LIGHT) DAY_GLOSS else NIGHT_GLOSS,
        )
    }

    @ParameterizedTest(name = "cells of {0} px")
    @ValueSource(floats = [13f, 31f, 32f, 48f, 96f])
    fun `an empty cell has the corners and the margin of a piece, without gloss`(side: Float) {
        // Given, by day and by night
        listOf(BloxThemeVariant.COLORFUL_LIGHT, BloxThemeVariant.COLORFUL_DARK).forEach { variant ->
            // When
            val empty = cellLayout(cellStyle(variant, Cell.Empty), side, DENSITY)
            val piece = cellLayout(cellStyle(variant, Cell.Filled(PieceType.I)), side, DENSITY)

            // Then
            assertThat(empty.fill).isEqualTo(piece.fill)
            assertThat(empty.shape).isEqualTo(piece.shape)
            assertThat(empty.gloss).isEqualTo(Gloss.None)
        }
    }

    @ParameterizedTest(name = "cells of {0} px")
    @ValueSource(floats = [32f, 96f])
    fun `a bubble has a margin of 1 dp whatever its size`(side: Float) {
        // Given a cell of a piece at a density of 2: 16 dp, then 48 dp
        val style = cellStyle(BloxThemeVariant.COLORFUL_LIGHT, Cell.Filled(PieceType.T))

        // When
        val layout = cellLayout(style, side, DENSITY)

        // Then 1 dp of the design system on each edge, 2 px, as the 1 px of the design system
        assertThat(layout.fill).isEqualTo(Rect(2f, 2f, side - 2f, side - 2f))
    }

    @ParameterizedTest(name = "density {0}")
    @ValueSource(floats = [0.75f, 1f, 1.33f, 2f, 2.625f, 3f, 3.5f])
    fun `the ring of the gloss fits in the margin`(density: Float) {
        // Given a cell of a piece of 24 dp by day, whose gloss has a ring around the bubble
        val style = cellStyle(BloxThemeVariant.COLORFUL_LIGHT, Cell.Filled(PieceType.T))

        // When
        val layout = cellLayout(style, side = 24 * density, Density(density))

        // Then the ring stays in the margin, never cut at the edge of the cell
        val ring = layout.gloss.layers.filterIsInstance<GlossLayer.Ring>().single()
        assertThat(ring.width.value * density).isLessThanOrEqualTo(layout.fill.left)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a cell cut to a narrower width keeps its height, its joint and its corners, its fill as narrow`(
        variant: BloxThemeVariant,
    ) {
        // Given a cell of a piece of 84 px at a density of 2, 42 dp, and the same cut to 46 px, 23 dp
        val style = cellStyle(variant, Cell.Filled(PieceType.T))
        val whole = cellLayout(style, side = 84f, DENSITY)

        // When
        val cut = cellLayout(style, side = 84f, DENSITY, width = 46f)

        // Then
        assertThat(cut.fill).isEqualTo(whole.fill.copy(right = 46f - whole.fill.left))
        assertThat(cut.border).isEqualTo(whole.border?.let { cut.fill })
        assertThat(cut.shape).isEqualTo(whole.shape)
    }

    private companion object {
        val DENSITY = Density(2f)
        val DAY_GLOSS = BloxGlossSets.ColorfulLight.cell
        val NIGHT_GLOSS = BloxGlossSets.ColorfulDark.cell
    }
}
