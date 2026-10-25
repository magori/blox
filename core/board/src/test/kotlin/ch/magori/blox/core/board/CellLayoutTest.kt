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

    private companion object {
        val DENSITY = Density(2f)
        val DAY_GLOSS = BloxGlossSets.ColorfulLight.cell
        val NIGHT_GLOSS = BloxGlossSets.ColorfulDark.cell
    }
}
