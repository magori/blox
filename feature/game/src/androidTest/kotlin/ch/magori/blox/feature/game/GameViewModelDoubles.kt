package ch.magori.blox.feature.game

import ch.magori.blox.core.audio.MusicPlayer
import ch.magori.blox.core.audio.Playback
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.RecordResult
import ch.magori.blox.core.data.ThemeChoice
import ch.magori.blox.core.data.UserPreferences
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.engine.ClassicGameEngineFactory
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.music.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/*
 * Test doubles shared by the instrumented tests that build the game view model through its factory.
 */

/** The factory of the game view model, with [engineFactory], no record, the default settings and no music. */
internal fun silentGameViewModelFactory(
    engineFactory: GameEngineFactory = ClassicGameEngineFactory,
): GameViewModel.Factory = GameViewModel.Factory(engineFactory, NoRecords, DefaultSettings, Silence) { null }

private object NoRecords : HighScoreRepository {
    override val highScore: Flow<Int> = flowOf(0)

    override suspend fun submit(score: Int): RecordResult = RecordResult(previous = 0, isNewRecord = false)
}

private object DefaultSettings : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences.DEFAULT)

    override suspend fun setTheme(theme: ThemeChoice) = Unit

    override suspend fun setMusic(music: MusicChoice) = Unit

    override suspend fun setStartLevel(startLevel: Int) = Unit
}

/** No score is ever given to it: the factory answers none for every music choice. */
private object Silence : MusicPlayer {
    override fun play(score: Score): Playback = error("No music is played in these tests")

    override fun setTempoFactor(factor: Double) = Unit

    override fun prepareTempo(factor: Double) = Unit
}
