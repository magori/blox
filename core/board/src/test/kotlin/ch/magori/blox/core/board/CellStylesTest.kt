package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CellStylesTest {
    @ParameterizedTest(name = "{0}: fill {1}, border {2}, centre {3}")
    @CsvSource(
        "I, 34503A, 34503A, 34503A",
        "O, 87A67F, 34503A, 34503A",
        "T, EEF5EA, 34503A, 34503A",
        "S, 87A67F, 87A67F, 87A67F",
        "Z, 34503A, 34503A, BFD6B6",
        "J, BFD6B6, 34503A, 87A67F",
        "L, 87A67F, 34503A, EEF5EA",
    )
    fun `in the Original theme a piece has the fill, border and centre of docs themes md, framed by a joint`(
        type: PieceType,
        fill: String,
        border: String,
        centre: String,
    ) {
        // When
        val style = cellStyle(BloxThemeVariant.ORIGINAL, Cell.Filled(type))

        // Then: on a 24 px cell, a joint of 1 px on each edge, 2 px between two cells, so that two flat cells
        // side by side never merge; it takes the colour under the cell (CellPainter). Inside it, a 2 px border
        // and an 8 px centre square
        assertThat(style).isEqualTo(
            CellStyle(
                fill = rgb(fill),
                border = CellBorder.Plain(rgb(border), width = 2f / 24),
                centre = CellCentre(rgb(centre), size = 8f / 24),
                joint = CellJoint(width = 1f / 24),
            ),
        )
    }

    @ParameterizedTest(name = "{0}: fill {1}, light bevel {2}, dark bevel {3}")
    @CsvSource(
        "I, 3FC5E8, 9BE6F6, 1D8FAE",
        "O, F2C641, FAE08F, B38A17",
        "T, A467E0, CFA6F1, 6E36A8",
        "S, 5BC46A, A3E3AB, 2F8A3C",
        "Z, E5534B, F29A94, A32A24",
        "J, 3F7EE8, 93B8F4, 1F4FA6",
        "L, EF8D3B, F6C18F, AD5A14",
    )
    fun `in the Colorful theme a piece has the fill and bevels of docs themes md`(
        type: PieceType,
        fill: String,
        light: String,
        dark: String,
    ) {
        // When
        val styles = COLORFUL_VARIANTS.map { cellStyle(it, Cell.Filled(type)) }

        // Then: a 3 px bevel on a 24 px cell, day and night, until the pieces take the colours of the design system
        assertThat(styles).containsOnly(
            CellStyle(
                fill = rgb(fill),
                border = CellBorder.Bevel(light = rgb(light), dark = rgb(dark), width = 3f / 24),
                centre = null,
            ),
        )
    }

    @Test
    fun `in the Original theme an empty cell is the board background without border`() {
        // When
        val style = cellStyle(BloxThemeVariant.ORIGINAL, Cell.Empty)

        // Then
        assertThat(style).isEqualTo(CellStyle(fill = rgb("DCEBD5"), border = null, centre = null))
    }

    @ParameterizedTest(name = "{0}: empty cell {1} on the board {2}")
    @CsvSource(
        "COLORFUL_LIGHT, F1F2F8, FFFFFF",
        "COLORFUL_DARK, 14151E, 0E0F16",
    )
    fun `in the Colorful theme an empty cell has a 1 px border`(
        variant: BloxThemeVariant,
        emptyCell: String,
        board: String,
    ) {
        // When
        val style = cellStyle(variant, Cell.Empty)

        // Then: in the colour of the empty cells, framed by 1 px of the board on a 24 px cell, as a margin
        assertThat(style).isEqualTo(
            CellStyle(fill = rgb(emptyCell), border = CellBorder.Plain(rgb(board), width = 1f / 24), centre = null),
        )
    }

    private fun rgb(hex: String): Color = Color(0xFF000000 or hex.toLong(radix = 16))

    private companion object {
        val COLORFUL_VARIANTS = listOf(BloxThemeVariant.COLORFUL_LIGHT, BloxThemeVariant.COLORFUL_DARK)
    }
}
