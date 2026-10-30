package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.data.OpeningOutcome
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockResult
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

    @Test
    fun `the card shows the best opening kept of the padlock, when it is not this one`() {
        // Given padlock 1 opened before without a hint, now opened with one
        val game = start.game.hint().game.solved()
        val best = PadlockResult(pieces = game.given.size, moves = game.given.size, refusals = 0, hints = 0)

        // When
        val card = openCardOf(start.copy(game = game, best = best, outcome = OpeningOutcome.NOTHING_NEW))

        // Then
        assertThat(card?.best).isEqualTo(best)
    }

    @Test
    fun `the card leaves the best opening out when it is this one`() {
        // Given padlock 1 opened, its opening now the best kept
        val game = start.game.solved()

        // When
        val card = openCardOf(start.copy(game = game, best = game.result))

        // Then
        assertThat(card?.best).isNull()
    }

    @Test
    fun `a new best bears the sticker NOUVEAU MEILLEUR`() {
        // Given padlock 1 opened with a hint, better than its best
        val game = start.game.hint().game.solved()

        // When
        val card = openCardOf(start.copy(game = game, outcome = OpeningOutcome.NEW_BEST))

        // Then
        assertThat(card?.sticker).isEqualTo(OpenSticker.NEW_BEST)
    }

    @Test
    fun `a perfect opening bears the sticker PARFAITE, over a new best, whatever the progress said`() {
        // Given padlock 1 opened without taking back, refusal or hint
        val game = start.game.solved()

        // When
        val stickers = listOf(null, OpeningOutcome.FIRST_OPENING, OpeningOutcome.NEW_BEST, OpeningOutcome.NOTHING_NEW)
            .map { outcome -> openCardOf(start.copy(game = game, outcome = outcome))?.sticker }

        // Then
        assertThat(stickers).containsOnly(OpenSticker.PERFECT)
    }

    @Test
    fun `a first opening, an opening no better or one not recorded bears no sticker, unless it is perfect`() {
        // Given padlock 1 opened with a hint
        val game = start.game.hint().game.solved()

        // When
        val stickers = listOf(null, OpeningOutcome.FIRST_OPENING, OpeningOutcome.NOTHING_NEW)
            .map { outcome -> openCardOf(start.copy(game = game, outcome = outcome))?.sticker }

        // Then
        assertThat(stickers).containsOnlyNulls()
    }

    @Test
    fun `after replaying a padlock whose next one is open, the card offers to resume at the frontier`() {
        // Given padlock 1 opened again, while padlocks 1 to 4 are open
        val game = start.game.solved()

        // When
        val card = openCardOf(start.copy(game = game, frontier = 5))

        // Then
        assertThat(card?.resumeAt).isEqualTo(5)
    }

    @Test
    fun `no resume when the next padlock is the frontier, its own button, or when the frontier is not known`() {
        // Given padlock 3 opened: its next one, padlock 4, is the frontier before or after this opening is recorded
        val game = PadlockGame.start(Padlock.ofLevel(3)).solved()
        val opened = LocksmithUiState(level = 3, game = game)

        // When
        val resumes = listOf(3, 4, null).map { frontier -> openCardOf(opened.copy(frontier = frontier))?.resumeAt }

        // Then
        assertThat(resumes).containsOnlyNulls()
    }

    @Test
    fun `an opening that could not be recorded is told on the card`() {
        // Given padlock 1 opened, its opening not written
        val game = start.game.solved()

        // When
        val card = openCardOf(start.copy(game = game, unrecorded = true))

        // Then
        assertThat(card?.unrecorded).isTrue
        assertThat(openCardOf(start.copy(game = game))?.unrecorded).isFalse
    }

    @Test
    fun `the sticker bounces in, a little past its size, and rests at it once motion-sticker is over`() {
        // When
        val scales = (0..BloxMotion.sticker).map { tick -> stickerScale(tick, BloxEffects.Full) }

        // Then
        assertThat(scales.first()).isZero
        assertThat(scales.max()).isGreaterThan(1f)
        assertThat(scales.last()).isEqualTo(1f)
        assertThat(stickerScale(Int.MAX_VALUE, BloxEffects.Full)).isEqualTo(1f)
    }

    @Test
    fun `in reduced effects the sticker is fixed, at its size from its first frame`() {
        // Given
        val reduced = BloxEffects(reduced = true, forcedBySystem = false)

        // When
        val scales = (0..BloxMotion.sticker).map { tick -> stickerScale(tick, reduced) }

        // Then
        assertThat(scales).containsOnly(1f)
    }

    @Test
    fun `the best opening waits for the progress to tell what the opening was, so that it does not flicker`() {
        // Given padlock 1 opened with a hint, its best kept without one, the opening being written
        val game = start.game.hint().game.solved()
        val best = PadlockResult(pieces = game.given.size, moves = game.given.size, refusals = 0, hints = 0)
        val writing = start.copy(game = game, best = best)

        // When
        val cards = listOf(writing, writing.copy(unrecorded = true)).map { openCardOf(it)?.best }

        // Then
        assertThat(cards).containsExactly(null, best)
    }
}
