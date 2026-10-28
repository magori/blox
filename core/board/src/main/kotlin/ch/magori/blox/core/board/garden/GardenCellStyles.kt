package ch.magori.blox.core.board.garden

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellDot
import ch.magori.blox.core.board.CellStyle
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.board.previewStyle
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell

/**
 * The style of [cell] of the garden in [variant], a Colorful variant, [inBand] when it lies in a group of 4 eighths on
 * `garden-band`: a free cell is the empty cell of the board, in `garden-band-cell` in a band, where the empty cell would
 * not show by day; a note is the bubble of its piece on the board, a dot on it when it is of the chord; a preview is
 * that of the Locksmith, faded over the free cell where it lies, its outline and its cross 2 dp wide in a cell of 22 dp.
 */
internal fun gardenCellStyle(
    variant: BloxThemeVariant,
    cell: GardenCell,
    inBand: Boolean,
): CellStyle {
    val colors = gardenColors(variant)
    val empty = cellStyle(variant, Cell.Empty)
    return when (cell) {
        GardenCell.Free -> if (inBand) empty.copy(fill = colors.bandCell) else empty

        is GardenCell.Note -> cellStyle(variant, Cell.Filled(cell.type)).let { note ->
            if (cell.inChord) note.copy(dot = chordDot(colors)) else note
        }

        is GardenCell.Preview -> previewStyle(
            placed = cellStyle(variant, Cell.Filled(cell.type)),
            under = gardenCellStyle(variant, GardenCell.Free, inBand).fill,
            ink = colors.ink,
            page = colors.page,
            outlineWidth = MARK_WIDTH,
            crossWidth = MARK_WIDTH,
            fits = cell.fits,
        )
    }
}

/**
 * The band the screen draws behind a group of 4 eighths out of two (`garden-band`), in [variant], a Colorful variant:
 * the colour under the cells of the group, their joint included.
 */
fun gardenBand(variant: BloxThemeVariant): Color = gardenColors(variant).band

/**
 * How the play head is drawn over its column: a [tint] over the cells, a [frame] around the column, [frameWidth] a
 * fraction of the cell size.
 */
internal data class GardenPlayHeadLook(val tint: Color, val frame: Color, val frameWidth: Float)

/**
 * The play head in [variant]: the column tinted in `garden-play-head`, framed in ink; the frame bears it, not the tint.
 * It stands out by 3:1 from the cells beside the column, untinted, and from the free cells inside, tinted; not from the
 * light notes of the night inside the column, which its outer side and the tint mark well enough.
 */
internal fun gardenPlayHeadLook(variant: BloxThemeVariant): GardenPlayHeadLook =
    gardenColors(variant).let { GardenPlayHeadLook(tint = it.playHead, frame = it.ink, frameWidth = PLAY_HEAD_FRAME) }

/** The dot of a note of the chord, `garden-chord-dot-size` in a cell of `garden-cell`, on a halo of 2 dp more. */
private fun chordDot(colors: GardenColors): CellDot = CellDot(
    color = colors.chordDot,
    halo = colors.chordHalo,
    size = GardenSizes.chordDot / GardenSizes.cell,
    haloSize = (GardenSizes.chordDot + CHORD_HALO * 2) / GardenSizes.cell,
)

/** The halo around the dot of a note of the chord, on each side. */
private val CHORD_HALO = 2.dp

/** The outline of a preview and the cross of a refusal: 2 dp of a cell of 22 dp, so that they read at that size. */
private const val MARK_WIDTH = 2f / 22

/** The frame of the play head: 2 dp of a cell of 22 dp, as the marks of a preview. */
private const val PLAY_HEAD_FRAME = 2f / 22
