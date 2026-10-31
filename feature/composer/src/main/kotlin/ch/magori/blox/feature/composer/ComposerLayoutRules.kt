package ch.magori.blox.feature.composer

/*
 * The decision of the layout of the editor that is its own (GridBetween), in whole pixels, as the layout counts them:
 * pure, so that it is tested apart from Compose. Those of FixedBesideRest and SideBySideOrStacked, shared, are in
 * core:designsystem (fitsBesideFixed, holdSideBySide).
 */

/**
 * The height of the grid: the room left on the [page], its height inside its padding (null when it has no bound), once
 * the other parts and their gaps have [taken] theirs; [gridMin] at least.
 */
internal fun gridHeight(
    page: Int?,
    taken: Int,
    gridMin: Int,
): Int = maxOf(if (page == null) 0 else page - taken, gridMin)
