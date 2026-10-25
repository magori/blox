package ch.magori.blox.core.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CellLayoutTest {
    @ParameterizedTest(name = "cells of {0} px")
    @ValueSource(floats = [4f, 7f, 13f, 24f, 37f, 96f])
    fun `two flat cells side by side in the Original theme are apart by their two joints, of 1 px each at least`(
        side: Float,
    ) {
        // Given two I cells side by side, at x = 0 and x = side: the same dark fill and border
        val layout = cellLayout(cellStyle(BloxThemeVariant.ORIGINAL, Cell.Filled(PieceType.I)), side)
        val first = layout.fill
        val second = layout.fill.translate(Offset(side, 0f))

        // Then the background shows between them, 2 px at least (1 of 24 per cell), and the border stays inside the fill
        assertThat(second.left - first.right).isGreaterThanOrEqualTo(2f)
        val border = requireNotNull(layout.border)
        assertThat(first.intersect(border)).isEqualTo(border)
        assertThat(Rect(0f, 0f, side, side).intersect(first)).isEqualTo(first)
    }
}
