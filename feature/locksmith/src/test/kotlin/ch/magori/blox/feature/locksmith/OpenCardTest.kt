package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.placing
import ch.magori.blox.core.locksmith.solved
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The card « Serrure ouverte », in place of the tiles and the controls once the padlock is open. */
class OpenCardTest {
    private val start = LocksmithUiState(level = 1, game = PadlockGame.start(Padlock.ofLevel(1)))

    @Test
    fun `there is no card while the padlock is closed`() {
        assertThat(openCardOf(start)).isNull()
    }

    @Test
    fun `an open padlock shows its moves, its refusals and its hints, each one its own counter`() {
        // Given: a hint asked, two placements refused on the metal corner, the first piece of the solution placed and
        // taken back, then every piece placed: one accepted placement more than the given pieces
        val first = start.game.padlock.solution.first()
        val tried = start.game.hint().game
            .place(Position(0, 0)).game
            .place(Position(0, 0)).game
            .placing(first)
        val game = tried.takeBack(tried.rankAt(first.cells.first())!!).game.solved()

        // When
        val card = openCardOf(start.copy(game = game))

        // Then
        assertThat(card).isEqualTo(OpenCard(moves = game.given.size + 1, refusals = 2, hints = 1))
    }
}
