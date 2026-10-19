package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.cellsOf
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.tPointingDown
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

class GameEngineControlsTest {
    private val engine = GameEngine(ClassicRules, ScriptedRandomizer())

    /** An engine for the tests where a J, next, appears: it draws an S after it. */
    private val nextPieceEngine = GameEngine(ClassicRules, ScriptedRandomizer(PieceType.S))

    @Test
    fun `pressing left moves nothing at once, the piece shifts one column on the next tick`() {
        // Given a T falling at level 0, far from its next drop
        val state = gameState(phase = Phase.Falling(tPointingDown(at = Position(5, 10))))

        // When
        val pressed = engine.apply(state, Input.Press(Button.LEFT))
        val ticked = engine.tick(pressed.state)

        // Then
        assertThat(pressed.state.current?.pivot).isEqualTo(Position(5, 10))
        assertThat(pressed.events).isEmpty()
        assertThat(ticked.state.current?.pivot).isEqualTo(Position(4, 10))
    }

    @Test
    fun `a held left shifts on the first tick, then 16 ticks after the press, then every 6 ticks`() {
        // Given a T falling at level 0, far from its next drop, and left pressed
        val state = gameState(phase = Phase.Falling(tPointingDown(at = Position(8, 10))))
        val pressed = engine.apply(state, Input.Press(Button.LEFT)).state

        // When
        val shiftTicks = ticksWhereThePieceMoved(pressed, ticks = 30)

        // Then
        assertThat(shiftTicks).containsExactly(0, 16, 22, 28)
    }

    @Test
    fun `when left and right are held together, right wins`() {
        // Given
        val state = gameState(phase = Phase.Falling(tPointingDown(at = Position(5, 10))))
        val pressed = engine.press(state, Button.LEFT, Button.RIGHT)

        // When
        val ticked = engine.tick(pressed).state

        // Then
        assertThat(ticked.current?.pivot).isEqualTo(Position(6, 10))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("newShiftPresses")
    fun `a new press of left or right starts the shift repeat again, so the piece shifts at once`(
        heldFirst: List<Button>,
        inputs: List<Input>,
        expectedColumn: Int,
    ) {
        assertThat(columnAfterTenTicksThen(heldFirst, inputs)).isEqualTo(expectedColumn)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("directionChangesWithoutAPress")
    fun `a change of direction without a new press goes on with the shift repeat, with no shift at once`(
        heldFirst: List<Button>,
        inputs: List<Input>,
        expectedColumn: Int,
    ) {
        assertThat(columnAfterTenTicksThen(heldFirst, inputs)).isEqualTo(expectedColumn)
    }

    /**
     * Given a T at (5, 10) and [heldFirst] pressed then held for 10 ticks, when [inputs] come and a tick
     * passes: the column of the piece.
     */
    private fun columnAfterTenTicksThen(
        heldFirst: List<Button>,
        inputs: List<Input>,
    ): Int? {
        val state = gameState(phase = Phase.Falling(T_AT_5_10))
        val held = engine.ticks(engine.press(state, *heldFirst.toTypedArray()), 10)
        return engine.tick(inputs.fold(held) { current, input -> engine.apply(current, input).state }).state
            .current?.pivot?.column
    }

    @Test
    fun `a shift refused by the stack charges the repeat, so the piece shifts as soon as the way is free`() {
        // Given a T held to the left against a cell at (3, 10), dropping below it on the 4th tick
        val board = Board.parse(
            """
            ...O......
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            ..........
            """,
        )
        val falling = Phase.Falling(tPointingDown(at = Position(5, 10)), ticksSinceDrop = 44)
        val pressed = engine.press(gameState(board = board, phase = falling), Button.LEFT)

        // When
        val shiftTicks = ticksWhereThePieceMoved(pressed, ticks = 12)

        // Then
        assertThat(shiftTicks).containsExactly(4, 10)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("rotations")
    fun `A turns the piece clockwise and B counterclockwise, on the next tick`(
        button: Button,
        expectedDrawing: String,
    ) {
        // Given a T pointing down, and the button pressed
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), button)

        // When
        val ticked = engine.tick(pressed).state

        // Then
        assertThat(ticked.current).isEqualTo(Piece(PieceType.T, Orientation(cellsOf(expectedDrawing)), Position(5, 10)))
    }

    @Test
    fun `a held A turns the piece once, a short tap between two ticks turns it too`() {
        // Given a T pointing down
        val state = gameState(phase = Phase.Falling(T_AT_5_10))

        // When A is held for two ticks, or pressed and released before a tick
        val held = engine.ticks(engine.press(state, Button.A), 2)
        val tapped = engine.tick(engine.apply(engine.press(state, Button.A), Input.Release(Button.A)).state).state

        // Then
        val turnedOnce = Piece(PieceType.T, Orientation(cellsOf(T_LEFT)), Position(5, 10))
        assertThat(held.current).isEqualTo(turnedOnce)
        assertThat(tapped.current).isEqualTo(turnedOnce)
    }

    @Test
    fun `a held down drops the piece a first row 2 ticks after the press, then every 2 ticks`() {
        // Given a T falling at level 0, far from its next drop, and down pressed
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN)

        // When
        val dropTicks = ticksWhereThePieceDropped(pressed, ticks = 9)

        // Then
        assertThat(dropTicks).containsExactly(2, 4, 6, 8)
    }

