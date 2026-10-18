package ch.magori.blox.di

import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.feature.game.GameViewModel

/**
 * Builds what the features need, by hand (ADR 0007). The features define their view model
 * factories; this container builds them with the implementations of the application (ADR 0013).
 */
class AppContainer {
    /** Classic mode: one engine per game, from the seed of the game route (ADR 0014). */
    val gameViewModelFactory: GameViewModel.Factory =
        GameViewModel.Factory(ClassicGameEngineFactory, ClassicRules.rotationSystem)
}
