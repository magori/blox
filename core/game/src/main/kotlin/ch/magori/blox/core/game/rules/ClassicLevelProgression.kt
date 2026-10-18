package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines

/**
 * Level progression of the Classic mode (NES, 1989), for the accepted [startLevels].
 *
 * The NES first levels up at `min(start × 10 + 10, max(100, start × 10 − 50))` lines, then every
 * 10 lines. For every accepted start, that first threshold is `(start + 1) × 10`, so the level is
 * the start level until then, and `lines / 10` from then on.
 */
public object ClassicLevelProgression : LevelProgression {
    private val startLevels = 0..9

    /** @throws IllegalArgumentException if [start] is not one of the [startLevels]. */
    override fun levelFor(
        start: Level,
        lines: Lines,
    ): Level {
        require(start.value in startLevels) { "The start level must be in $startLevels: ${start.value}" }
        return Level(maxOf(start.value, lines.value / 10))
    }
}
