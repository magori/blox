package ch.magori.blox.feature.garden

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import ch.magori.blox.core.board.garden.GardenSizes
import ch.magori.blox.core.garden.Garden

/** The frame of the grid on each side: its border of 2 dp, then 1 dp of the board before the cells (V40). */
internal val GRID_FRAME: Dp = 3.dp

/**
 * The cell of the grid in a column [width] wide: `garden-cell`, 22 dp, where the grid and its frame hold, as on a
 * phone of 390 dp; narrower on a narrower screen, so that the 16 columns still fill it at most.
 */
internal fun gridCellFor(width: Dp): Dp = min(GardenSizes.cell, (width - GRID_FRAME * 2) / Garden.COLUMNS)

/** The lines the information of the mood may take beside Tout effacer (V40). */
internal const val INFORMATION_LINES = 2

/**
 * Whether the information of the mood stands beside Tout effacer, in the [room] left by the button, in pixels: while
 * its [longestWord] holds on a line, and its [height] at that width takes [INFORMATION_LINES] lines at most. Its
 * [lineHeight] is its height on one line, trimmed of the leading above and below it: lines run higher than it, and one
 * line more would exceed that many [lineHeight]s. Under the button otherwise, as in a large font, rather than cut a
 * word or run on 4 lines.
 */
internal fun holdsBeside(
    room: Int,
    longestWord: Int,
    height: Int,
    lineHeight: Int,
): Boolean = longestWord <= room && height < lineHeight * (INFORMATION_LINES + 1)
