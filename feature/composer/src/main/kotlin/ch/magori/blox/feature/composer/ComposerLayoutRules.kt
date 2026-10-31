package ch.magori.blox.feature.composer

/*
 * The decisions of the layouts of the editor (FixedBesideRest, ColumnOrRow, GridBetween), in whole pixels, as the
 * layout counts them: pure, so that they are tested apart from Compose.
 */

/**
 * Whether a part that [restNeeds] pixels stands beside a fixed one [fixedWidth] wide (as wide as the line at most), on a
 * line [width] wide, [gap] between them.
 */
internal fun fitsBesideFixed(
    width: Int,
    gap: Int,
    fixedWidth: Int,
    restNeeds: Int,
): Boolean = width - fixedWidth.coerceAtMost(width) - gap >= restNeeds

/** Whether [count] parts, the widest [widest] wide, stand side by side on a line [width] wide, [gap] between them. */
internal fun sideBySide(
    width: Int,
    gap: Int,
    count: Int,
    widest: Int,
): Boolean = (width - gap * (count - 1)) / count >= widest

/**
 * The height of the grid: the room left on the [page], its height inside its padding (null when it has no bound), once
 * the other parts and their gaps have [taken] theirs; [gridMin] at least.
 */
internal fun gridHeight(
    page: Int?,
    taken: Int,
    gridMin: Int,
): Int = maxOf(if (page == null) 0 else page - taken, gridMin)
