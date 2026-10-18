package ch.magori.blox.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** A cell of the logo, at [column] and [row] of its 2 × 2 square, in [color]. */
internal data class LogoCell(val column: Int, val row: Int, val color: Color)

/** The cells of the logo of the home screen (V1): a square of four cells, in the logo colours of [palette]. */
internal fun logoCells(palette: BloxPalette): List<LogoCell> = with(palette.logoCells) {
    listOf(
        LogoCell(column = 0, row = 0, color = topLeft),
        LogoCell(column = 1, row = 0, color = topRight),
        LogoCell(column = 0, row = 1, color = bottomLeft),
        LogoCell(column = 1, row = 1, color = bottomRight),
    )
}

/**
 * The logo of four cells, in the colours of the current theme. Plain squares: the cell painter of the
 * board belongs to feature:game, which this feature does not depend on. Decorative: TalkBack skips it.
 */
@Composable
internal fun HomeLogo(
    modifier: Modifier = Modifier,
    cellSize: Dp = 20.dp,
) {
    val cells = logoCells(BloxTheme.palette)
    Canvas(modifier = modifier.size(cellSize * SIDE)) {
        val side = size.width / SIDE
        // A thin gap between the cells, as between the cells of the board.
        val gap = (side / GAP_FRACTION).coerceAtLeast(1f)
        cells.forEach { cell ->
            drawRect(
                color = cell.color,
                topLeft = Offset(cell.column * side + gap / 2, cell.row * side + gap / 2),
                size = Size(side - gap, side - gap),
            )
        }
    }
}

/** The logo is a square of [SIDE] × [SIDE] cells. */
private const val SIDE = 2

/** The gap between two cells, as a fraction of a cell: 1/12. */
private const val GAP_FRACTION = 12f
