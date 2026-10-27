package ch.magori.blox.feature.game

import ch.magori.blox.core.data.DailyChallengeRepository
import ch.magori.blox.core.data.HighScoreRepository
import ch.magori.blox.core.data.MusicChoice
import ch.magori.blox.core.data.UserPreferencesRepository
import ch.magori.blox.core.game.engine.ClassicRules
import ch.magori.blox.core.game.engine.GameEngine
import ch.magori.blox.core.game.engine.GameEngineFactory
import ch.magori.blox.core.game.rules.ClassicRandomizer
import ch.magori.blox.core.music.Score
import ch.magori.blox.feature.game.music.GameMusic
import ch.magori.blox.feature.game.navigation.GameVariant
import ch.magori.blox.feature.game.results.ClassicGameResults
import ch.magori.blox.feature.game.results.DailyGameResults
import ch.magori.blox.feature.game.results.GameResults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

/**
 * What a game takes from its variant (ADR 0026): the engines that deal its pieces, where its results go, what its screen
 * says of it ([labels]), and the music it plays (null for none).
 */
internal class GameSetup(
    val engines: GameEngineFactory,
    val results: GameResults,
    val labels: GameLabels,
    val music: suspend () -> GameMusic?,
)

/**
 * The setup of each variant: the only place where the view model of a game depends on its variant (ADR 0026).
 *
 * The Classic deals its pieces from [classicEngines], submits its score to [highScores], and plays the music of the
 * player chosen for the games ([gameMusic]), else the one chosen on the home screen ([musicOf], read in the
 * [preferences]). The daily game deals the pieces of its seed from [dailyEngines] ([StableClassicEngines] in the
 * application), and writes its tries to [daily], one write at a time, in order ([dailyWrites], shared by every daily
 * game).
 */
internal class GameVariants(
    private val classicEngines: GameEngineFactory,
    private val highScores: HighScoreRepository,
    private val preferences: UserPreferencesRepository,
    private val musicOf: suspend (MusicChoice) -> GameMusic?,
    private val gameMusic: suspend () -> Score?,
    private val daily: DailyChallengeRepository,
    private val dailyWrites: Mutex,
    private val dailyEngines: GameEngineFactory,
) {
    fun setupOf(variant: GameVariant): GameSetup = when (variant) {
        GameVariant.Classic -> GameSetup(
            classicEngines,
            ClassicGameResults(highScores),
            GameLabels.Classic,
            ::classicMusic,
        )

        // The live music, whatever the music chosen, on the home screen or for the games.
        is GameVariant.Daily -> GameSetup(
            dailyEngines,
            DailyGameResults(daily, variant.date, dailyWrites),
            GameLabels.Daily,
        ) {
            GameMusic.Live
        }
    }

    /**
     * The music of the player chosen for the games (« Mes musiques »), which plays instead of the one chosen on the
     * home screen, even when that one is none; the settings are read only without it.
     */
    private suspend fun classicMusic(): GameMusic? =
        gameMusic()?.let(GameMusic::Fixed) ?: musicOf(preferences.preferences.first().music)
}

/**
 * Engines of the daily game: the Classic rules, and pieces drawn from a [ClassicRandomizer.stable] of the seed, the same
 * from one version of the application to the next (ADR 0026).
 */
internal object StableClassicEngines : GameEngineFactory {
    override fun create(seed: Long): GameEngine = GameEngine(ClassicRules, ClassicRandomizer.stable(seed))
}
