package ch.magori.blox.feature.game.feedback

import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.engine.spawned
import ch.magori.blox.core.game.model.Level
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
        val locked = GameFeedback()
            .after(tick = 100, events = listOf(GameEvent.PieceLocked(lockedT, bySoftDrop = false)))

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
            listOf(GameEvent.PieceLocked(lockedT, bySoftDrop = false)),
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

    @Test
    fun `four lines run the iridescent border once`() {
        // Given rows 16 to 19 full on tick 100, cleared on tick 118, and two rows cleared alike
        val fourLines = clearOfFourRows(lockTick = 100, clearDelay = 18)
        val twoLines = clearOfRows18And19(lockTick = 100, clearDelay = 18)

        // When the ticks go on
        val borders = (118..163).map { tick -> fourLines(tick).irisBorder(reduced = false) }

        // Then a reflection goes once around the frame, from its start towards its end, then is gone; none before the
        // rows are cleared, none for two rows
        val heads = borders.take(BloxMotion.pop).map { checkNotNull(it).head }
        assertThat(heads.first()).isZero()
        assertThat(heads).doesNotHaveDuplicates().isSorted()
        assertThat(heads.last()).isLessThan(1f)
        assertThat(borders.last()).isNull()
        assertThat(fourLines(117).irisBorder(reduced = false)).isNull()
        assertThat((118..163).map { twoLines(it).irisBorder(reduced = false) }).containsOnlyNulls()
    }

    @Test
    fun `in reduced effects four lines frame the board in iridescence, still`() {
        // Given rows 16 to 19 full on tick 100, cleared on tick 118
        val fourLines = clearOfFourRows(lockTick = 100, clearDelay = 18)

        // When the ticks go on, in reduced effects
        val borders = (118..163).map { tick -> fourLines(tick).irisBorder(reduced = true) }

        // Then the whole frame is iridescent, still, as long as the border would go around, and fades in its last 8 ticks
        val shown = borders.take(BloxMotion.pop).map { checkNotNull(it) }
        assertThat(shown).allSatisfy { assertThat(it.whole).isTrue() }
        assertThat(shown.dropLast(BloxMotion.reducedFade).map { it.alpha }).containsOnly(1f)
        assertThat(shown.takeLast(BloxMotion.reducedFade).map { it.alpha }).isSortedAccordingTo(reverseOrder())
            .allSatisfy { assertThat(it).isLessThan(1f).isPositive() }
        assertThat(borders.last()).isNull()
    }

    @Test
    fun `four lines show the sticker « 4 LIGNES » with their points`() {
        // Given rows 16 to 19 cleared on tick 118 for 1 200 points, and two rows cleared alike
        val fourLines = clearOfFourRows(lockTick = 100, clearDelay = 18)
        val twoLines = clearOfRows18And19(lockTick = 100, clearDelay = 18)

        // When the rows are cleared
        val sticker = fourLines(118).sticker(reduced = false)

        // Then the sticker says four lines and their points, in the middle of the rows, in reduced effects as well;
        // the sticker of two rows says their points alone
        assertThat(sticker?.fourLines).isTrue()
        assertThat(sticker?.points).isEqualTo(1_200)
        assertThat(sticker?.row).isEqualTo(18f)
        assertThat(fourLines(118).sticker(reduced = true)?.fourLines).isTrue()
        assertThat(twoLines(118).sticker(reduced = false)?.fourLines).isFalse()
    }

    @Test
    fun `a level up frames the level for 1 second and tilts it`() {
        // Given a line clear that reached level 5 on tick 100
        val levelledUp = GameFeedback().after(
            100,
            listOf(GameEvent.LinesCleared(LineClear(1)), GameEvent.LevelUp(Level(5))),
        )

        // When the ticks go on
        val looks = (100..160).map { tick -> levelledUp.ticked(tick).levelLook(reduced = false) }

        // Then the level is framed for 60 ticks, then no more; it tilts one way then the other, and comes back straight
        assertThat(looks.take(60)).allSatisfy { assertThat(it.framed).isTrue() }
        assertThat(looks.last().framed).isFalse()
        val tilts = looks.map { it.tilt }
        assertThat(tilts.first()).isZero()
        assertThat(tilts).anySatisfy { assertThat(it).isPositive() }.anySatisfy { assertThat(it).isNegative() }
        assertThat(tilts.takeLast(10)).containsOnly(0f)
        assertThat(GameFeedback().levelLook(reduced = false)).isEqualTo(LevelLook(framed = false, tilt = 0f))
    }

    @Test
    fun `in reduced effects a level up frames the level without tilting it`() {
        // Given a line clear that reached level 5 on tick 100
        val levelledUp = GameFeedback().after(
            100,
            listOf(GameEvent.LinesCleared(LineClear(1)), GameEvent.LevelUp(Level(5))),
        )

        // When the ticks go on, in reduced effects
        val looks = (100..160).map { tick -> levelledUp.ticked(tick).levelLook(reduced = true) }

        // Then the level is framed for 60 ticks, as in full effects, and never tilts
        assertThat(looks.take(60)).allSatisfy { assertThat(it.framed).isTrue() }
        assertThat(looks.last().framed).isFalse()
        assertThat(looks.map { it.tilt }).containsOnly(0f)
    }

    @Test
    fun `the danger frame is 4 dp and stays until the danger ends`() {
        // Given the stack entered the danger on tick 100, then left it on tick 400
        val palette = BloxPalettes.ColorfulLight
        val entered = GameFeedback().after(100, listOf(GameEvent.StackDanger(on = true)))
        val left = entered.after(400, listOf(GameEvent.StackDanger(on = false)))

        // When the ticks go on
        val borders = (100..399).map { tick -> boardFrameBorder(palette, entered.ticked(tick).inDanger) }

        // Then the frame of the board is 4 dp of danger all along, and back to 2 dp of panel border once the danger ends
        assertThat(borders).containsOnly(FrameBorder(4.dp, palette.danger))
        assertThat(
            boardFrameBorder(palette, left.inDanger),
        ).isEqualTo(FrameBorder(BloxBorders.frame, palette.panelBorder))
        assertThat(boardFrameBorder(palette, GameFeedback().inDanger)).isEqualTo(FrameBorder(2.dp, palette.panelBorder))
    }

    @Test
    fun `in danger the 5 top rows breathe in danger-soft`() {
        // Given the stack entered the danger on tick 100
        val entered = GameFeedback().after(100, listOf(GameEvent.StackDanger(on = true)))

        // When the ticks go on
        val tints = (100..400).map { tick -> entered.ticked(tick).dangerTint(row = 4, reduced = false) }

        // Then the 5 top rows are tinted all along, more and less, and not the rows under them, nor without danger
        assertThat(tints).allSatisfy { assertThat(it).isPositive().isLessThanOrEqualTo(1f) }
        assertThat(tints.max() - tints.min()).isGreaterThan(0.3f)
        assertThat(entered.dangerTint(row = 5, reduced = false)).isZero()
        assertThat(GameFeedback().dangerTint(row = 0, reduced = false)).isZero()
    }

    @Test
    fun `in reduced effects the 5 top rows in danger are tinted, still`() {
        // Given the stack entered the danger on tick 100
        val entered = GameFeedback().after(100, listOf(GameEvent.StackDanger(on = true)))

        // When the ticks go on, in reduced effects
        val tints = (100..400).map { tick -> (0..5).map { entered.ticked(tick).dangerTint(it, reduced = true) } }

        // Then the 5 top rows are wholly tinted all along, the others not at all
        assertThat(tints).containsOnly(listOf(1f, 1f, 1f, 1f, 1f, 0f))
    }

    @Test
    fun `nothing flashes more than 3 times a second`() {
        // Given four lines cleared on tick 118, which reach the next level and leave the stack in danger
        val fourLines = clearOfFourRows(lockTick = 100, clearDelay = 18)
        val cleared = fourLines(118).after(
            118,
            listOf(GameEvent.LevelUp(Level(5)), GameEvent.StackDanger(on = true)),
        )
        val feedbacks = (100..117).map(fourLines) + (118..700).map { cleared.ticked(it) }

        // When the effects play, in full and in reduced effects
        val lights = listOf(false, true).flatMap { reduced ->
            listOf<(GameFeedback) -> Float>(
                { it.lockLight(lockedT.cells.first(), reduced) },
                { if (it.rowGlows(16)) 1f else 0f },
                { it.irisBorder(reduced)?.alpha ?: 0f },
                { it.sticker(reduced)?.alpha ?: 0f },
                { if (it.levelLook(reduced).framed) 1f else 0f },
                { it.dangerTint(row = 0, reduced) },
            ).map { light -> feedbacks.map(light) }
        }

        // Then no light grows brighter more than 3 times in any second
        assertThat(lights).allSatisfy { light ->
            assertThat(light.windowed(60).maxOf { brightenings(it) }).isLessThanOrEqualTo(3)
        }
    }

    @Test
    fun `there are 40 particles at most`() {
        // Given four lines full on tick 100, the most a clear bursts, cleared 20 ticks later
        val fourLines = clearOfFourRows(lockTick = 100, clearDelay = 20)

        // When the clear goes on, counting the cells that burst: drawn, but not at their size
        val bursting = (100..140).map { tick ->
            (0..19).sumOf { row ->
                (0..9).count { column ->
                    val scale = fourLines(tick).cellScale(Position(column, row), reduced = false)
                    scale > 0f && scale != 1f
                }
            }
        }

        // Then no more than 40 cells burst at once
        assertThat(bursting.max()).isPositive().isLessThanOrEqualTo(40)
    }

    @Test
    fun `a beaten best bounces its sticker once in 30 ticks`() {
        // Given the best beaten on tick 100
        val beaten = GameFeedback().ticked(100).withBestBeaten()

        // When the ticks go on, and the score goes on above the best
        val scales = (100..140).map { tick -> beaten.ticked(tick).bestBounce(reduced = false) }
        val beatenAgain = beaten.ticked(120).withBestBeaten()

        // Then the sticker swells and comes back to its size in 30 ticks, once only
        assertThat(scales.take(BloxMotion.sticker)).anySatisfy { assertThat(it).isGreaterThan(1f) }
        assertThat(scales.drop(BloxMotion.sticker)).containsOnly(1f)
        assertThat(beatenAgain.bestBounce(reduced = false)).isEqualTo(scales[20])
        assertThat(GameFeedback().bestBounce(reduced = false)).isEqualTo(1f)
    }

    @Test
    fun `in reduced effects a beaten best shows without bouncing`() {
        // Given the best beaten on tick 100
        val beaten = GameFeedback().ticked(100).withBestBeaten()

        // When the ticks go on, in reduced effects
        val scales = (100..140).map { tick -> beaten.ticked(tick).bestBounce(reduced = true) }

        // Then the sticker takes the place of the best at its size
        assertThat(scales).containsOnly(1f)
    }

    /** How many times [light] starts growing brighter: a tick brighter than the one before, which was not. */
    private fun brightenings(light: List<Float>): Int = light.indices.drop(1).count { i ->
        light[i] > light[i - 1] && (i == 1 || light[i - 1] <= light[i - 2])
    }

    /** The feedback, on each tick, of a game whose rows 16 to 19 filled on [lockTick], cleared [clearDelay] ticks later. */
    private fun clearOfFourRows(
        lockTick: Int,
        clearDelay: Int,
    ): (Int) -> GameFeedback =
        lineClearFeedback(lockedT, listOf(16, 17, 18, 19), lockTick, clearDelay, Points(1_000), Points(2_200))

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
