package ch.magori.blox.feature.locksmith

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces

/** The tiles of the tray: each [tile] wide and high, [perRow] on a row, the last row holding what is left. */
internal data class TrayLayout(val tile: Dp, val perRow: Int)

/**
 * The tray of [count] tiles in a column [width] wide, at [density], [BloxSpaces.space2] apart: on one row, the tiles
 * [MAX_TILE] wide at most, narrower to fill the width but never under a touch target ([BloxSizes.minTouchTarget]);
 * otherwise on two rows, the first one holding the odd tile, 4 tiles on a row at most up to 8 tiles. Each tile is a
 * whole number of pixels, rounded down, so that a row laid out never passes the width.
 */
internal fun trayLayout(
    width: Dp,
    count: Int,
    density: Density,
): TrayLayout {
    val oneRow = TrayLayout(tile = tileOn(width, count, density), perRow = count)
    if (oneRow.tile >= BloxSizes.minTouchTarget) return oneRow
    val perRow = (count + 1) / 2
    return TrayLayout(tile = tileOn(width, perRow, density), perRow = perRow)
}

/**
 * The side of [perRow] tiles that fill [width], [MAX_TILE] at most, in whole pixels at [density], counted as the row lays
 * them out: the width and the gaps rounded to whole pixels as Compose rounds them, the tiles rounded down.
 */
private fun tileOn(
    width: Dp,
    perRow: Int,
    density: Density,
): Dp = with(density) {
    val gaps = BloxSpaces.space2.roundToPx() * (perRow - 1)
    minOf(MAX_TILE.roundToPx(), (width.roundToPx() - gaps) / perRow).toDp()
}

/** The size of a tile where the width allows it, the size `PieceTile` takes of itself. */
private val MAX_TILE = 52.dp
