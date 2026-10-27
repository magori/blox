package ch.magori.blox.feature.game.feedback

import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.spawned
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.feature.game.controls.ControlButton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GameFeedbackTest {
    /** A T locked where it spawns: its cells on rows 0 and 1, around column 5. */
    private val lockedT = spawned(PieceType.T)

    @Test
    fun `a lock lights its four cells for 8 ticks`() {
        // Given a T locked on tick 100
        val locked = GameFeedback().after(tick = 100, events = listOf(GameEvent.PieceLocked(lockedT)))

        // When the ticks go on
        val lit = (100..108).map { tick -> locked.ticked(tick).lockLight(lockedT.cells.first(), reduced = false) }
        val beside = locked.lockLight(Position(column = 0, row = 19), reduced = false)

        // Then its cells are lit on the tick of the lock and the 7 ticks after it, less and less, then no more
        assertThat(lockedT.cells).hasSize(4).allSatisfy { cell ->
            assertThat(locked.lockLight(cell, reduced = false)).isPositive()
        }
        assertThat(lit.take(8)).allSatisfy { assertThat(it).isPositive() }.isSortedAccordingTo(reverseOrder())
        assertThat(lit.last()).isZero()
        assertThat(beside).isZero()
    }

    @Test
    fun `a rotation turns the icon of its key by 90 degrees in 6 ticks`() {
        // Given A turned the piece clockwise on tick 100, and B counterclockwise on tick 200
        val turnedByA = GameFeedback().after(100, listOf(GameEvent.PieceRotated(lockedT, Rotation.CLOCKWISE)))
        val turnedByB = turnedByA.after(200, listOf(GameEvent.PieceRotated(lockedT, Rotation.COUNTERCLOCKWISE)))

        // When the ticks go on
        val angleOfA = (100..106).map { tick -> turnedByA.ticked(tick).keyAngle(ControlButton.A, reduced = false) }
        val angleOfB = (200..206).map { tick -> turnedByB.ticked(tick).keyAngle(ControlButton.B, reduced = false) }

        // Then the icon of A turns clockwise by 90 degrees back to its place, that of B counterclockwise; not the other
        assertThat(angleOfA.first()).isEqualTo(-90f)
        assertThat(angleOfA).isSorted().last().isEqualTo(0f)
        assertThat(angleOfB.first()).isEqualTo(90f)
        assertThat(angleOfB).isSortedAccordingTo(reverseOrder()).last().isEqualTo(0f)
        assertThat(turnedByA.keyAngle(ControlButton.B, reduced = false)).isZero()
    }

    @Test
    fun `a turn of B leaves the turn of A going on`() {
        // Given A turned the piece on tick 100, then B on tick 102
        val turnedByA = GameFeedback().after(100, listOf(GameEvent.PieceRotated(lockedT, Rotation.CLOCKWISE)))
        val turnedByB = turnedByA.after(102, listOf(GameEvent.PieceRotated(lockedT, Rotation.COUNTERCLOCKWISE)))

        // Then the icon of A goes on turning where it stood, and that of B starts its own turn
        assertThat(turnedByB.keyAngle(ControlButton.A, reduced = false)).isEqualTo(-60f)
        assertThat(turnedByB.keyAngle(ControlButton.B, reduced = false)).isEqualTo(90f)
    }

    @ParameterizedTest
    @ValueSource(ints = [17, 18, 19, 20])
    fun `the clear pops the cells by pairs from the centre within the clear delay`(clearDelay: Int) {
        // Given rows 18 and 19 full on tick 100, cleared clearDelay ticks later, as the engine says
        val clearing = clearOfRows18And19(lockTick = 100, clearDelay)

        // When the ticks of the clear go on
        val ticks = 100 until 100 + clearDelay
        val scales = ticks.map { tick -> (0..9).map { clearing(tick).cellScale(Position(it, 19), reduced = false) } }
        val goneOn = (0..4).map { pair -> ticks.first { tick -> scales[tick - 100][4 - pair] == 0f } }

        // Then the cells are whole on the tick of the lock, the two of each pair alike, the centre ones gone first, and
        // all gone on the last tick of the clear; the rows glow all along, and the rows above stay as they are
        assertThat(scales.first()).containsOnly(1f)
        assertThat(scales).allSatisfy { row -> assertThat(row).isEqualTo(row.reversed()) }
        assertThat(goneOn).doesNotHaveDuplicates().isSorted()
        assertThat(scales.last()).containsOnly(0f)
        assertThat(ticks).allSatisfy { tick -> assertThat(clearing(tick).rowGlows(18)).isTrue() }
        assertThat(clearing(105).cellScale(Position(4, 17), reduced = false)).isEqualTo(1f)
        assertThat(clearing(105).rowGlows(17)).isFalse()
    }

    @Test
    fun `once the rows are cleared the rows above them slide down to their place`() {
        // Given rows 15 and 18 full, cleared on tick 120
        val locked = GameFeedback().after(
            100,
            listOf(GameEvent.PieceLocked(lockedT)),
            ClearingRows(listOf(15, 18), ticksLeft = 20),
            Points(0),
            Points(0),
        )
        val cleared = locked.after(120, listOf(GameEvent.LinesCleared(LineClear(2))), null, Points(0), Points(300))

        // When the ticks go on
        val lifts = (120..128).map { tick -> (0..19).map { cleared.ticked(tick).rowLift(it, reduced = false) } }

        // Then each row is first drawn where it stood, as high above its place as rows were cleared under it, then comes
        // down to it, the rows of the board cleared at once
        assertThat(lifts.first().takeLast(4)).containsExactly(2f, 1f, 1f, 0f)
        assertThat(lifts.first().take(16)).containsOnly(2f)
        assertThat(lifts.map { it[18] }).isSortedAccordingTo(reverseOrder())
        assertThat(lifts.last()).containsOnly(0f)
        assertThat(cleared.cellScale(Position(4, 18), reduced = false)).isEqualTo(1f)
        assertThat(cleared.rowGlows(18)).isFalse()
    }

    @Test
    fun `the points sticker rises and fades in 45 ticks`() {
        // Given rows 18 and 19 cleared on tick 118 for 100 points
        val clearing = clearOfRows18And19(lockTick = 100, clearDelay = 18)

        // When the ticks go on
        val stickers = (118..163).map { tick -> clearing(tick) }

        // Then the sticker shows the points gained in the middle of the cleared rows, rises, fades, and is gone
        val first = stickers.first().sticker(reduced = false)
        assertThat(first?.points).isEqualTo(100)
        assertThat(first?.row).isEqualTo(19f)
        assertThat(first?.rise).isZero()
        assertThat(first?.alpha).isEqualTo(1f)
        val shown = stickers.take(45).map { checkNotNull(it.sticker(reduced = false)) }
        assertThat(shown.map { it.rise }).isSorted()
        assertThat(shown.last().rise).isPositive()
        assertThat(shown.map { it.alpha }).isSortedAccordingTo(reverseOrder())
        assertThat(shown.last().alpha).isPositive()
        assertThat(stickers.last().sticker(reduced = false)).isNull()
        assertThat(clearing(117).sticker(reduced = false)).isNull()
    }

    @Test
    fun `the score counts up to its new value once the rows are cleared`() {
        // Given rows 18 and 19 cleared on tick 118, the score going from 1 000 to 1 100
        val clearing = clearOfRows18And19(lockTick = 100, clearDelay = 18)

        // When the ticks go on
        val shown = (118..160).map { tick -> clearing(tick).shownScore(Points(1_100), reduced = false).value }

        // Then the score shown starts from the score before, counts up, and reaches the score
        assertThat(shown.first()).isEqualTo(1_000)
        assertThat(shown).isSorted().contains(1_050).last().isEqualTo(1_100)
        assertThat(clearing(117).shownScore(Points(1_000), reduced = false)).isEqualTo(Points(1_000))
        assertThat(clearing(119).shownScore(Points(1_200), reduced = false))
            .describedAs("a score that changed again, shown as it is")
            .isEqualTo(Points(1_200))
    }

    @Test
    fun `in reduced effects nothing moves and the score shows at once`() {
        // Given a T locked and turned by A on tick 100, filling rows 18 and 19, cleared on tick 118 for 100 points
        val clear = clearOfRows18And19(lockTick = 100, clearDelay = 18)
        val turned = GameFeedback().after(100, listOf(GameEvent.PieceRotated(lockedT, Rotation.CLOCKWISE)))

        // When the ticks go on, in reduced effects
        val clearing = (100..117).map(clear)
        val cleared = (118..163).map(clear)

        // Then the cells of the full rows are hidden at once in their glowing rows, the rows above at their place, the
        // score is the new one at once, and the sticker stays where it shows: no light, no key turns
        val cell = lockedT.cells.first()
        assertThat(clearing).allSatisfy { feedback ->
            assertThat(feedback.lockLight(cell, reduced = true)).isZero()
            assertThat((0..9).map { feedback.cellScale(Position(it, 19), reduced = true) }).containsOnly(0f)
            assertThat(feedback.rowGlows(19)).isTrue()
        }
        assertThat((100..106).map { turned.ticked(it).keyAngle(ControlButton.A, reduced = true) }).containsOnly(0f)
        assertThat(cleared).allSatisfy { feedback ->
            assertThat((0..19).map { feedback.rowLift(it, reduced = true) }).containsOnly(0f)
            assertThat(feedback.shownScore(Points(1_100), reduced = true)).isEqualTo(Points(1_100))
        }
        val stickers = cleared.take(45).map { checkNotNull(it.sticker(reduced = true)) }
        assertThat(stickers.map { it.rise }).containsOnly(0f)
        assertThat(cleared.last().sticker(reduced = true)).isNull()
    }

    @Test
    fun `in reduced effects the sticker fades out in 8 ticks at most`() {
        // Given rows 18 and 19 cleared on tick 118
        val clear = clearOfRows18And19(lockTick = 100, clearDelay = 18)

        // When the ticks go on, in reduced effects
        val alphas = (118..162).map { tick -> checkNotNull(clear(tick).sticker(reduced = true)).alpha }

        // Then the sticker shows whole, and fades only during its last 8 ticks
        assertThat(alphas.dropLast(BloxMotion.reducedFade)).containsOnly(1f)
        assertThat(alphas.takeLast(BloxMotion.reducedFade)).isSortedAccordingTo(reverseOrder()).allSatisfy {
            assertThat(it).isLessThan(1f).isPositive()
        }
    }

    /** The feedback, on each tick, of a game whose rows 18 and 19 filled on [lockTick], cleared [clearDelay] ticks later. */
    private fun clearOfRows18And19(
        lockTick: Int,
        clearDelay: Int,
    ): (
        Int,
    ) -> GameFeedback =
        lineClearFeedback(lockedT, listOf(18, 19), lockTick, clearDelay, Points(1_000), Points(1_100))

    private fun GameFeedback.lockLight(
        cell: Position,
        reduced: Boolean,
    ): Float = lockLight(cell.column, cell.row, reduced)

    private fun GameFeedback.cellScale(
        cell: Position,
        reduced: Boolean,
    ): Float = cellScale(cell.column, cell.row, reduced)

    /** [this] feedback once the game reached [tick] without any event, at the same score. */
    private fun GameFeedback.ticked(tick: Int): GameFeedback = after(tick = tick, events = emptyList())

    private fun GameFeedback.after(
        tick: Int,
        events: List<GameEvent>,
    ): GameFeedback = after(tick, events, clearing = null, scoreBefore = Points(0), score = Points(0))
}
