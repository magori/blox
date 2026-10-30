package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks

/**
 * Drop timing of the Classic mode (NES, 1989): the piece drops when the ticks per row of [gravityTable] have passed.
 * The tick count of the game plays no part: the NES counts the gravity from the appearance of the piece or its last
 * drop (`fallTimer`), never from the start of the game.
 */
public data class ClassicDropTiming(private val gravityTable: GravityTable) : DropTiming {
    override fun dropsAfter(
        ticksSinceDrop: Ticks,
        tickCount: Int,
        level: Level,
    ): Boolean = ticksSinceDrop.value >= gravityTable.ticksPerRow(level).value
}
