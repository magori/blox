package ch.magori.blox.feature.game

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.haptics.FakeHaptics
import ch.magori.blox.feature.game.music.GameMusic
import kotlinx.coroutines.MainScope

/**
 * The factory of the game view model, with [engineFactory], the shared fakes of core:data (no record, the
 * default settings, vibrations off and sound effects on) and no music: the factory answers [music], none by default, for
 * every music choice, and no music of the player for the games, so the fake player is never asked to play with no
 * [music] (a daily game plays the live music, though). The results of the daily game go to [dailyChallenge].
 */
internal fun silentGameViewModelFactory(
    engineFactory: GameEngineFactory = ClassicGameEngineFactory,
    dailyChallenge: FakeDailyChallengeRepository = FakeDailyChallengeRepository(),
    music: GameMusic? = null,
): GameViewModel.Factory = GameViewModel.Factory(
    engineFactory,
    FakeHighScoreRepository(),
    FakeUserPreferencesRepository(),
    FakeMusicPlayer(),
    FakeHaptics(),
    FakeSoundEffectPlayer(),
    musicOf = { music },
    gameMusic = { null },
    dailyChallenge = dailyChallenge,
    appScope = MainScope(),
)
