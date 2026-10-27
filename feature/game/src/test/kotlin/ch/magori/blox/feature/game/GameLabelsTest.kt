package ch.magori.blox.feature.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameLabelsTest {
    @Test
    fun `the game over recalls the record as Ancien record once beaten, as Record otherwise`() {
        val labels = GameLabels.Classic

        assertThat(labels.gameOverBest(newBest = true)).isEqualTo(R.string.feature_game_over_previous_record)
        assertThat(labels.gameOverBest(newBest = false)).isEqualTo(R.string.feature_game_top_record)
    }

    @Test
    fun `the game over recalls the best of the day as Ancien meilleur once beaten, as Meilleur otherwise`() {
        val labels = GameLabels.Daily

        assertThat(labels.gameOverBest(newBest = true)).isEqualTo(R.string.feature_game_daily_previous_best)
        assertThat(labels.gameOverBest(newBest = false)).isEqualTo(R.string.feature_game_daily_best)
    }
}
