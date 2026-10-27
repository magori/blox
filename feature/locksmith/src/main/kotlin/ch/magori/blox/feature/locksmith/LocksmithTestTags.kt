package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.board.lock.LockCell
import ch.magori.blox.core.game.model.Position

/*
 * The tags of the screen, the same in every language, read by the tests and by the journey of `:benchmark` (`By.res`,
 * with `testTagsAsResourceId` set by `app`), which copies them as they are: it does not see this module.
 */

/** The tag of the button Place. */
internal const val PLACE_TAG = "locksmith_place"

/**
 * The tag of a cell of the padlock, by what it shows: the journey of `:benchmark` touches the first hole it finds,
 * without knowing the padlock, drawn from a random seed.
 */
internal fun cellTestTag(cell: LockCell): String = when (cell) {
    LockCell.Hole -> "locksmith_cell_hole"
    LockCell.Metal -> "locksmith_cell_metal"
    LockCell.Hint -> "locksmith_cell_hint"
    is LockCell.Placed -> "locksmith_cell_placed"
    is LockCell.Preview -> "locksmith_cell_preview"
}

/** The tag of the tile of the given piece of [rank]. */
internal fun tileTestTag(rank: Int): String = "locksmith_tile_$rank"

/** The tag of a cell of the preview on the ring at [position], read by the tests alone: TalkBack does not read it. */
internal fun rimCellTestTag(position: Position): String = "locksmith_rim_${position.column}_${position.row}"
