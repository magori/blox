package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks

/**
 * Drop timing policy: whether the falling piece drops one row by gravity on this tick (ADR 0028). The policy is
 * interchangeable: [ClassicDropTiming] follows the gravity table of the level.
 */
public interface DropTiming {
    /**
     * Whether the piece drops one row by gravity on the tick [tickCount] of the game (`GameState.tickCount`, the tick
     * being played counted), when [ticksSinceDrop] have passed since it appeared or last dropped, that tick included,
     * at [level].
     */
    public fun dropsAfter(
        ticksSinceDrop: Ticks,
        tickCount: Int,
        level: Level,
    ): Boolean
}
