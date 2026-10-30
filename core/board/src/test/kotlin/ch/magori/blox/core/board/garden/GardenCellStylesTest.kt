package ch.magori.blox.core.board.garden

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import ch.magori.blox.core.board.CellCross
import ch.magori.blox.core.board.CellDot
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.contrast
import ch.magori.blox.core.designsystem.theme.variantPalette
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GardenCellStylesTest {
    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a free cell is the empty cell of the board`(variant: BloxThemeVariant) {
        // When
        val style = gardenCellStyle(variant, GardenCell.Free, inBand = false)

        // Then
        assertThat(style).isEqualTo(cellStyle(variant, Cell.Empty))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a free cell of a band has its own colour, apart from the band as the empty cell from the board`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val colors = gardenColors(variant)
        val empty = variantPalette(variant).emptyCell

        // When
        val style = gardenCellStyle(variant, GardenCell.Free, inBand = true)

        // Then the empty cell in garden-band-cell: by day, the empty cell has the colour of the band, and would not show
        // on it. Darker than the band by day, lighter by night, as the empty cell on the board
        assertThat(style).isEqualTo(cellStyle(variant, Cell.Empty).copy(fill = colors.bandCell))
        assertThat(style.fill).isNotEqualTo(colors.band)
        val cellIsDarker = style.fill.luminance() < colors.band.luminance()
        val emptyIsDarker = empty.luminance() < colors.board.luminance()
        assertThat(cellIsDarker).isEqualTo(emptyIsDarker)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a note is a bubble of its piece, as on the board, in a band or not`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            for (inBand in listOf(false, true)) {
                // When
                val style = gardenCellStyle(variant, GardenCell.Note(type), inBand)

                // Then
                assertThat(style).`as`("$type").isEqualTo(cellStyle(variant, Cell.Filled(type)))
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a note of the chord bears a dot of 6 dp in a cell of 22 dp, on a halo of 2 dp more`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val colors = gardenColors(variant)

        for (type in PieceType.entries) {
            // When
            val style = gardenCellStyle(variant, GardenCell.Note(type, inChord = true), inBand = false)

            // Then the note, bearing a dot in garden-chord-dot on a halo of garden-chord-halo
            assertThat(style).`as`("$type").isEqualTo(
                gardenCellStyle(variant, GardenCell.Note(type), inBand = false).copy(
                    dot = CellDot(
                        color = colors.chordDot,
                        halo = colors.chordHalo,
                        size = 6f / 22,
                        haloSize = 10f / 22,
                    ),
                ),
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the dot of the chord reads on every piece, by its ink or by its halo, and on no free cell`(
        variant: BloxThemeVariant,
    ) {
        for (type in PieceType.entries) {
            // Given
            val style = gardenCellStyle(variant, GardenCell.Note(type, inChord = true), inBand = false)
            val dot = requireNotNull(style.dot)

            // Then the ink stands out from its halo, and the ink or the halo from the bottom and the top of the bubble,
            // by 3:1 (WCAG 1.4.11): the chord does not rest on colour
            assertThat(contrast(dot.color, dot.halo)).isGreaterThanOrEqualTo(3.0)
            for (under in listOfNotNull(style.fill, style.bubble?.highlight)) {
                val readable = maxOf(contrast(dot.color, under), contrast(dot.halo, under))
                assertThat(readable).`as`("$type on $under").isGreaterThanOrEqualTo(3.0)
            }
        }
        assertThat(gardenCellStyle(variant, GardenCell.Free, inBand = false).dot).isNull()
        assertThat(gardenCellStyle(variant, GardenCell.Free, inBand = true).dot).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a preview that fits is flat and outlined in ink of 2 dp in a cell of 22 dp`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            // Given the note once placed, in relief, without outline
            val note = gardenCellStyle(variant, GardenCell.Note(type), inBand = false)

            // When
            val style = gardenCellStyle(variant, GardenCell.Preview(type, fits = true), inBand = false)

            // Then the preview is flat, without the gloss of the cells, faded, and outlined in ink, 2 dp of 22 at least,
            // standing out from the empty cell and from the free cell of a band by 3:1 (WCAG 1.4.11)
            assertThat(note.bubble?.gloss).isNotEqualTo(Gloss.None)
            assertThat(style.bubble?.gloss).`as`("$type").isEqualTo(Gloss.None)
            assertThat(style.fill).isNotEqualTo(note.fill)
            val outline = requireNotNull(style.border) { "$type" }
            assertThat(outline.color).isEqualTo(gardenColors(variant).ink)
            assertThat(outline.width * 22).isGreaterThanOrEqualTo(2f)
            for (under in listOf(variantPalette(variant).emptyCell, gardenColors(variant).bandCell)) {
                assertThat(contrast(outline.color, under)).isGreaterThanOrEqualTo(3.0)
            }
            assertThat(style.cross).isNull()
            assertThat(style.dot).isNull()
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a refused preview is crossed out in ink of 2 dp in a cell of 22 dp, on a halo of the page`(
        variant: BloxThemeVariant,
    ) {
        for (type in PieceType.entries) {
            // When
            val style = gardenCellStyle(variant, GardenCell.Preview(type, fits = false), inBand = false)

            // Then the preview that fits, under a cross in ink, 2 dp of 22 at least, on a halo of the page
            val colors = gardenColors(variant)
            assertThat(style).`as`("$type").isEqualTo(
                gardenCellStyle(variant, GardenCell.Preview(type, fits = true), inBand = false).copy(
                    cross = CellCross(color = colors.ink, halo = colors.page, width = 2f / 22),
                ),
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the cross of a refused preview reads on every piece, by its ink or by its halo`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            // Given
            val style = gardenCellStyle(variant, GardenCell.Preview(type, fits = false), inBand = false)
            val cross = requireNotNull(style.cross)

            // Then the ink or the halo stands out from the bottom and the top of the preview by 3:1 (WCAG 1.4.11)
            assertThat(contrast(cross.color, cross.halo)).isGreaterThanOrEqualTo(3.0)
            for (under in listOfNotNull(style.fill, style.bubble?.highlight)) {
                val readable = maxOf(contrast(cross.color, under), contrast(cross.halo, under))
                assertThat(readable).`as`("$type on $under").isGreaterThanOrEqualTo(3.0)
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the play head is framed in ink of 2 dp, the frame standing out from the tinted cells by 3 to 1`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val colors = gardenColors(variant)

        // When
        val look = gardenPlayHeadLook(variant)

        // Then the tint of garden-play-head, and a frame in ink, which bears the play head, not the tint: it stands out
        // from the tinted board, band and free cells by 3:1 (WCAG 1.4.11)
        assertThat(look.tint).isEqualTo(colors.playHead)
        assertThat(look.frame).isEqualTo(colors.ink)
        assertThat(look.frameWidth * 22).isEqualTo(2f)
        val under = listOf(colors.board, colors.band, variantPalette(variant).emptyCell, colors.bandCell)
        for (color in under) {
            assertThat(contrast(look.frame, look.tint.compositeOver(color))).`as`("$color").isGreaterThanOrEqualTo(3.0)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the frame of the play head stands out from the untinted cells beside the column by 3 to 1`(
        variant: BloxThemeVariant,
    ) {
        // Given
        val colors = gardenColors(variant)

        // When
        val look = gardenPlayHeadLook(variant)

        // Then on its outer side, the board, the band and the free cells beside the column, untinted (WCAG 1.4.11)
        val beside = listOf(colors.board, colors.band, variantPalette(variant).emptyCell, colors.bandCell)
        for (color in beside) {
            assertThat(contrast(look.frame, color)).`as`("$color").isGreaterThanOrEqualTo(3.0)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a preview in a band fades over the free cell of the band`(variant: BloxThemeVariant) {
        for (type in PieceType.entries) {
            for (fits in listOf(true, false)) {
                // When
                val style = gardenCellStyle(variant, GardenCell.Preview(type, fits), inBand = true)

                // Then the preview out of a band, its colours faded over garden-band-cell rather than empty-cell
                val freeInBand = gardenCellStyle(variant, GardenCell.Free, inBand = true).fill
                val note = cellStyle(variant, Cell.Filled(type))
                val fade = { color: Color -> color.copy(alpha = 0.6f).compositeOver(freeInBand) }
                assertThat(style).`as`("$type").isEqualTo(
                    gardenCellStyle(variant, GardenCell.Preview(type, fits), inBand = false).copy(
                        fill = fade(note.fill),
                        bubble = style.bubble?.copy(highlight = note.bubble?.highlight?.let(fade)),
                    ),
                )
            }
        }
    }

    @Test
    fun `the garden has no style in the Original theme, it is drawn in the Colorful theme only`() {
        // Then
        assertThatIllegalArgumentException().isThrownBy {
            gardenCellStyle(BloxThemeVariant.ORIGINAL, GardenCell.Free, inBand = false)
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `the band the screen draws behind a group is garden band`(variant: BloxThemeVariant) {
        assertThat(gardenBand(variant)).isEqualTo(gardenColors(variant).band)
    }

    @Test
    fun `a group of 4 eighths out of two lies in a band, the second first`() {
        // When
        val inBand = (0..15).map(::isInGardenBand)

        // Then eighths 1 to 4 out of a band, 5 to 8 in one, 9 to 12 out, 13 to 16 in
        assertThat(inBand).containsExactlyElementsOf(
            List(4) { false } + List(4) { true } + List(4) { false } + List(4) { true },
        )
    }
}
