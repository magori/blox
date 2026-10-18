package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level

/** Scoring policy: the points earned by the player's actions. */
public interface ScoringRule {
    /**
     * Points for [linesCleared] lines cleared at once at [level].
     *
     * @throws IllegalArgumentException if [linesCleared] is not between 0 and 4.
     */
    public fun pointsForLinesCleared(
        linesCleared: Int,
        level: Level,
    ): Int

    /**
     * Points for a soft drop. Called once, when the piece locks, with the number of rows that piece
     * dropped in a row while down was held.
     *
     * @throws IllegalArgumentException if [rowsDropped] is negative.
     */
    public fun pointsForSoftDrop(rowsDropped: Int): Int
}
