package ch.magori.blox.feature.locksmith

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.locksmith.PadlockResult
import ch.magori.blox.core.locksmith.PadlockTier

/**
 * An item of the grid of the screen « Serrures »: a header, or the cell of a padlock and what it shows. Immutable for
 * Compose, its tiles skipped when their item does not change: every value of an item is a value object.
 */
@Immutable
internal sealed interface PadlocksItem {
    /** The header of [tier], over its padlocks. */
    data class TierHeader(val tier: PadlockTier) : PadlocksItem

    /** The cell of padlock [level]. */
    data class Cell(
        val level: Int,
        val state: CellState,
        val best: PadlockResult? = null,
        val current: Boolean = false,
    ) : PadlocksItem
}

/** The state of a cell. */
internal enum class CellState { OPEN, PERFECT, FRONTIER, LOCKED }

/** The grid. */
internal fun padlocksGridOf(
    openings: List<PadlockResult>,
    current: Int?,
    columns: Int,
): List<PadlocksItem> = buildList {
    val frontier = openings.size + 1
    var inTier = 0
    (1..frontier).forEach { level ->
        val tier = PadlockTier.of(level)
        if (level == 1 || PadlockTier.of(level - 1) != tier) {
            add(PadlocksItem.TierHeader(tier))
            inTier = 0
        }
        val best = openings.getOrNull(level - 1)
        val state = when {
            best == null -> CellState.FRONTIER
            best.isPerfect -> CellState.PERFECT
            else -> CellState.OPEN
        }
        add(PadlocksItem.Cell(level, state, best, current = level == current))
        inTier++
    }
    (frontier + 1..frontier + (columns - inTier % columns) % columns)
        .takeWhile { PadlockTier.of(it) == PadlockTier.of(frontier) }
        .forEach { level -> add(PadlocksItem.Cell(level, CellState.LOCKED)) }
}

/** Where an item lies in a grid: its [row], its first [column], and how many columns it spans, [span]. */
internal data class GridPlace(val row: Int, val column: Int, val span: Int)

/**
 * Where items of [spans] lie in a grid of [columns], as the lazy grid lays them out: from the left of a row, an item
 * that no longer fits beginning the next one. A grid holds one column at least, an item spans 1 to [columns].
 */
internal fun gridPlacesOf(
    spans: List<Int>,
    columns: Int,
): List<GridPlace> {
    require(columns >= 1) { "A grid holds one column at least, not $columns" }
    var row = 0
    var column = 0
    return spans.map { span ->
        require(span in 1..columns) { "An item spans 1 to $columns columns, not $span" }
        if (column + span > columns) {
            row++
            column = 0
        }
        GridPlace(row, column, span).also { column += span }
    }
}

/**
 * How many columns of tiles the grid holds in [width]: as many tiles of [TILE] as fit, [GAP] between them, wider in a
 * large font, [TILE_PER_FONT] per unit of [fontScale], for the words of a tile; one at least.
 */
internal fun padlockColumns(
    width: Dp,
    fontScale: Float,
): Int {
    val tile = maxOf(TILE, TILE_PER_FONT * fontScale)
    return ((width + GAP) / (tile + GAP)).toInt().coerceAtLeast(1)
}

/** The narrowest tile of the grid, the 80 dp aimed at. */
internal val TILE = 80.dp

/** The narrowest tile, per unit of the scale of the font: 136 dp in a font twice as large. */
private val TILE_PER_FONT = 68.dp

/** The room between two tiles, and between two rows. */
internal val GAP = BloxSpaces.space2

/** How the header of a tier tells the [pieces] of its padlocks: one count, two counts apart, or a range. */
@StringRes
internal fun piecesTextOf(pieces: IntRange): Int = when (pieces.last - pieces.first) {
    0 -> R.string.feature_locksmith_padlocks_tier_pieces
    1 -> R.string.feature_locksmith_padlocks_tier_pieces_or
    else -> R.string.feature_locksmith_padlocks_tier_pieces_range
}
