package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.LevelProgression

/**
 * How far the player has gone in a game. Immutable: only the engine makes it progress.
 *
 * Its level is never given: it is computed by the level progression from [startLevel] and [lines], so it is
 * always the level a game started at [startLevel] reaches with those lines. A level below [startLevel] is
 * refused.
 *
 * @property startLevel the level chosen at the start of the game.
 * @property level the current level, computed from [startLevel] and [lines].
 * @property lines the lines cleared since the start.
 * @property score the points earned since the start.
 * @property statistics the statistics of the game: the four-line clears so far.
 */
@ConsistentCopyVisibility
public data class GameProgress private constructor(
    public val startLevel: Level,
    public val level: Level,
    public val lines: Lines,
    public val score: Points,
    public val statistics: GameStatistics,
) {
    init {
        require(level.value >= startLevel.value) {
            "The level cannot be below the start level ${startLevel.value}: ${level.value}"
        }
    }

    internal companion object {
        /** The progress of a game that starts at [startLevel]: no line, no point. */
        fun startingAt(
            startLevel: Level,
            levelProgression: LevelProgression,
        ): GameProgress = of(startLevel, Lines(0), levelProgression)

        /**
         * The progress of a game started at [startLevel], once [lines] are cleared, with [score] and
         * [statistics]: its level is the one [levelProgression] gives.
         */
        fun of(
            startLevel: Level,
            lines: Lines,
            levelProgression: LevelProgression,
            score: Points = Points(0),
            statistics: GameStatistics = GameStatistics(),
        ): GameProgress = GameProgress(
            startLevel = startLevel,
            level = levelProgression.levelFor(startLevel, lines),
            lines = lines,
            score = score,
            statistics = statistics,
        )
    }

    /** With [points] more, such as the points of a soft drop. */
    internal fun withPoints(points: Points): GameProgress = copy(score = score + points)

    /**
     * After the lines of [lineClear] are cleared: the lines go up, the level is computed again, and the
     * points are counted at the new level, with the [rules].
     *
     * @throws IllegalArgumentException if the level progression of the [rules] gives a lower level than now.
     */
    internal fun afterClear(
        lineClear: LineClear,
        rules: Rules,
    ): GameProgress {
        val cleared = of(
            startLevel = startLevel,
            lines = lines + lineClear,
            levelProgression = rules.levelProgression,
            score = score,
            statistics = statistics.afterClear(lineClear),
        )
        require(cleared.level.value >= level.value) {
            "The level cannot go down after a clear: from ${level.value} to ${cleared.level.value}"
        }
        return cleared.withPoints(rules.scoringRule.pointsForLinesCleared(lineClear, cleared.level))
    }
}
