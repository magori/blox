package ch.magori.blox.feature.game

import ch.magori.blox.core.data.FakeDailyChallengeRepository
import ch.magori.blox.core.data.FakeHighScoreRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.feature.game.navigation.GameVariant
import kotlinx.coroutines.sync.Mutex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** What each variant of the Classic says on the game screen (ADR 0026): the setup gives it, the screen shows it. */
class GameVariantsTest {
    private val variants = GameVariants(
        classicEngines = StableClassicEngines,
        highScores = FakeHighScoreRepository(),
        preferences = FakeUserPreferencesRepository(),
        musicOf = { null },
        gameMusic = { null },
        daily = FakeDailyChallengeRepository(),
        dailyWrites = Mutex(),
        dailyEngines = StableClassicEngines,
    )

    @Test
    fun `a classic game is titled Mode classique, shows the record and offers no copy`() {
        val labels = variants.setupOf(GameVariant.Classic).labels

        assertThat(labels).isEqualTo(
            GameLabels(
                title = R.string.feature_game_top_title,
                best = R.string.feature_game_top_record,
                newBest = R.string.feature_game_top_new_record,
                gameOverNewBest = R.string.feature_game_over_new_record,
                gameOverPreviousBest = R.string.feature_game_over_previous_record,
                copyOffered = false,
            ),
        )
    }

    @Test
    fun `a daily game is titled Graine du jour, shows the best of the day and offers to copy the score`() {
        val labels = variants.setupOf(GameVariant.Daily(LocalDate.of(2026, 10, 26), id = 1L)).labels

        assertThat(labels).isEqualTo(
            GameLabels(
                title = R.string.feature_game_daily_title,
                best = R.string.feature_game_daily_best,
                newBest = R.string.feature_game_daily_top_new_best,
                gameOverNewBest = R.string.feature_game_daily_new_best,
                gameOverPreviousBest = R.string.feature_game_daily_previous_best,
                copyOffered = true,
            ),
        )
    }
}
