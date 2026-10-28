package ch.magori.blox.feature.garden

import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenPlacement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The animated feedback of the changes of the garden, in ticks, full or in reduced effects (docs, Retours; ADR 0024). */
class GardenFeedbackTest {
    @Test
    fun `a placement lights the bubbles of its piece for 8 ticks, dimmer at each tick`() {
        // Given a T placed
        val state = placed(T_AT_5_6)

        // When
        val feedback = gardenFeedbackOf(state)!!
        val lights = (0..8).map { tick -> feedback.light(T_AT_5_6.pivot, tick, FULL) }

        // Then
        assertThat(feedback.ticks(FULL)).isEqualTo(8)
        assertThat(lights.first()).isEqualTo(1f)
        assertThat(lights).isSortedAccordingTo(reverseOrder())
        assertThat(lights.last()).isZero()
        assertThat(feedback.light(Position(0, 0), 0, FULL)).isZero()
    }

    @Test
    fun `in reduced effects a placement lights its piece the same, a fade without a move`() {
        // Given
        val feedback = gardenFeedbackOf(placed(T_AT_5_6))!!

        // Then
        assertThat(feedback.ticks(REDUCED)).isEqualTo(feedback.ticks(FULL))
        assertThat(T_AT_5_6.cells).allSatisfy { cell ->
            assertThat((0..8).map { feedback.light(cell, it, REDUCED) })
                .isEqualTo((0..8).map { feedback.light(cell, it, FULL) })
            assertThat(feedback.lift(cell, 0, REDUCED)).isZero()
            assertThat(feedback.shown(cell, 0, REDUCED)).isEqualTo(1f)
        }
    }

    @Test
    fun `Au hasard drops its pieces one by one from the first column, each 2 ticks after the one before`() {
        // Given a garden rolled, its pieces from the right to the left in the order they are held
        val state = rolled()
        val pieces = state.garden.pieces.sortedBy { piece -> piece.cells.minOf { it.column } }

        // When
        val feedback = gardenFeedbackOf(state)!!

        // Then each piece out of sight until its turn, then shown above its place, all its cells together
        pieces.forEachIndexed { rank, piece ->
            val start = 2 * rank
            assertThat(piece.cells).allSatisfy { cell ->
                assertThat(feedback.shown(cell, start - 1, FULL)).isZero()
                assertThat(feedback.shown(cell, start, FULL)).isEqualTo(1f)
                assertThat(feedback.lift(cell, start, FULL)).isEqualTo(feedback.lift(piece.pivot, start, FULL))
                assertThat(feedback.lift(cell, start, FULL)).isGreaterThan(0f)
            }
        }
        assertThat(feedback.light(pieces.first().pivot, 0, FULL)).isZero()
    }

    @Test
    fun `a piece of Au hasard falls faster and faster to its place, the last one there once the feedback is over`() {
        // Given
        val state = rolled()
        val last = state.garden.pieces.maxBy { piece -> piece.cells.minOf { it.column } }.pivot
        val feedback = gardenFeedbackOf(state)!!

        // When the last piece falls, from its start, 4 ticks after the first one
        val lifts = (4..feedback.ticks(FULL)).map { tick -> feedback.lift(last, tick, FULL) }

        // Then
        val steps = lifts.zipWithNext { above, below -> above - below }
        assertThat(lifts).isSortedAccordingTo(reverseOrder())
        assertThat(steps.dropLast(1)).isSorted()
        assertThat(lifts.last()).isZero()
        assertThat(lifts.dropLast(1)).allSatisfy { assertThat(it).isGreaterThan(0f) }
    }

    @Test
    fun `in reduced effects the pieces of Au hasard appear together where they lie, fading in over 8 ticks`() {
        // Given
        val state = rolled()
        val feedback = gardenFeedbackOf(state)!!
        val cells = state.garden.pieces.flatMap { it.cells }

        // Then nothing falls; every note more and more visible, all of them in step
        assertThat(feedback.ticks(REDUCED)).isEqualTo(8)
        assertThat(cells).allSatisfy { cell ->
            val shown = (0..8).map { tick -> feedback.shown(cell, tick, REDUCED) }
            assertThat(shown.first()).isZero()
            assertThat(shown).isSorted()
            assertThat(shown.last()).isEqualTo(1f)
            assertThat(shown).isEqualTo((0..8).map { tick -> feedback.shown(cells.first(), tick, REDUCED) })
            assertThat((0..8).map { tick -> feedback.lift(cell, tick, REDUCED) }).containsOnly(0f)
        }
    }

