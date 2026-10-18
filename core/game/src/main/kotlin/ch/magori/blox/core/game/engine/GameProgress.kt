package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points

/**
 * How far the player has gone in a game. Immutable: only the engine makes it progress.
 *
 * @property level the current level, computed from the start level of the game and [lines].
 * @property lines the lines cleared since the start.
 * @property score the points earned since the start.
 * @property statistics the statistics of the game: the four-line clears so far.
 */
@ConsistentCopyVisibility
public data class GameProgress internal constructor(
    public val level: Level,
    public val lines: Lines,
    public val score: Points,
    public val statistics: GameStatistics,
) {
    internal companion object {
        /** The progress of a game that starts at [startLevel]: no line, no point. */
        fun startingAt(startLevel: Level): GameProgress = GameProgress(
            level = startLevel,
            lines = Lines(0),
            score = Points(0),
            statistics = GameStatistics(),
        )
    }

    /** With [points] more, such as the points of a soft drop. */
    internal fun withPoints(points: Points): GameProgress = copy(score = score + points)

    /**
     * After the lines of [lineClear] are cleared, in a game started at [startLevel]: the lines go up, the
     * level is computed again, and the points are counted at the new level, with the [rules].
     */
    internal fun afterClear(
        lineClear: LineClear,
        startLevel: Level,
        rules: Rules,
    ): GameProgress {
        val lines = lines + lineClear
        val level = rules.levelProgression.levelFor(startLevel, lines)
        return GameProgress(
            level = level,
            lines = lines,
            score = score + rules.scoringRule.pointsForLinesCleared(lineClear, level),
            statistics = statistics.afterClear(lineClear),
        )
    }
}
