package ch.magori.blox.feature.game.gameover

import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameOverPlayTest {
    @Test
    fun `the stack goes out from the bottom, 2 ticks a row`() {
        // Given a game over that starts to play
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false)

        // When / Then: no row out at first, the bottom row after 2 ticks, the next after 4, every row after 40
        assertThat((0 until 20).filter(play::rowOut)).isEmpty()
        assertThat((0 until 20).filter(play.advance(ticks = 1)::rowOut)).isEmpty()
        assertThat((0 until 20).filter(play.advance(ticks = 2)::rowOut)).containsExactly(19)
        assertThat((0 until 20).filter(play.advance(ticks = 4)::rowOut)).containsExactly(18, 19)
        assertThat((0 until 20).filter(play.advance(ticks = 39)::rowOut)).hasSize(19).doesNotContain(0)
        assertThat((0 until 20).filter(play.advance(ticks = 40)::rowOut)).hasSize(20)
    }

    @Test
    fun `the card shows once the whole stack is out`() {
        // Given a game over that starts to play
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false)

        // When / Then: no card until the top row is out
        assertThat(play.cardShown).isFalse()
        assertThat(play.advance(ticks = 39).cardShown).isFalse()
        assertThat(play.advance(ticks = 40).cardShown).isTrue()
    }

    @Test
    fun `the stack goes out over the rows of its board, as many as it has`() {
        // Given a game over of a board of 4 rows that starts to play
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false, rows = 4)

        // When / Then: its 4 rows are out after 8 ticks, and the card shows then
        assertThat((0 until 4).filter(play.advance(ticks = 7)::rowOut)).containsExactly(1, 2, 3)
        assertThat(play.advance(ticks = 7).cardShown).isFalse()
        assertThat(play.advance(ticks = 8).cardShown).isTrue()
    }

    @Test
    fun `the final score counts up from 0 in 30 ticks once the card shows`() {
        // Given a game over of 12 000 points that starts to play
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false)
        val score = Points(12_000)

        // When / Then: 0 until the card shows, half of it 15 ticks later, all of it 30 ticks later and from then on
        assertThat(play.shownScore(score)).isEqualTo(Points(0))
        assertThat(play.advance(ticks = 40).shownScore(score)).isEqualTo(Points(0))
        assertThat(play.advance(ticks = 55).shownScore(score)).isEqualTo(Points(6_000))
        assertThat(play.advance(ticks = 70).shownScore(score)).isEqualTo(score)
        assertThat(play.advance(ticks = 200).shownScore(score)).isEqualTo(score)
    }

    @Test
    fun `in reduced effects the stack goes out at once and the score shows at once`() {
        // Given a game over that starts to play in reduced effects
        val play = GameOverPlay.starting(BloxEffects(reduced = true, forcedBySystem = false), newBest = false)

        // Then every row is out, the card shows and the score is whole, from the first tick
        assertThat((0 until 20).filter(play::rowOut)).hasSize(20)
        assertThat(play.cardShown).isTrue()
        assertThat(play.shownScore(Points(12_000))).isEqualTo(Points(12_000))
    }

    @Test
    fun `the game over needs frames until its score has counted up, none in reduced effects`() {
        // Given a game over that starts to play, and one in reduced effects
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = false)
        val reduced = GameOverPlay.starting(BloxEffects(reduced = true, forcedBySystem = false), newBest = false)

        // Then it plays 40 ticks of stack, then 30 of score; the one in reduced effects does not play
        assertThat(play.playing).isTrue()
        assertThat(play.advance(ticks = 69).playing).isTrue()
        assertThat(play.advance(ticks = 70).playing).isFalse()
        assertThat(reduced.playing).isFalse()
    }

    @Test
    fun `a new best throws 40 confetti in the colours of the pieces once the card shows, for 90 ticks`() {
        // Given the game over of a new best that starts to play
        val play = GameOverPlay.starting(BloxEffects.Full, newBest = true)
        val confetti = { at: GameOverPlay -> (0 until at.confettiCount).mapNotNull(at::confetto) }

        // When / Then: none until the card shows, 40 of every piece as it shows, none 90 ticks later, when it ends
        assertThat(play.confettiCount).isEqualTo(40)
        assertThat(confetti(play.advance(ticks = 39))).isEmpty()
        val thrown = confetti(play.advance(ticks = 40))
        assertThat(thrown).hasSize(40)
        assertThat(thrown.map { it.type }.toSet()).containsExactlyInAnyOrderElementsOf(PieceType.entries)
        assertThat(play.advance(ticks = 129).playing).isTrue()
        assertThat(confetti(play.advance(ticks = 130))).isEmpty()
        assertThat(play.advance(ticks = 130).playing).isFalse()
    }

    @Test
    fun `without a new best, or in reduced effects, no confetti`() {
        // Given a game over without a new best, and one of a new best in reduced effects
        val plain = GameOverPlay.starting(BloxEffects.Full, newBest = false)
        val reduced = GameOverPlay.starting(BloxEffects(reduced = true, forcedBySystem = false), newBest = true)

        // Then neither throws any
        assertThat(plain.confettiCount).isZero()
        assertThat(reduced.confettiCount).isZero()
    }
}
