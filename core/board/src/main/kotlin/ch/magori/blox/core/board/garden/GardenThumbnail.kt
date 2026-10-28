package ch.magori.blox.core.board.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import ch.magori.blox.core.board.roundToPixel
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood

/**
 * The thumbnail of [garden] in a row of the list « Mes jardins » (docs/jardin-musical.md): its grid of 16 × 14 cells
 * of `garden-thumb-cell` ([GardenSizes.thumbnailCell]), 48 × 42 dp, flat, without relief nor margin, in the colours
 * of the cells of the grid: a note in the colour of its piece, a free cell in that of a free cell, in a band or not.
 * Decorative: TalkBack does not read it, the row says what it shows. In [variant], which must be a Colorful variant,
 * as the garden: an Original variant fails at once, at composition.
 */
@Composable
fun GardenThumbnail(
    garden: Garden,
    modifier: Modifier = Modifier,
    variant: BloxThemeVariant = BloxTheme.variant,
) {
    require(variant.kind == BloxThemeKind.COLORFUL) {
        "The thumbnail of a garden is drawn in the Colorful theme only, not in $variant: the screen must impose it."
    }
    Spacer(
        modifier = modifier
            .size(gardenThumbnailSize)
            .semantics { hideFromAccessibility() }
            .drawWithCache {
                val rects = gardenThumbnailRects(
                    garden = garden,
                    variant = variant,
                    width = size.width.roundToPixel(),
                    height = size.height.roundToPixel(),
                )
                onDrawBehind {
                    // An index rather than an iterator: nothing is allocated while drawing.
                    for (index in rects.indices) {
                        val rect = rects[index]
                        drawRect(
                            color = rect.colour,
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.right - rect.left, rect.bottom - rect.top),
                        )
                    }
                }
            },
    )
}

/** The size of a thumbnail: the grid of the garden, a cell of [GardenSizes.thumbnailCell] per eighth and per degree. */
internal val gardenThumbnailSize: DpSize =
    DpSize(GardenSizes.thumbnailCell * Garden.COLUMNS, GardenSizes.thumbnailCell * Garden.ROWS)

/** A flat rectangle of the thumbnail, from [left] to [right] and from [top] to [bottom] in pixels, in [colour]. */
internal data class ThumbnailRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val colour: Color,
)

/**
 * What the thumbnail of [garden] draws over [width] × [height] pixels in [variant], a Colorful variant, in order: the
 * free cells of each group of 4 eighths in one rectangle, in or out of a band, then a cell per note over them.
 */
internal fun gardenThumbnailRects(
    garden: Garden,
    variant: BloxThemeVariant,
    width: Float,
    height: Float,
): List<ThumbnailRect> {
    val columns = thumbnailEdges(Garden.COLUMNS, width)
    val rows = thumbnailEdges(Garden.ROWS, height)
    val group = GardenMood.EIGHTHS_PER_CHORD
    val free = (0 until Garden.COLUMNS / group).map { index ->
        val first = index * group
        val colour = gardenCellStyle(variant, GardenCell.Free, inBand = isInGardenBand(first)).fill
        ThumbnailRect(columns[first], 0f, columns[first + group], height, colour)
    }
    val notes = garden.pieces.flatMap { piece ->
        val colour = gardenCellStyle(variant, GardenCell.Note(piece.type), inBand = false).fill
        piece.cells.map { (column, row) ->
            ThumbnailRect(columns[column], rows[row], columns[column + 1], rows[row + 1], colour)
        }
    }
    return free + notes
}

/**
 * The edges of [count] cells side by side over [extent] pixels, from 0 to [extent], each on the nearest whole pixel:
 * the flat cells meet without a blurred seam, within a pixel of one another in size.
 */
internal fun thumbnailEdges(
    count: Int,
    extent: Float,
): List<Float> = List(count + 1) { (it * extent / count).roundToPixel() }

/** A garden of every piece, low and high, in a band or not. */
private val previewGarden: Garden = Garden.parse(
    """
    ..........OO....
    ..TTT.....OO....
    ...T............
    .......SS.......
    ......SS....J...
    ............JJJ.
    IIII............
    ................
    ....L...ZZ......
    ..LLL....ZZ.....
    ...........T....
    .....2.....TT.L.
    1111.22....T..L.
    ......2.......LL
    """,
)

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render this preview.

/** The thumbnail of a garden of every piece, then that of an empty garden, on a row of a list (`panel`). */
@Preview
@Composable
internal fun GardenThumbnailsPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        Row(
            modifier = Modifier
                .background(BloxTheme.palette.panel)
                .padding(BloxSpaces.space3),
            horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
        ) {
            GardenThumbnail(previewGarden)
            GardenThumbnail(Garden.EMPTY)
        }
    }
}
