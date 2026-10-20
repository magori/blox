package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.rules.ClassicRandomizer
import ch.magori.blox.core.game.rules.GravityTable

/** A gravity of one row per tick, at every level. */
public object OneRowPerTickGravity : GravityTable {
    override fun ticksPerRow(level: Level): Ticks = Ticks(1)
}

/**
 * The rules of the Classic mode, but for the gravity: the piece falls one row per tick, at every level
 * ([OneRowPerTickGravity]). Left to itself, each piece falls straight down and the stack rises in the middle of the
 * board, never filling a row: the game is over within a few hundred ticks, a few seconds of the test clock rather than
 * minutes of play (`OneRowPerTickTest`).
 */
public val OneRowPerTickRules: Rules = ClassicRules.copy(gravityTable = OneRowPerTickGravity)

/** Engines of [OneRowPerTickRules], dealing the pieces of the Classic mode from the seed. */
public val OneRowPerTickGameEngineFactory: GameEngineFactory =
    GameEngineFactory { seed -> GameEngine(OneRowPerTickRules, ClassicRandomizer(seed)) }
