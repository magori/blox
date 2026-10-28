package ch.magori.blox.core.board.garden

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.board.pieceColor
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.garden.Garden
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class GardenThumbnailTest {
    @Test
    fun `a thumbnail is the grid of the garden, 16 by 14 cells of garden-thumb-cell, 48 by 42 dp`() {
        // When
        val size = gardenThumbnailSize

        // Then
        assertThat(size).isEqualTo(DpSize(48.dp, 42.dp))
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `an empty garden is drawn in 4 rectangles, one per group of 4 eighths, out of a band then in one`(
        variant: BloxThemeVariant,
    ) {
        // When the thumbnail is drawn over 126 × 110 px, its size at a density of 2.625
        val rects = gardenThumbnailRects(Garden.EMPTY, variant, width = WIDTH, height = HEIGHT)

        // Then each group from its first edge to that of the next, on the whole height
        val free = gardenCellStyle(variant, GardenCell.Free, inBand = false).fill
        val band = gardenCellStyle(variant, GardenCell.Free, inBand = true).fill
        val columns = thumbnailEdges(Garden.COLUMNS, WIDTH)
        assertThat(rects).containsExactly(
            ThumbnailRect(columns[0], 0f, columns[4], HEIGHT, free),
            ThumbnailRect(columns[4], 0f, columns[8], HEIGHT, band),
            ThumbnailRect(columns[8], 0f, columns[12], HEIGHT, free),
            ThumbnailRect(columns[12], 0f, columns[16], HEIGHT, band),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `each note is drawn after the free groups, as one cell in the colour of its piece`(variant: BloxThemeVariant) {
        // Given a garden of an O in the corner of the second row
        val garden = Garden.parse(
            """
            ................
            OO..............
            OO..............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        )

        // When
        val rects = gardenThumbnailRects(garden, variant, width = WIDTH, height = HEIGHT)

        // Then the 4 groups, then the 4 cells of the O, each between its edges
        val columns = thumbnailEdges(Garden.COLUMNS, WIDTH)
        val rows = thumbnailEdges(Garden.ROWS, HEIGHT)
        val o = pieceColor(variant, PieceType.O)
        assertThat(rects).hasSize(4 + 4)
        assertThat(rects.drop(4)).containsExactlyInAnyOrder(
            ThumbnailRect(columns[0], rows[1], columns[1], rows[2], o),
            ThumbnailRect(columns[1], rows[1], columns[2], rows[2], o),
            ThumbnailRect(columns[0], rows[2], columns[1], rows[3], o),
            ThumbnailRect(columns[1], rows[2], columns[2], rows[3], o),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a free cell out of a band shows the colour of a free cell of the garden, the empty cell`(
        variant: BloxThemeVariant,
    ) {
        // When
        val colours = shownColours(Garden.EMPTY, variant)

        // Then the first group of 4 eighths, on every row
        val free = cellStyle(variant, Cell.Empty).fill
        for (row in 0 until Garden.ROWS) {
            for (column in 0..3) {
                assertThat(colours[row][column]).isEqualTo(free)
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a free cell of a group of 4 eighths out of two shows the colour of a free cell of a band, as on the grid`(
        variant: BloxThemeVariant,
    ) {
        // When
        val colours = shownColours(Garden.EMPTY, variant)

        // Then eighths 5 to 8 and 13 to 16 lie in a band, the others not
        val free = gardenCellStyle(variant, GardenCell.Free, inBand = false).fill
        val band = gardenCellStyle(variant, GardenCell.Free, inBand = true).fill
        assertThat(colours.last()).containsExactlyElementsOf(
            List(4) { free } + List(4) { band } + List(4) { free } + List(4) { band },
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BloxThemeVariant::class, names = ["COLORFUL_LIGHT", "COLORFUL_DARK"])
    fun `a cell of a piece shows the colour of its piece, as on the board, in a band or not`(
        variant: BloxThemeVariant,
    ) {
        // Given a T across the edge of the first band, and an I in the second
        val garden = Garden.parse(
            """
            ..TTT...........
            ...T............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ............IIII
            """,
        )

        // When
        val colours = shownColours(garden, variant)

        // Then each cell of a piece in the colour of its piece, the cells around them still free
        val t = pieceColor(variant, PieceType.T)
        val i = pieceColor(variant, PieceType.I)
        assertThat(listOf(colours[0][2], colours[0][3], colours[0][4], colours[1][3])).containsOnly(t)
        assertThat(colours[13].subList(12, 16)).containsOnly(i)
        assertThat(colours[1][2]).isEqualTo(cellStyle(variant, Cell.Empty).fill)
        assertThat(colours[13][11]).isEqualTo(cellStyle(variant, Cell.Empty).fill)
        assertThat(colours.flatten().count { it == t }).isEqualTo(4)
        assertThat(colours.flatten().count { it == i }).isEqualTo(4)
    }

    @Test
    fun `a thumbnail is drawn in the Colorful theme only, as the garden`() {
        assertThatIllegalArgumentException().isThrownBy {
            gardenThumbnailRects(Garden.EMPTY, BloxThemeVariant.ORIGINAL, width = WIDTH, height = HEIGHT)
        }
    }

    @ParameterizedTest(name = "{0} cells over {1} px")
    @CsvSource("16, 126", "14, 110", "16, 48", "14, 42", "16, 144")
    fun `the edges of the cells lie on whole pixels from one end to the other, the cells within a pixel in size`(
        count: Int,
        extent: Float,
    ) {
        // When
        val edges = thumbnailEdges(count, extent)

        // Then from one end to the other, each edge on a whole pixel, the cells within a pixel of one another
        assertThat(edges).hasSize(count + 1)
        assertThat(edges.first()).isEqualTo(0f)
        assertThat(edges.last()).isEqualTo(extent)
        assertThat(edges).allMatch { it == it.roundToPixel() }
        val widths = edges.zipWithNext { start, end -> end - start }
        assertThat(widths.max() - widths.min()).isLessThanOrEqualTo(1f)
    }

    private companion object {
        /** The thumbnail at a density of 2.625: 48 × 42 dp. */
        const val WIDTH = 126f
        const val HEIGHT = 110f

        /**
         * The colour shown in the centre of each cell of the thumbnail of [garden], row after row: that of the last
         * rectangle drawn over it.
         */
        fun shownColours(
            garden: Garden,
            variant: BloxThemeVariant,
        ): List<List<Color>> {
            val rects = gardenThumbnailRects(garden, variant, width = WIDTH, height = HEIGHT)
            val columns = thumbnailEdges(Garden.COLUMNS, WIDTH)
            val rows = thumbnailEdges(Garden.ROWS, HEIGHT)
            return List(Garden.ROWS) { row ->
                List(Garden.COLUMNS) { column ->
                    val x = (columns[column] + columns[column + 1]) / 2
                    val y = (rows[row] + rows[row + 1]) / 2
                    rects.last { x in it.left..it.right && y in it.top..it.bottom }.colour
                }
            }
        }
    }
}
