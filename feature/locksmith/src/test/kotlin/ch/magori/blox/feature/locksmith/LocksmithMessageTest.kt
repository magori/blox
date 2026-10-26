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
    private val start = LocksmithUiState(level = 1, game = PadlockGame.start(Padlock.generate(level = 1, seed = 7)))

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
    fun `the padlock of the bar is closed until the padlock opens`() {
        assertThat(padlockStatusOf(start)).isEqualTo(R.string.feature_locksmith_status_closed)
        assertThat(padlockStatusOf(start.copy(game = start.game.solved())))
            .isEqualTo(R.string.feature_locksmith_status_open)
    }

    @Test
    fun `an open padlock says so, whatever came before`() {
        // Given: every piece placed where the solution has it
        val open = start.game.solved()

        // Then
        assertThat(open.isOpen).isTrue
        assertThat(
            messageOf(start.copy(game = open, aim = Position(3, 3))),
        ).isEqualTo(R.string.feature_locksmith_message_open)
    }
}
