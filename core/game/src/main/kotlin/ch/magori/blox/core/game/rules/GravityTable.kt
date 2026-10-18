package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level

/** Gravity policy: how many ticks the falling piece waits before dropping one row. */
public interface GravityTable {
    public fun ticksPerRow(level: Level): Int
}