    @Test
    fun `a soft drop step starts the gravity count again`() {
        // Given down held for 3 ticks, its step on the third, then released, at level 0 (48 ticks per row)
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN)
        val released = engine.apply(engine.ticks(pressed, 3), Input.Release(Button.DOWN)).state

        // When
        val dropTicks = ticksWhereThePieceDropped(released, ticks = 48)

        // Then the gravity drops the piece 48 ticks after the soft drop step
        assertThat(dropTicks).containsExactly(47)
    }

    @Test
    fun `the piece drops at most one row per tick, even when the soft drop and the gravity both fall due`() {
        // Given down held at level 29, where the gravity drops the piece on every tick
        val pressed = engine.press(gameState(lines = LINES_TO_LEVEL_29, phase = Phase.Falling(T_AT_5_10)), Button.DOWN)

        // When
        val rows = generateSequence(pressed) { engine.tick(it).state }.take(7).map { it.current?.pivot?.row }.toList()

        // Then
        assertThat(rows).containsExactly(10, 11, 12, 13, 14, 15, 16)
    }

    @Test
    fun `a held down blocks the shift`() {
        // Given down held, then left pressed
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN, Button.LEFT)

        // When
        val shiftTicks = ticksWhereThePieceMoved(pressed, ticks = 20)

        // Then
        assertThat(shiftTicks).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Button::class, names = ["LEFT", "RIGHT"])
    fun `a held shift stops the soft drop and its points, and down must be pressed again to soft drop`(shift: Button) {
        // Given down held for 3 ticks, which dropped the piece one row on the third
        val dropping = engine.ticks(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN), 3)

        // When the shift is held for 6 ticks, then released while down is still held, then down is pressed again
        val shiftHeld = engine.ticks(engine.press(dropping, shift), 6)
        val shiftReleased = engine.apply(shiftHeld, Input.Release(shift)).state
        val downPressedAgain = engine.press(engine.apply(shiftReleased, Input.Release(Button.DOWN)).state, Button.DOWN)

        // Then the soft drop stops, its points are gone, and it only starts again on the new press of down
        assertThat(dropping.phase).isInstanceOfSatisfying(Phase.Falling::class.java) { falling ->
            assertThat(falling.softDropRows).isEqualTo(1)
        }
        assertThat(shiftHeld.current?.pivot).isEqualTo(Position(5, 11))
        assertThat(shiftHeld.phase).isInstanceOfSatisfying(Phase.Falling::class.java) { falling ->
            assertThat(falling.softDropRows).isEqualTo(0)
        }
        assertThat(ticksWhereThePieceDropped(shiftReleased, ticks = 5)).isEmpty()
        assertThat(ticksWhereThePieceDropped(downPressedAgain, ticks = 5)).containsExactly(2, 4)
    }

    @Test
    fun `down released and pressed again between two ticks starts the soft drop again`() {
        // Given down held for 4 ticks: steps on ticks 2 and 4 would follow, the next one on tick 6
        val dropping = engine.ticks(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.DOWN), 4)

        // When down is released and pressed again before the next tick
        val pressedAgain = engine.press(engine.apply(dropping, Input.Release(Button.DOWN)).state, Button.DOWN)

        // Then the soft drop starts again: its next step comes 2 ticks after the new press
        assertThat(ticksWhereThePieceDropped(pressedAgain, ticks = 5)).containsExactly(2, 4)
    }

    @Test
    fun `the rows soft dropped score when the piece locks, the refused step that locks it does not count`() {
        // Given a T with its pivot on row 14 and down held: steps to rows 15 to 18, then a refused step
        val pressed = engine.press(gameState(phase = Phase.Falling(tPointingDown(at = Position(5, 14)))), Button.DOWN)

        // When
        val beforeLock = engine.ticks(pressed, 10)
        val lock = engine.tick(beforeLock)

        // Then
        assertThat(beforeLock.current?.pivot).isEqualTo(Position(5, 18))
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(tPointingDown(at = Position(5, 18))))
        assertThat(lock.state.progress.score).isEqualTo(Points(4))
    }

    @Test
    fun `only the rows soft dropped in a row score, a release of down starts them again`() {
        // Given a T with its pivot on row 14, down held for 2 steps (rows 15, 16), released for a tick, then held again
        val firstSteps = engine.ticks(
            engine.press(gameState(phase = Phase.Falling(tPointingDown(at = Position(5, 14)))), Button.DOWN),
            5,
        )
        val released = engine.tick(engine.apply(firstSteps, Input.Release(Button.DOWN)).state).state
        val pressedAgain = engine.press(released, Button.DOWN)

        // When the second soft drop takes the piece to row 18, then locks it
        val lock = engine.tick(engine.ticks(pressedAgain, 6))

        // Then only the 2 rows of the second soft drop score
        assertThat(firstSteps.current?.pivot).isEqualTo(Position(5, 16))
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(tPointingDown(at = Position(5, 18))))
        assertThat(lock.state.progress.score).isEqualTo(Points(2))
    }

    @Test
    fun `a lock by soft drop goes the way of a lock by gravity, line clear, entry delay and events included`() {
        // Given a T with its pivot on row 16 and down held: steps to rows 17 and 18, then locks on tick 7, filling 2 rows
        val pressed = engine.press(
            gameState(board = TWO_ROWS_TO_FILL, phase = Phase.Falling(tPointingDown(at = Position(5, 16)))),
            Button.DOWN,
        )

        // When
        val lock = engine.tick(engine.ticks(pressed, 6))
        val clearEnd = engine.tick(engine.ticks(lock.state, 16))

        // Then the clear lasts 17 ticks (lock on tick 7), then comes the entry delay of a lock on row 18
        assertThat(lock.events).containsExactly(GameEvent.PieceLocked(tPointingDown(at = Position(5, 18))))
        assertThat(lock.state.phase).isInstanceOfSatisfying(Phase.Clearing::class.java) { clearing ->
            assertThat(clearing.ticksLeft).isEqualTo(17)
        }
        assertThat(clearEnd.events).containsExactly(GameEvent.LinesCleared(LineClear(2)))
        assertThat(clearEnd.state.phase).isEqualTo(Phase.EntryDelay(10))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("boardsToLockOn")
    fun `the held shift outlives the lock and stands still during the line clear and the entry delay`(board: Board) {
        // Given left held: the T shifts on the first tick, then locks on the floor on that same tick
        val falling = Phase.Falling(tPointingDown(at = Position(5, 18)), ticksSinceDrop = 47)
        val pressed = nextPieceEngine.press(gameState(board = board, next = PieceType.J, phase = falling), Button.LEFT)
        val spawned = firstStateWithTheNextPiece(pressed)

        // When
        val shiftTicks = generateSequence(spawned) { nextPieceEngine.tick(it).state }
            .take(17)
            .map { it.current?.pivot?.column }

        // Then the repeat goes on from where it stood: the next step on the 16th tick the J falls
        assertThat(spawned.current).isEqualTo(spawned(PieceType.J))
        assertThat(shiftTicks.toList()).containsExactly(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4)
    }

    @Test
    fun `a down held through the lock does not drop the next piece until it is pressed again`() {
        // Given down held: the T drops to the floor and locks; the J appears 10 ticks later
        val falling = Phase.Falling(tPointingDown(at = Position(5, 17)))
        val pressed = nextPieceEngine.press(gameState(next = PieceType.J, phase = falling), Button.DOWN)
        val spawned = firstStateWithTheNextPiece(pressed)

        // When down stays held for 6 ticks, then is released and pressed again
        val held = nextPieceEngine.ticks(spawned, 6)
        val pressedAgain = nextPieceEngine.press(
            nextPieceEngine.apply(held, Input.Release(Button.DOWN)).state,
            Button.DOWN,
        )

        // Then the held down does nothing on the J; the new press soft drops it 2 ticks later
        assertThat(spawned.phase).isEqualTo(Phase.Falling(spawned(PieceType.J)))
        assertThat(held.current).isEqualTo(spawned(PieceType.J))
        assertThat(ticksWhereThePieceDropped(pressedAgain, ticks = 5, nextPieceEngine)).containsExactly(2, 4)
    }

    // "Before the next piece": during the line clear or the entry delay, while the ROM does not read the controller.

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("rotationsBeforeTheNextPiece")
    fun `A or B pressed before the next piece is lost, it appears unturned`(
        board: Board,
        phase: Class<out Phase>,
        button: Button,
    ) {
        // Given a T that locks on the first tick, then the button pressed and held until the J appears
        val pressed = nextPieceEngine.press(lockedOnTheFirstTick(board, into = phase), button)

        // When
        val falls = nextPieceEngine.ticks(firstStateWithTheNextPiece(pressed), 3)

        // Then
        assertThat(falls.current).isEqualTo(spawned(PieceType.J))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("shiftsBeforeTheNextPiece")
    fun `left or right pressed before the next piece, with no shift repeat yet, shifts it on its 16th tick of fall`(
        board: Board,
        phase: Class<out Phase>,
        button: Button,
    ) {
        // Given a T that locks on the first tick, with no shift repeat yet, then the button pressed and held
        val locked = lockedOnTheFirstTick(board, into = phase)
        val pressed = nextPieceEngine.press(locked, button)

        // When
        val shiftTicks = ticksWhereThePieceMoved(firstStateWithTheNextPiece(pressed), ticks = 22, nextPieceEngine)

        // Then the press starts nothing: the J shifts on its 16th tick of fall (the 15th counted from 0), not at once
        assertThat(locked.controls.shift).isNull()
        assertThat(shiftTicks).containsExactly(15, 21)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Button::class, names = ["LEFT", "RIGHT"])
    fun `left or right pressed before the next piece, with a charged shift repeat, shifts it on its first tick of fall`(
        button: Button,
    ) {
        // Given a T pushed left against the wall, which charges the shift repeat, then left released before the lock
        val againstTheWall = Phase.Falling(tPointingDown(at = Position(1, 18)), ticksSinceDrop = 46)
        val pushed = nextPieceEngine.tick(
            nextPieceEngine.press(gameState(next = PieceType.J, phase = againstTheWall), Button.LEFT),
        ).state
        val locked = nextPieceEngine.tick(nextPieceEngine.apply(pushed, Input.Release(Button.LEFT)).state).state

        // When the button is pressed during the entry delay, and held
        val pressed = nextPieceEngine.press(locked, button)
        val shiftTicks = ticksWhereThePieceMoved(firstStateWithTheNextPiece(pressed), ticks = 7, nextPieceEngine)

        // Then the press starts nothing again: the charged repeat shifts the J at once, then 6 ticks later
        assertThat(pushed.current?.pivot).isEqualTo(Position(1, 18))
        assertThat(locked.phase).isInstanceOf(Phase.EntryDelay::class.java)
        assertThat(shiftTicks).containsExactly(0, 6)
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("locksBeforeTheNextPiece")
    fun `down pressed before the next piece does not soft drop it until it is pressed again`(
        board: Board,
        phase: Class<out Phase>,
    ) {
        // Given a T that locks on the first tick, then down pressed and held until the J appears
        val pressed = nextPieceEngine.press(lockedOnTheFirstTick(board, into = phase), Button.DOWN)
        val spawned = firstStateWithTheNextPiece(pressed)

        // When down stays held for 6 ticks, then is released and pressed again
        val held = nextPieceEngine.ticks(spawned, 6)
        val pressedAgain = nextPieceEngine.press(
            nextPieceEngine.apply(held, Input.Release(Button.DOWN)).state,
            Button.DOWN,
        )

        // Then the held down does nothing on the J; the new press soft drops it 2 ticks later
        assertThat(held.current).isEqualTo(spawned(PieceType.J))
        assertThat(ticksWhereThePieceDropped(pressedAgain, ticks = 5, nextPieceEngine)).containsExactly(2, 4)
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Button::class, names = ["A", "LEFT"])
    fun `A or left pressed on the last tick of the entry delay is lost, the next piece neither turns nor shifts`(
        button: Button,
    ) {
        // Given the J due to appear on the next tick, and the button pressed and held
        val pressed = nextPieceEngine.press(gameState(next = PieceType.J, phase = Phase.EntryDelay(1)), button)

        // When the J appears, then falls one tick
        val spawned = firstStateWithTheNextPiece(pressed)
        val firstFall = nextPieceEngine.tick(spawned).state

        // Then
        assertThat(spawned.tickCount).isEqualTo(1)
        assertThat(firstFall.current).isEqualTo(spawned(PieceType.J))
    }

    @Test
    fun `a press of a button already held is not a new press`() {
        // Given A pressed, then pressed again while still held
        val state = gameState(phase = Phase.Falling(T_AT_5_10))
        val turned = engine.tick(engine.press(state, Button.A)).state

        // When
        val pressedAgain = engine.tick(engine.press(turned, Button.A)).state

        // Then the piece turned once only
        assertThat(pressedAgain.current).isEqualTo(Piece(PieceType.T, Orientation(cellsOf(T_LEFT)), Position(5, 10)))
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("pressesWithDown")
    fun `a press of down starts the soft drop unless left or right is pressed on the same tick`(
        otherButton: Button,
        expectedDropTicks: List<Int>,
    ) {
        // Given down pressed, and another button tapped (pressed and released) before the same tick
        val state = gameState(phase = Phase.Falling(T_AT_5_10))
        val pressed = engine.apply(engine.press(state, Button.DOWN, otherButton), Input.Release(otherButton)).state

        // When
        val dropTicks = ticksWhereThePieceDropped(pressed, ticks = 5)

        // Then
        assertThat(dropTicks).containsExactlyElementsOf(expectedDropTicks)
    }

    @Test
    fun `the shift comes before the rotation in a tick`() {
        // Given a T pointing down at (5, 10), which cannot turn there because of a cell at (5, 9), but can at (4, 10)
        val board = Board.parse(".....O....\n" + List(10) { ".........." }.joinToString("\n"))
        val pressed = engine.press(gameState(board = board, phase = Phase.Falling(T_AT_5_10)), Button.LEFT, Button.A)

        // When
        val ticked = engine.tick(pressed).state

        // Then
        assertThat(ticked.current).isEqualTo(Piece(PieceType.T, Orientation(cellsOf(T_LEFT)), Position(4, 10)))
    }

    @Test
    fun `the rotation comes before the drop in a tick`() {
        // Given a J pointing down at (5, 10), due to drop on this tick: a cell at (6, 12) stops it, not the J turned
        val board = Board.parse("......O...\n" + List(7) { ".........." }.joinToString("\n"))
        val jDown = Piece(PieceType.J, Orientation(cellsOf(J_DOWN)), Position(5, 10))
        val pressed = engine.press(
            gameState(board = board, phase = Phase.Falling(jDown, ticksSinceDrop = 47)),
            Button.A,
        )

        // When
        val ticked = engine.tick(pressed)

        // Then the J turned, then dropped
        assertThat(ticked.events).isEmpty()
        assertThat(ticked.state.current).isEqualTo(Piece(PieceType.J, Orientation(cellsOf(J_LEFT)), Position(5, 11)))
    }

    @Test
    fun `a rotation against the wall is refused, and a new press of B on the next tick turns the piece back`() {
        // Given a T pointing right against the left wall: turned clockwise it would leave the board
        val tRight = Piece(PieceType.T, Orientation(cellsOf(T_RIGHT)), Position(0, 10))
        val againstTheWall = engine.tick(engine.press(gameState(phase = Phase.Falling(tRight)), Button.A)).state

        // When, away from the wall, A is pressed, then B on the next tick
        val turnedOnce = engine.tick(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.A)).state
        val turnedBack = engine.tick(engine.press(turnedOnce, Button.B)).state

        // Then
        assertThat(againstTheWall.current).isEqualTo(tRight)
        assertThat(turnedOnce.current).isEqualTo(Piece(PieceType.T, Orientation(cellsOf(T_LEFT)), Position(5, 10)))
        assertThat(turnedBack.current).isEqualTo(T_AT_5_10)
    }

    @Test
    fun `A and B pressed together turn the piece once, clockwise`() {
        // Given
        val pressed = engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.A, Button.B)

        // When
        val ticked = engine.tick(pressed).state

        // Then
        assertThat(ticked.current).isEqualTo(Piece(PieceType.T, Orientation(cellsOf(T_LEFT)), Position(5, 10)))
    }

    @Test
    fun `a held down freezes the shift repeat, which goes on where it stood when down is released`() {
        // Given left held for 10 ticks (one shift, the count at 9), then down held for 4 ticks, then released
        val leftHeld = engine.ticks(engine.press(gameState(phase = Phase.Falling(T_AT_5_10)), Button.LEFT), 10)
        val downHeld = engine.ticks(engine.press(leftHeld, Button.DOWN), 4)
        val downReleased = engine.apply(downHeld, Input.Release(Button.DOWN)).state

        // When
        val shiftTicks = ticksWhereThePieceMoved(downReleased, ticks = 8)

        // Then the next shift comes on the 16th held tick of left, the 7th tick after the release
        assertThat(downHeld.current?.pivot).isEqualTo(Position(4, 10))
        assertThat(shiftTicks).containsExactly(6)
    }

    @Test
    fun `once the game is over, an input changes nothing`() {
        // Given
        val over = gameState(phase = Phase.GameOver(spawned(PieceType.T)))

        // When
        val pressed = engine.apply(over, Input.Press(Button.LEFT))
        val ticked = engine.tick(pressed.state)

        // Then
        assertThat(pressed.state).isEqualTo(over)
        assertThat(pressed.events).isEmpty()
        assertThat(ticked.state).isEqualTo(over)
    }

    /**
     * Given a T pointing down with its pivot on row 18, due to drop, on [board], and a J next: the state once
     * the T locked on the first tick, checked to be [into], the line clear or the entry delay.
     */
    private fun lockedOnTheFirstTick(
        board: Board,
        into: Class<out Phase>,
    ): GameState {
        val falling = Phase.Falling(tPointingDown(at = Position(5, 18)), ticksSinceDrop = 47)
        val locked = nextPieceEngine.tick(gameState(board = board, next = PieceType.J, phase = falling)).state
        assertThat(locked.phase).isInstanceOf(into)
        return locked
    }

    /**
     * When: ticks of [nextPieceEngine] from [state] until the piece in play, if any, locks and the next one
     * appears; the state where it appears, before it falls.
     */
    private fun firstStateWithTheNextPiece(state: GameState): GameState =
        generateSequence(state) { nextPieceEngine.tick(it).state }
            .dropWhile { it.phase is Phase.Falling }
            .first { it.phase is Phase.Falling }

    /**
     * When: [ticks] ticks of [engine], the one that gave [state], from [state]; the ticks, counted from 0, where
     * the piece in play changed column.
     */
    private fun ticksWhereThePieceMoved(
        state: GameState,
        ticks: Int,
        engine: GameEngine = this.engine,
    ): List<Int> = ticksWhereChanged(state, ticks, engine) { it.current?.pivot?.column }

    /**
     * When: [ticks] ticks of [engine], the one that gave [state], from [state]; the ticks, counted from 0, where
     * the piece in play changed row.
     */
    private fun ticksWhereThePieceDropped(
        state: GameState,
        ticks: Int,
        engine: GameEngine = this.engine,
    ): List<Int> = ticksWhereChanged(state, ticks, engine) { it.current?.pivot?.row }

    /** When: [ticks] ticks of [engine] from [state]; the ticks, counted from 0, where [watched] changed. */
    private fun ticksWhereChanged(
        state: GameState,
        ticks: Int,
        engine: GameEngine,
        watched: (GameState) -> Any?,
    ): List<Int> {
        val states = generateSequence(state) { engine.tick(it).state }.take(ticks + 1).toList()
        return states.zipWithNext().mapIndexedNotNull { index, (before, after) ->
            index.takeIf { watched(before) != watched(after) }
        }
    }

    private companion object {
        val T_AT_5_10 = tPointingDown(at = Position(5, 10))

        const val T_LEFT = """
            .T
            T@
            .T
            """

        const val T_RIGHT = """
            T.
            @T
            T.
            """

        const val J_DOWN = """
            J@J
            ..J
            """

        const val J_LEFT = """
            .J
            .@
            JJ
            """

        @JvmStatic
        fun rotations(): List<Arguments> = listOf(
            argumentSet("A turns it clockwise: the T points left", Button.A, T_LEFT),
            argumentSet("B turns it counterclockwise: the T points right", Button.B, T_RIGHT),
        )

        @JvmStatic
        fun boardsToLockOn(): List<Arguments> = listOf(
            argumentSet("a lock without a line clear", Board.empty()),
            argumentSet(
                "a lock that clears 2 rows",
                Board.parse(
                    """
                    III...IIII
                    IIII.IIIII
                    """,
                ),
            ),
        )

        /**
         * A T pointing down, locked with its pivot at (5, 18) on these boards, leads to the phase named, the entry
         * delay at once, or the line clear first. Its place differs from [boardsToLockOn], where the T shifts first.
         */
        private val LOCKS_BEFORE_THE_NEXT_PIECE: List<Triple<String, Board, Class<out Phase>>> = listOf(
            Triple("the entry delay", Board.empty(), Phase.EntryDelay::class.java),
            Triple("the line clear", TWO_ROWS_TO_FILL, Phase.Clearing::class.java),
        )

        /** For each lock before the next piece: the board and the phase that follows. */
        @JvmStatic
        fun locksBeforeTheNextPiece(): List<Arguments> = LOCKS_BEFORE_THE_NEXT_PIECE.map { (phase, board, phaseClass) ->
            argumentSet("during $phase", board, phaseClass)
        }

        @JvmStatic
        fun rotationsBeforeTheNextPiece(): List<Arguments> = pressesBeforeTheNextPiece(Button.A, Button.B)

        @JvmStatic
        fun shiftsBeforeTheNextPiece(): List<Arguments> = pressesBeforeTheNextPiece(Button.LEFT, Button.RIGHT)

        /** Each of [buttons] crossed with each lock before the next piece: the board, the phase, the button. */
        private fun pressesBeforeTheNextPiece(vararg buttons: Button): List<Arguments> =
            LOCKS_BEFORE_THE_NEXT_PIECE.flatMap { (phase, board, phaseClass) ->
                buttons.map { button -> argumentSet("$button pressed during $phase", board, phaseClass, button) }
            }

        @JvmStatic
        fun pressesWithDown(): List<Arguments> = listOf(
            argumentSet("left tapped with down: no soft drop", Button.LEFT, emptyList<Int>()),
            argumentSet("right tapped with down: no soft drop", Button.RIGHT, emptyList<Int>()),
            argumentSet("A tapped with down: the soft drop starts", Button.A, listOf(2, 4)),
            argumentSet("B tapped with down: the soft drop starts", Button.B, listOf(2, 4)),
        )

        /** After 10 ticks of left held, the T stands in column 4; after 10 ticks of right, in column 6. */
        @JvmStatic
        fun newShiftPresses(): List<Arguments> = listOf(
            argumentSet(
                "right pressed while left is held",
                listOf(Button.LEFT),
                listOf(Input.Press(Button.RIGHT)),
                5,
            ),
            argumentSet(
                "left released and pressed again between two ticks",
                listOf(Button.LEFT),
                listOf(Input.Release(Button.LEFT), Input.Press(Button.LEFT)),
                3,
            ),
            argumentSet(
                "left pressed while right is held: right still wins, and shifts at once",
                listOf(Button.RIGHT),
                listOf(Input.Press(Button.LEFT)),
                7,
            ),
        )

        /** After 10 ticks of left and right held, the T stands in column 6; under down, it stays in column 5. */
        @JvmStatic
        fun directionChangesWithoutAPress(): List<Arguments> = listOf(
            argumentSet(
                "right released while left and right are held",
                listOf(Button.LEFT, Button.RIGHT),
                listOf(Input.Release(Button.RIGHT)),
                6,
            ),
            argumentSet(
                "down released while left, pressed under it, is held",
                listOf(Button.DOWN, Button.LEFT),
                listOf(Input.Release(Button.DOWN)),
                5,
            ),
        )
    }
}
