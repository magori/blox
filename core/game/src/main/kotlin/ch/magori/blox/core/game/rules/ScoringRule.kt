package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Points

/** Scoring policy: the points earned by the player's actions. */
public interface ScoringRule {
    /** Points for the lines cleared at once in [lineClear], at [level]. */
    public fun pointsForLinesCleared(
        lineClear: LineClear,
        level: Level,
    ): Points

    /**
     * Points for a soft drop. Called once, when the piece locks, with the number of rows that piece
     * dropped in a row while down was held.
     *
     * @throws IllegalArgumentException if [rowsDropped] is negative.
     */
    public fun pointsForSoftDrop(rowsDropped: Int): Points
}
