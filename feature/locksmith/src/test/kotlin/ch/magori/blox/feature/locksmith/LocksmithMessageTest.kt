package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.Refusal
import ch.magori.blox.core.locksmith.solved
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The message under the padlock: what the last action told, or how to play. */
class LocksmithMessageTest {
    private val start = LocksmithUiState(level = 1, game = PadlockGame.start(Padlock.ofLevel(1)))

    @Test
    fun `without a preview the message says to touch the hole`() {
        assertThat(messageOf(start)).isEqualTo(R.string.feature_locksmith_message_aim)
    }

    @Test
    fun `with a preview the message says to touch it again or to place it`() {
        assertThat(messageOf(start.copy(aim = Position(3, 3)))).isEqualTo(R.string.feature_locksmith_message_preview)
    }

    @Test
    fun `a refusal tells its reason`() {
        // Given
        val outOfHole = start.copy(events = listOf(PadlockEvent.PlacementRefused(0, Refusal.OUT_OF_HOLE)))
        val overlap = start.copy(events = listOf(PadlockEvent.PlacementRefused(0, Refusal.OVERLAP)))

        // Then
        assertThat(messageOf(outOfHole)).isEqualTo(R.string.feature_locksmith_message_out_of_hole)
        assertThat(messageOf(overlap)).isEqualTo(R.string.feature_locksmith_message_overlap)
    }

    @Test
    fun `a refusal is told with its number, so that TalkBack tells a second refusal in a row again`() {
        // Given a piece aimed half on the metal, refused twice in a row
        val aim = Position(1, 1)
        val first = start.game.place(aim)
        val second = first.game.place(aim)

        // When
        val told = listOf(first, second).mapIndexed { move, update ->
            refusalNumberOf(start.copy(game = update.game, events = update.events, move = move + 1, aim = aim))
        }

        // Then the same reason, told under two numbers; none without a refusal
        assertThat(
            messageOf(start.copy(events = first.events)),
        ).isEqualTo(messageOf(start.copy(events = second.events)))
        assertThat(told).containsExactly(1, 2)
        assertThat(refusalNumberOf(start)).isNull()
    }

    @Test
    fun `a refusal is told before the hint and the preview`() {
        // Given
        val state = start.copy(
            events = listOf(PadlockEvent.PlacementRefused(0, Refusal.OVERLAP)),
            hint = Hint.Blocking(0),
            aim = Position(3, 3),
        )

        // Then
        assertThat(messageOf(state)).isEqualTo(R.string.feature_locksmith_message_overlap)
    }

    @Test
    fun `a turn tells the orientation of the chosen piece, before the hint and the preview`() {
        // Given the piece just turned, a hint shown and the piece aimed
        val state = start.copy(turned = true, hint = Hint.Blocking(0), aim = Position(3, 3))

        // Then: TalkBack hears the new orientation from the message, the one region it announces
        assertThat(messageOf(state)).isEqualTo(R.string.feature_locksmith_message_turned)
    }

    @Test
    fun `a hint is told before the preview`() {
        assertThat(messageOf(start.copy(hint = Hint.Blocking(0), aim = Position(3, 3))))
            .isEqualTo(R.string.feature_locksmith_message_blocking)
    }

    @Test
    fun `a hint tells where a piece fits, or that a placed piece blocks`() {
        // Given
        val fit = start.copy(hint = Hint.Fit(0, start.game.padlock.solution.first()))
        val blocking = start.copy(hint = Hint.Blocking(0))

        // Then
        assertThat(messageOf(fit)).isEqualTo(R.string.feature_locksmith_message_hint)
        assertThat(messageOf(blocking)).isEqualTo(R.string.feature_locksmith_message_blocking)
    }

    @Test
    fun `a hint names its piece, the one that fits or the placed one that blocks`() {
        // Given
        val piece = start.game.padlock.solution.first { it.type == start.game.given.last() }
        val fit = start.copy(hint = Hint.Fit(start.game.given.lastIndex, piece))
        val blocking = start.copy(hint = Hint.Blocking(0))

        // Then
        assertThat(hintedPieceOf(fit)).isEqualTo(start.game.given.last())
        assertThat(hintedPieceOf(blocking)).isEqualTo(start.game.given.first())
        assertThat(hintedPieceOf(start)).isNull()
    }

    @Test
    fun `the title of the bar tells the padlock closed until it opens`() {
        // Then: the padlock drawn beside it says nothing to TalkBack, the title tells its state
        assertThat(barTitleOf(start)).isEqualTo(R.string.feature_locksmith_title_closed)
        assertThat(barTitleOf(start.copy(game = start.game.solved())))
            .isEqualTo(R.string.feature_locksmith_title_open)
    }

    @Test
    fun `an open padlock tells the cadence that closed the phrase, whatever came before`() {
        // Given: every piece placed where the solution has it
        val open = start.game.solved()

        // Then: the card alone says « Serrure ouverte ! »
        assertThat(open.isOpen).isTrue
        assertThat(
            messageOf(start.copy(game = open, aim = Position(3, 3))),
        ).isEqualTo(R.string.feature_locksmith_message_cadence)
    }
}
