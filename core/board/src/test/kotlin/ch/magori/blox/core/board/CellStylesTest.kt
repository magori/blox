package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxGlossSets
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

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
                border = CellBorder(rgb(border), width = 2f / 24),
                centre = CellCentre(rgb(centre), size = 8f / 24),
                joint = CellJoint.Fraction(width = 1f / 24),
            ),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `in the Colorful theme a piece is a bubble with the cell gloss, framed by a joint of 1 px, without border`(
        variant: BloxThemeVariant,
    ) {
        // When
        val styles = PieceType.entries.map { cellStyle(variant, Cell.Filled(it)) }

        // Then each piece in its colours (PieceTokensTest), lit from the top, in relief, framed as an empty cell is
        assertThat(styles).allSatisfy { style ->
            assertThat(style.border).isNull()
            assertThat(style.centre).isNull()
            assertThat(style.joint).isEqualTo(CellJoint.Fixed(width = 1.dp))
            assertThat(style.bubble?.highlight).isNotNull()
            assertThat(style.bubble?.gloss).isEqualTo(cellGloss(variant))
        }
    }

    @Test
    fun `in the Original theme an empty cell is the board background without border`() {
        // When
        val style = cellStyle(BloxThemeVariant.ORIGINAL, Cell.Empty)

        // Then
        assertThat(style).isEqualTo(CellStyle(fill = rgb("DCEBD5"), border = null, centre = null))
    }

    @ParameterizedTest(name = "{0}: empty cell {1}")
    @CsvSource(
        "COLORFUL_LIGHT, F1F2F8",
        "COLORFUL_DARK, 14151E",
    )
    fun `in the Colorful theme an empty cell is framed by a joint of 1 px, as a piece is`(
        variant: BloxThemeVariant,
        emptyCell: String,
    ) {
        // When
        val style = cellStyle(variant, Cell.Empty)

        // Then: in the colour of the empty cells, framed by 1 dp (1 px of the design system) in the colour under it, the
        // background the board is drawn on, as a margin; rounded as a piece, flat, without gloss
        assertThat(style).isEqualTo(
            CellStyle(
                fill = rgb(emptyCell),
                border = null,
                centre = null,
                joint = CellJoint.Fixed(width = 1.dp),
                bubble = CellBubble(highlight = null, gloss = Gloss.None),
            ),
        )
    }

    private fun rgb(hex: String): Color = Color(0xFF000000 or hex.toLong(radix = 16))

    /** The gloss of the cells of a Colorful variant, by day or by night. */
    private fun cellGloss(variant: BloxThemeVariant): Gloss = when (variant) {
        BloxThemeVariant.COLORFUL_LIGHT -> BloxGlossSets.ColorfulLight.cell
        else -> BloxGlossSets.ColorfulDark.cell
    }
}
