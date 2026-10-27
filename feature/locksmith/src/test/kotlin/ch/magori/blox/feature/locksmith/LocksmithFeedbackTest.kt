package ch.magori.blox.feature.locksmith

import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.core.locksmith.turnedTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The animated feedback of the last action of the player, in ticks, full or in reduced effects (ADR 0024). */
class LocksmithFeedbackTest {
    private val start = PadlockGame.start(Padlock.generate(level = 3, seed = 7))

    @Test
    fun `a placement lights the cells of its piece for 8 ticks, dimmer at each tick`() {
        // Given the first piece of the solution placed
        val piece = start.padlock.solution.first()
        val state = shown(place(start, piece))

        // When
        val feedback = feedbackOf(state)!!
        val glows = (0..8).map { tick -> feedback.glow(piece.pivot, tick, FULL) }

        // Then
        assertThat(feedback.kind).isEqualTo(FeedbackKind.PLACED)
        assertThat(feedback.cells).isEqualTo(piece.cells)
        assertThat(feedback.ticks(FULL)).isEqualTo(8)
        assertThat(glows.first()).isEqualTo(1f)
        assertThat(glows).isSortedAccordingTo(reverseOrder())
        assertThat(glows.last()).isZero()
    }

    @Test
    fun `in reduced effects a placement lights nothing, as a lock of the Classic, the pill of its chord telling it`() {
        // Given
        val piece = start.padlock.solution.first()
        val feedback = feedbackOf(shown(place(start, piece)))!!

        // Then
        assertThat(feedback.ticks(REDUCED)).isZero()
        assertThat(feedback.glow(piece.pivot, 0, REDUCED)).isZero()
    }

    @Test
    fun `a refusal shakes the padlock side to side for 18 ticks from its first, less and less, then puts it back`() {
        // Given the first piece refused on the metal
        val state = refused()

        // When
        val feedback = feedbackOf(state)!!
        val shakes = (0..feedback.ticks(FULL)).map { tick -> feedback.shake(tick, FULL) }

        // Then it goes both ways, never beyond its amplitude, and lights nothing
        assertThat(feedback.kind).isEqualTo(FeedbackKind.REFUSED)
        assertThat(feedback.ticks(FULL)).isEqualTo(18)
        assertThat(shakes).allSatisfy { assertThat(it).isBetween(-1f, 1f) }
        assertThat(shakes.max()).isGreaterThan(0.5f)
        assertThat(shakes.min()).isLessThan(-0.5f)
        assertThat(shakes.last()).isZero()
        // Aside from its first tick, the frame the refusal is drawn on
        assertThat(shakes.first()).isNotZero()
        assertThat(feedback.glow(state.aim!!, 0, FULL)).isZero()
    }

    @Test
    fun `a refusal concerns the cells it crosses out alone, those on the ring included`() {
        // Given the first piece refused where it lies partly in the hole; and at the corner, partly on the ring
        val hole = start.padlock.hole
        val aim = INSIDE_CELLS.first { pivot ->
            val cells = start.selectedAt(pivot)!!.cells
            cells.any { it in hole } && cells.any { it !in hole }
        }
        val partly = shown(start.place(aim)).copy(aim = aim)
        val corner = refused()

        // When
        val feedbacks = listOf(partly, corner).map { feedbackOf(it)!!.cells }

        // Then: those off the hole, as the screen crosses them out, the cells that fit left dark
        assertThat(feedbacks.first()).containsExactlyInAnyOrderElementsOf(
            partly.preview!!.cells.filterNot {
                it in hole
            },
        )
        assertThat(feedbacks.last()).anySatisfy { assertThat(it.isInside).isFalse() }
    }

    @Test
    fun `in reduced effects a refusal does not shake, its preview lit and fading in 8 ticks`() {
        // Given
        val state = refused()
        val feedback = feedbackOf(state)!!

        // When
        val shakes = (0..feedback.ticks(REDUCED)).map { tick -> feedback.shake(tick, REDUCED) }
        val glows = (0..feedback.ticks(REDUCED)).map { tick -> feedback.glow(state.aim!!, tick, REDUCED) }

        // Then the fade that takes the place of a move (motion-reduced-fade)
        assertThat(feedback.ticks(REDUCED)).isEqualTo(BloxMotion.reducedFade)
        assertThat(shakes).containsOnly(0f)
        assertThat(glows.first()).isEqualTo(1f)
        assertThat(glows).isSortedAccordingTo(reverseOrder())
        assertThat(glows.last()).isZero()
    }

    @Test
    fun `an open padlock lights its hole row by row from the bottom up, each row 2 ticks after the one below`() {
        // Given every piece placed, the last one opening the padlock
        val state = opened()

        // When
        val feedback = feedbackOf(state)!!
        val hole = start.padlock.hole
        val bottom = hole.maxOf { it.row }
        val lowest = hole.first { it.row == bottom }
        val above = hole.filter { it.row < bottom }

        // Then the lowest row lit at once; each cell above as the lowest one was, 2 ticks per row later
        assertThat(feedback.kind).isEqualTo(FeedbackKind.OPENED)
        assertThat(feedback.cells).isEqualTo(hole)
        assertThat(feedback.glow(lowest, 0, FULL)).isEqualTo(1f)
        assertThat(above).allSatisfy { cell ->
            val delay = 2 * (bottom - cell.row)
            assertThat(feedback.glow(cell, delay - 1, FULL)).isZero()
            assertThat((0..8).map { feedback.glow(cell, delay + it, FULL) })
                .isEqualTo((0..8).map { feedback.glow(lowest, it, FULL) })
        }
        assertThat(hole).allSatisfy { assertThat(feedback.glow(it, feedback.ticks(FULL), FULL)).isZero() }
    }

