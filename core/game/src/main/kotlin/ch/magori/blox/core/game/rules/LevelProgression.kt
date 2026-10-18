package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines

/** Level progression policy: the current level, given the starting level and the lines cleared so far. */
public interface LevelProgression {
    /**
     * Level reached in a game started at [start] once [lines] lines have been cleared in total.
     *
     * @throws IllegalArgumentException if [start] is not a starting level the mode accepts.
     */
    public fun levelFor(
        start: Level,
        lines: Lines,
    ): Level
}