    @Test
    fun `a change of mood fades the whole garden in its new mood over 8 ticks, the same in reduced effects`() {
        // Given the mood of a garden changed
        val state = GardenUiState(garden = ROLLED, change = GardenChange.MoodChosen, move = 1)

        // When
        val feedback = gardenFeedbackOf(state)!!

        // Then each note lit at once, then less and less, nothing moving, in full or reduced effects alike
        assertThat(feedback.ticks(FULL)).isEqualTo(8)
        assertThat(feedback.ticks(REDUCED)).isEqualTo(8)
        assertThat(ROLLED.pieces.flatMap { it.cells }).allSatisfy { cell ->
            assertThat(listOf(FULL, REDUCED)).allSatisfy { effects ->
                val lights = (0..8).map { tick -> feedback.light(cell, tick, effects) }
                assertThat(lights.first()).isEqualTo(1f)
                assertThat(lights).isSortedAccordingTo(reverseOrder())
                assertThat(lights.last()).isZero()
                assertThat(feedback.lift(cell, 0, effects)).isZero()
                assertThat(feedback.shown(cell, 0, effects)).isEqualTo(1f)
            }
        }
    }

    @Test
    fun `a state without a change to animate shows no feedback, and two same changes in a row are two feedbacks`() {
        // Given a garden shown without a change to animate, a preview aimed; then a T placed twice
        val unanimated = GardenUiState(garden = ROLLED, aim = Position(3, 3), move = 4)
        val first = placed(T_AT_5_6)
        val second = first.copy(move = 3)

        // Then
        assertThat(gardenFeedbackOf(unanimated)).isNull()
        assertThat(gardenFeedbackOf(second)).isNotEqualTo(gardenFeedbackOf(first))
    }

    @Test
    fun `the notes of the column played bounce to 1,1 times their size and back within motion-garden-bounce`() {
        // When, over the ticks of motion-garden-bounce and the one after
        val length = BloxMotion.gardenBounce
        val scales = (0..length).map { tick -> playHeadBounce(tick, FULL) }
        val peak = scales.indexOf(scales.max())

        // Then larger from the first frame of the eighth, up then down, at rest from its last tick on
        assertThat(scales.max()).isEqualTo(1.1f)
        assertThat(scales.first()).isGreaterThan(1f)
        assertThat(scales.take(peak + 1)).isSorted()
        assertThat(scales.drop(peak)).isSortedAccordingTo(reverseOrder())
        assertThat(scales.subList(length - 1, length + 1)).containsOnly(1f)
        assertThat(playHeadBounce(-1, FULL)).isEqualTo(1f)
    }

    @Test
    fun `in reduced effects the notes of the column played do not bounce, the play head going from column to column`() {
        // Then
        assertThat((0..BloxMotion.gardenBounce).map { tick -> playHeadBounce(tick, REDUCED) }).containsOnly(1f)
    }

    @Test
    fun `the notes of a column played long ago are at rest, however many ticks went by`() {
        // Then, the tick of a bounce over, as on coming back to the screen
        assertThat(playHeadBounce(Int.MAX_VALUE, FULL)).isEqualTo(1f)
    }

    /** The garden of [ROLLED] composed by Au hasard, as the screen shows it then. */
    private fun rolled(): GardenUiState = GardenUiState(garden = ROLLED, change = GardenChange.Rolled, move = 1)

    /** The garden with [piece] placed, as the screen shows it once Poser is pressed. */
    private fun placed(piece: Piece): GardenUiState = GardenUiState(
        garden = (Garden.EMPTY.place(piece) as GardenPlacement.Placed).garden,
        change = GardenChange.Placed(piece),
        move = 1,
    )

    private companion object {
        val FULL = BloxEffects.Full
        val REDUCED = BloxEffects(reduced = true, forcedBySystem = false)

        /** A T in its first orientation, its pivot on (5, 6). */
        val T_AT_5_6 = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(5, 6))

        /** Three pieces, placed from the right to the left: a garden Au hasard might compose. */
        val ROLLED: Garden = listOf(
            Piece(PieceType.O, ClassicRotationSystem.spawnOrientation(PieceType.O), Position(12, 3)),
            Piece(PieceType.I, ClassicRotationSystem.spawnOrientation(PieceType.I), Position(7, 10)),
            Piece(PieceType.L, ClassicRotationSystem.spawnOrientation(PieceType.L), Position(1, 6)),
        ).fold(Garden.EMPTY) { garden, piece -> (garden.place(piece) as GardenPlacement.Placed).garden }
    }
}