    @Test
    fun `in reduced effects an open padlock lights its whole hole at once, fading in 8 ticks`() {
        // Given
        val feedback = feedbackOf(opened())!!

        // Then
        assertThat(feedback.ticks(REDUCED)).isEqualTo(BloxMotion.reducedFade)
        assertThat(start.padlock.hole).allSatisfy { cell ->
            assertThat(feedback.glow(cell, 0, REDUCED)).isEqualTo(1f)
            assertThat(feedback.glow(cell, BloxMotion.reducedFade, REDUCED)).isZero()
        }
    }

    @Test
    fun `a hint lights the cells it marks in one swell of 30 ticks, once, so that nothing flashes`() {
        // Given a hint asked at the start: where a piece fits
        val update = start.hint()
        val hint = (update.events.single() as PadlockEvent.HintGiven).hint as Hint.Fit
        val state = shown(update).copy(hint = hint)

        // When
        val feedback = feedbackOf(state)!!
        val glows = (0..feedback.ticks(FULL)).map { tick -> feedback.glow(hint.piece.pivot, tick, FULL) }
        val peak = glows.indexOf(glows.max())

        // Then up to its peak, then down, dark at both ends
        assertThat(feedback.kind).isEqualTo(FeedbackKind.HINTED)
        assertThat(feedback.cells).isEqualTo(hint.piece.cells)
        assertThat(feedback.ticks(FULL)).isEqualTo(BloxMotion.sticker)
        assertThat(glows.take(peak + 1)).isSorted()
        assertThat(glows.drop(peak)).isSortedAccordingTo(reverseOrder())
        assertThat(glows.max()).isEqualTo(1f)
        assertThat(glows.first()).isZero()
        assertThat(glows.last()).isZero()
    }

    @Test
    fun `in reduced effects a hint lights its cells at once, fading in 8 ticks`() {
        // Given
        val state = hinted()
        val pivot = (state.hint as Hint.Fit).piece.pivot

        // When
        val feedback = feedbackOf(state)!!
        val glows = (0..feedback.ticks(REDUCED)).map { tick -> feedback.glow(pivot, tick, REDUCED) }

        // Then
        assertThat(feedback.ticks(REDUCED)).isEqualTo(BloxMotion.reducedFade)
        assertThat(glows.first()).isEqualTo(1f)
        assertThat(glows).isSortedAccordingTo(reverseOrder())
        assertThat(glows.last()).isZero()
    }

    @Test
    fun `a hint naming a placed piece that blocks lights that piece`() {
        // Given a placed piece that blocks, named by the hint
        val placed = place(start, start.padlock.solution.first()).game
        val state = LocksmithUiState(
            level = 3,
            game = placed,
            events = listOf(PadlockEvent.HintGiven(Hint.Blocking(placed.placementOrder.single()))),
            move = 2,
            hint = Hint.Blocking(placed.placementOrder.single()),
        )

        // Then
        assertThat(feedbackOf(state)!!.cells).isEqualTo(start.padlock.solution.first().cells)
    }

    @Test
    fun `taking a piece back, choosing, turning or aiming shows no feedback`() {
        // Given a piece placed, then taken back; and the start, nothing judged
        val placed = place(start, start.padlock.solution.first()).game
        val takenBack = placed.takeBack(placed.placementOrder.single())

        // Then
        assertThat(feedbackOf(shown(takenBack))).isNull()
        assertThat(feedbackOf(LocksmithUiState(level = 3, game = start, aim = Position(3, 3)))).isNull()
    }

    @Test
    fun `two refusals in a row are two feedbacks, each one played from its start`() {
        // Given
        val first = refused()
        val second = first.copy(move = 2)

        // Then
        assertThat(feedbackOf(second)).isNotEqualTo(feedbackOf(first))
    }

    /** A hint asked at the start, where a piece fits: what the screen shows then. */
    private fun hinted(): LocksmithUiState {
        val update = start.hint()
        return shown(update).copy(hint = (update.events.single() as PadlockEvent.HintGiven).hint)
    }

    /** Every piece of the solution placed, the last one opening the padlock: what the screen shows then. */
    private fun opened(): LocksmithUiState {
        val solution = start.padlock.solution
        val before = solution.dropLast(1).fold(start) { game, piece -> place(game, piece).game }
        return shown(place(before, solution.last()))
    }

    /** The selected piece aimed by its pivot on a corner of the 8 × 8, half on the metal, and refused there. */
    private fun refused(): LocksmithUiState {
        val aim = Position(1, 1)
        return shown(start.place(aim)).copy(aim = aim)
    }

    /** [piece] placed in [game]: a free given piece of its type selected, turned as it and put down on its pivot. */
    private fun place(
        game: PadlockGame,
        piece: Piece,
    ): PadlockUpdate {
        val rank = game.given.indices.first { it !in game.placed && game.given[it] == piece.type }
        return game.select(rank).turnedTo(piece.orientation).place(piece.pivot)
    }

    /** What the screen shows after [update], the first move. */
    private fun shown(update: PadlockUpdate): LocksmithUiState =
        LocksmithUiState(level = 3, game = update.game, events = update.events, move = 1)

    private companion object {
        val FULL = BloxEffects.Full
        val REDUCED = BloxEffects(reduced = true, forcedBySystem = false)
    }
}
