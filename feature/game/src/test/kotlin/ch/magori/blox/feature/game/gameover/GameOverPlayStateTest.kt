package ch.magori.blox.feature.game.gameover

import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Points
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameOverPlayStateTest {
    @Test
    fun `a game that ends starts its game over, from its whole stack`() {
        // Given a game under way
        val state = GameOverPlayState(over = false)

        // When it ends, then 2 ticks pass
        state.follow(GAME_OVER, BloxEffects.Full)
        val started = state.play
        state.advance(ticks = 2)

        // Then its game over plays from its first tick, the bottom row out 2 ticks later
        assertThat(state.play).isEqualTo(GameOverPlay(tick = 2, reduced = false, newBest = true))
        assertThat(started?.tick).isZero()
    }

    @Test
    fun `a game shown already over shows its card and its whole score at once, without confetti, and plays nothing`() {
        // Given a game shown already over, as after a recreation of the activity
        val state = GameOverPlayState(over = true)

        // When the screen learns its game over
        state.follow(GAME_OVER, BloxEffects.Full)

        // Then
        val play = checkNotNull(state.play)
        assertThat(play.cardShown).isTrue()
        assertThat(play.shownScore(GAME_OVER.score)).isEqualTo(GAME_OVER.score)
        assertThat((0 until play.confettiCount).mapNotNull(play::confetto)).isEmpty()
        assertThat(play.playing).isFalse()
    }

    private companion object {
        val GAME_OVER = GameOverUiState(
            score = Points(12_340),
            lines = Lines(52),
            level = Level(5),
            fourLineClears = 1,
            ticksPlayed = 3_600,
            newBest = true,
            resultsKept = true,
            previousBest = 0,
            practice = false,
        )
    }
}
