package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level

/**
 * Gravity of the Classic mode (NES, 1989): levels 0 to 9 each have their own speed, then
 * levels share a speed by tier, down to one row every tick from level 29 onwards.
 */
public object ClassicGravityTable : GravityTable {
    override fun ticksPerRow(level: Level): Int = when (level.value) {
        0 -> 48
        1 -> 43
        2 -> 38
        3 -> 33
        4 -> 28
        5 -> 23
        6 -> 18
        7 -> 13
        8 -> 8
        9 -> 6
        in 10..12 -> 5
        in 13..15 -> 4
        in 16..18 -> 3
        in 19..28 -> 2
        else -> 1
    }
}
