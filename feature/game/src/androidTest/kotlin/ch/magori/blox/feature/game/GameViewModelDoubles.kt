package ch.magori.blox.feature.game

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.haptics.FakeHaptics

/**
 * The factory of the game view model, with [engineFactory], the shared fakes of core:data (no record, the
 * default settings) and no music: the factory answers no music for every music choice, and no music of the
 * player for the games, so the fake player is never asked to play.
 */
internal fun silentGameViewModelFactory(
    engineFactory: GameEngineFactory = ClassicGameEngineFactory,
): GameViewModel.Factory = GameViewModel.Factory(
    engineFactory,
    FakeHighScoreRepository(),
    FakeUserPreferencesRepository(),
    FakeMusicPlayer(),
    FakeHaptics(),
    musicOf = { null },
    gameMusic = { null },
)
