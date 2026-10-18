package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines

/** Level progression policy: the current level, given the starting level and the lines cleared so far. */
public interface LevelProgression {
    /** The levels a game may start at, in increasing order. The level selection screen offers them. */
    public val startLevels: List<Level>

    /** Rejects [start] if it is not one of the [startLevels]; the message lists them. */
    public fun requireStartLevel(start: Level) {
        require(start in startLevels) {
            "The start level must be one of ${startLevels.map(Level::value)}: ${start.value}"
        }
    }

    /**
     * Level reached in a game started at [start] once [lines] lines have been cleared in total.
     *
     * @throws IllegalArgumentException if [start] is not one of the [startLevels].
     */
    public fun levelFor(
        start: Level,
        lines: Lines,
    ): Level
}
