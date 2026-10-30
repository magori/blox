package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks

/**
 * Drop timing policy: whether the falling piece drops one row by gravity on this tick (ADR 0028). The policy is
 * interchangeable: [ClassicDropTiming] follows the gravity table of the level, `TempoDropTiming` (Au tempo) the
 * eighths of a meter.
 */
public interface DropTiming {
    /**
     * Whether the piece drops one row by gravity on the tick [tickCount] of the game (`GameState.tickCount`, the tick
     * being played counted), when [ticksSinceDrop] have passed, that tick included, at [level]. [ticksSinceDrop] starts
     * again when the piece appears, at each drop by gravity and at each soft drop step. [softDropping] tells whether
     * the soft drop is held on this tick, between two of its steps.
     */
    public fun dropsAfter(
        ticksSinceDrop: Ticks,
        tickCount: Int,
        level: Level,
        softDropping: Boolean,
    ): Boolean
}
