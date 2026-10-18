package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.rules.ClassicRandomizer

/** Creates the engine of one game. The same seed deals the same pieces. */
public fun interface GameEngineFactory {
    public fun create(seed: Long): GameEngine
}

/** Engines of the Classic mode: [ClassicRules] and a [ClassicRandomizer] built from the seed. */
public object ClassicGameEngineFactory : GameEngineFactory {
    override fun create(seed: Long): GameEngine = GameEngine(ClassicRules, ClassicRandomizer(seed))
}
