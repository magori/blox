package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.model.Ticks
import ch.magori.blox.core.game.rules.Randomizer

/**
 * The engine of one game: it starts the game and makes it evolve, with the [rules] of its mode and
 * nothing else. Each call gives a new [GameState] and the [GameEvent]s of that step, as a
 * [GameUpdate]; it never changes a state.
 *
 * One engine plays one game. Its [randomizer] is the only mutable part of the domain (ADR 0014) and
 * belongs to that game: give the engine back only the last state it returned, and create a new
 * engine, with a [GameEngineFactory], for each new game.
 */
public class GameEngine(public val rules: Rules, private val randomizer: Randomizer) {
    /**
     * Starts the game at [startLevel]: an empty board, no line, no point, and a first piece in play.
     * Two pieces are drawn: the piece in play, with no previous piece, then the next one, with the
     * piece in play as previous. Starting gives no event.
     *
     * @throws IllegalArgumentException if [startLevel] is not one of the starting levels of the rules.
     */
    public fun start(startLevel: Level): GameUpdate {
        rules.levelProgression.requireStartLevel(startLevel)
        val board = Board.empty()
        val appearance = appearance(board, type = randomizer.next(previous = null))
        val state = GameState(
            board = board,
            next = appearance.next,
            progress = GameProgress.startingAt(startLevel, rules.levelProgression),
            phase = appearance.phase,
            tickCount = 0,
            stackInDanger = false,
            controls = Controls.NONE,
            paused = false,
        )
        return GameUpdate(state, events = emptyList())
    }

    /**
     * Advances [state] by one tick (1/60 s), counted in [GameState.tickCount].
     *
     * - The piece in play reads the controller, in the order of the NES: the shift first, then the
     *   rotation, then the drop.
     *   - Shift: right wins over left; a held shift repeats ([Rules.inputRepeat]), and a new press or a
     *     change of direction starts the repeat again; a shift refused by a wall or the stack charges it,
     *     so the piece moves as soon as the way is free. A held down blocks the shift.
     *   - Rotation: A turns clockwise, B counterclockwise, once per press, even a press released before the
     *     tick; the first candidate of the rotation system that fits wins. A piece that turns sends
     *     [GameEvent.PieceRotated], first among the events of the tick.
     *   - Drop: at most one row per tick. A new press of down starts the soft drop ([Rules.inputRepeat]),
     *     which goes on while down is held alone and starts the gravity count again at each step. A held
     *     left or right, or a release of down, stops it and its points; it then needs a new press of down.
     *     Otherwise the piece drops one row when the ticks per row of the level have passed.
     * - A piece that cannot drop locks on the board ([GameEvent.PieceLocked]), whether the soft drop or the
     *   gravity moved it; the rows it soft dropped in a row score. The stack danger is measured on the
     *   board once cleared.
     * - Without a full row, [GameEvent.StackDanger] follows at once if the danger changed, then the entry
     *   delay computed when the piece locked.
     * - With full rows, they are cleared after an animation, as long as [Rules.lineClearDelay] says,
     *   without any event: the board shown keeps them until then. At the end, the cleared board is shown,
     *   the lines and the level go up, the points are counted at the new level ([GameEvent.LinesCleared]),
     *   then [GameEvent.LevelUp] if the level went up, then [GameEvent.StackDanger] if the danger changed, so
     *   that it changes as the stack goes down on screen. The entry delay follows.
     * - When the entry delay is over, the next piece appears, or the game is over if it does not fit. The
     *   held shift stands still from the lock until then, and goes on with the next piece; a down held
     *   through the lock does not soft drop the next piece until it is pressed again. A press made during the
     *   line clear or the entry delay is lost, as in the ROM: a button pressed then and still held is only
     *   held for the next piece. A or B does not turn it; left or right does not start the shift repeat
     *   again: the shift goes on with its count where it stood, or from 0 if there is none yet.
     * - Once the game is over, or while it is paused, a tick changes nothing, not even [GameState.tickCount].
     *
     * Not pure: the next piece is drawn from the randomizer (ADR 0014). Pass back only the last state
     * returned; the ViewModel is the only caller (ADR 0012).
     */
    public fun tick(state: GameState): GameUpdate {
        if (state.paused) return GameUpdate(state, events = emptyList())
        val update = when (val phase = state.phase) {
            is Phase.Falling -> fall(state.counted(), phase)

            is Phase.Clearing ->
                countDown(state.counted(), phase.ticksLeft, { phase.copy(ticksLeft = it) }) { endClear(it, phase) }

            is Phase.EntryDelay -> countDown(state.counted(), phase.ticksLeft, { phase.copy(ticksLeft = it) }, ::spawn)

            is Phase.GameOver -> return GameUpdate(state, events = emptyList())
        }
        return update.copy(state = update.state.copy(controls = update.state.controls.afterTick()))
    }

    /**
     * Records [input] in [state]: a press or a release goes to the controller, and nothing moves until the
     * next [tick], which reads it. Gives no event.
     *
     * [Input.Pause] pauses the game and forgets the held buttons and the shift repeat, so that nothing
     * resumes charged: a button still held must be pressed again. While paused, only [Input.Resume] counts,
     * and the game goes on exactly where it stood. Once the game is over, an input changes nothing.
     */
    public fun apply(
        state: GameState,
        input: Input,
    ): GameUpdate {
        val applied = when {
            state.phase is Phase.GameOver -> state

            state.paused -> if (input == Input.Resume) state.copy(paused = false) else state

            else -> when (input) {
                is Input.Press -> state.copy(controls = state.controls.afterPress(input.button))
                is Input.Release -> state.copy(controls = state.controls.afterRelease(input.button))
                Input.Pause -> state.copy(paused = true, controls = Controls.NONE)
                Input.Resume -> state
            }
        }
        return GameUpdate(applied, events = emptyList())
    }

    /** [GameState] with one more tick played. */
    private fun GameState.counted(): GameState = copy(tickCount = tickCount + 1)

    /**
     * A tick of the piece in play: the shift, then the rotation, then the drop, as the NES reads them. A piece
     * that turned sends [GameEvent.PieceRotated] before the events of the drop.
     */
    private fun fall(
        state: GameState,
        falling: Phase.Falling,
    ): GameUpdate {
        val (controls, shifted) = shift(state.board, state.controls, falling.piece)
        val rotation = rotationOf(controls)
        val turned = falling.copy(piece = rotation?.let { rotate(state.board, it, shifted) } ?: shifted)
        val dropped = drop(state.copy(controls = controls), turned)
        if (rotation == null || turned.piece == shifted) return dropped
        return dropped.copy(events = listOf(GameEvent.PieceRotated(turned.piece, rotation)) + dropped.events)
    }

    /**
     * The drop on this tick, one row at most: a soft drop step if one is due, else the gravity.
     *
     * As in the ROM (meatfighter.com, `autorepeatY`), the soft drop starts only on a new press of down,
     * and goes on while down is held without left or right. Anything else stops it and its points; it
     * then needs a new press of down, so a down held through a lock does not drop the next piece.
     */
    private fun drop(
        state: GameState,
        falling: Phase.Falling,
    ): GameUpdate {
        val held = state.controls.held
        val downOnly = Button.DOWN in held && Button.LEFT !in held && Button.RIGHT !in held
        val repeat = when {
            !downOnly -> null

            state.controls.pressedSinceLastTick.let {
                Button.DOWN in it && Button.LEFT !in it && Button.RIGHT !in it
            } ->
                AutoRepeat(rules.inputRepeat.softDrop)

            else -> falling.softDrop
        }
        val softDrop = repeat?.tick(pressed = true)
        val counted = if (softDrop == null) {
            falling.copy(softDrop = null, softDropRows = 0)
        } else {
            falling.copy(softDrop = softDrop.next)
        }
        if (softDrop?.step == true) return dropOneRow(state, counted) { it.copy(softDropRows = it.softDropRows + 1) }
        val ticks = counted.ticksSinceDrop + 1
        if (ticks < rules.gravityTable.ticksPerRow(state.progress.level).value) {
            return GameUpdate(state.copy(phase = counted.copy(ticksSinceDrop = ticks)), events = emptyList())
        }
        return dropOneRow(state, counted) { it }
    }

    /**
     * The piece of [falling] drops one row, which starts the gravity count again, then [afterDrop] counts
     * the drop; if it cannot drop, it locks.
     */
    private fun dropOneRow(
        state: GameState,
        falling: Phase.Falling,
        afterDrop: (Phase.Falling) -> Phase.Falling,
    ): GameUpdate {
        val dropped = state.board.move(falling.piece, Direction.DOWN) ?: return lock(state, falling)
        return GameUpdate(
            state.copy(phase = afterDrop(falling.copy(piece = dropped, ticksSinceDrop = 0))),
            events = emptyList(),
        )
    }

    /**
     * The shift on this tick: the controls with the repeat moved on, and the piece, shifted if a step is due.
     *
     * As in the ROM (meatfighter.com, `$89B8`): a held down blocks the shift and freezes its repeat; right
     * wins over left; a single repeat serves both directions, and only a new press of left or right starts
     * it again, so a change of direction without a new press goes on with the count. A refused step
     * charges the repeat.
     */
    private fun shift(
        board: Board,
        controls: Controls,
        piece: Piece,
    ): Pair<Controls, Piece> {
        if (Button.DOWN in controls.held) return controls to piece
        val direction = when {
            Button.RIGHT in controls.held -> Direction.RIGHT
            Button.LEFT in controls.held -> Direction.LEFT
            else -> return controls to piece
        }
        val newPress = Button.LEFT in controls.pressedSinceLastTick || Button.RIGHT in controls.pressedSinceLastTick
        val repeat = when {
            newPress -> AutoRepeat(rules.inputRepeat.shift)
            else -> controls.shift ?: AutoRepeat.heldWithoutPress(rules.inputRepeat.shift)
        }.tick(pressed = true)
        if (!repeat.step) return controls.copy(shift = repeat.next) to piece
        val shifted = board.move(piece, direction) ?: return controls.copy(shift = repeat.next.charged()) to piece
        return controls.copy(shift = repeat.next) to shifted
    }

    /** The rotation asked on this tick, on a new press only: A turns clockwise, else B counterclockwise; null for none. */
    private fun rotationOf(controls: Controls): Rotation? = when {
        Button.A in controls.pressedSinceLastTick -> Rotation.CLOCKWISE
        Button.B in controls.pressedSinceLastTick -> Rotation.COUNTERCLOCKWISE
        else -> null
    }

    /**
     * [piece] turned by [rotation]: the first candidate of the rotation system that fits wins; if none fits, the piece
     * stays as it was.
     */
    private fun rotate(
        board: Board,
        rotation: Rotation,
        piece: Piece,
    ): Piece = rules.rotationSystem.candidates(piece, rotation).firstNotNullOfOrNull(board::place) ?: piece

    /**
     * The piece of [falling] could not drop: it locks on the board, and the rows it soft dropped in a row
     * score. Then comes the line clear, if it fills rows, or the entry delay.
     */
    private fun lock(
        stateBeforeLock: GameState,
        falling: Phase.Falling,
    ): GameUpdate {
        val piece = falling.piece
        val softDropPoints = rules.scoringRule.pointsForSoftDrop(falling.softDropRows)
        val state = stateBeforeLock.copy(progress = stateBeforeLock.progress.withPoints(softDropPoints))
        val locked = state.board.lock(piece)
        val rowsCleared = locked.clearFullRows()
        val entryDelay = rules.entryDelay.ticksAfterLock(piece)
        val inDanger = rules.stackDangerRule.inDanger(state.stackInDanger, rowsCleared.board.stackHeight)
        if (rowsCleared.clearedRows.isNotEmpty()) {
            val clearing = Phase.Clearing(
                rowsCleared,
                ticksLeft = rules.lineClearDelay.ticksToClear(lockTick = state.tickCount),
                entryDelayAfter = entryDelay,
                stackInDangerAfter = inDanger,
            )
            return GameUpdate(
                state.copy(board = locked, phase = clearing),
                events = listOf(GameEvent.PieceLocked(piece)),
            )
        }
        val waiting = state.copy(board = locked, phase = Phase.EntryDelay(entryDelay), stackInDanger = inDanger)
        return GameUpdate(waiting, events = listOf(GameEvent.PieceLocked(piece)) + dangerChange(state, inDanger))
    }

    /** [GameEvent.LevelUp] if the level of [after] is not the one of [before], else nothing. */
    private fun levelUp(
        before: GameState,
        after: GameState,
    ): List<GameEvent> {
        val level = after.progress.level
        return if (level != before.progress.level) listOf(GameEvent.LevelUp(level)) else emptyList()
    }

    /** [GameEvent.StackDanger] if the stack danger changes from the one of [state] to [inDanger], else nothing. */
    private fun dangerChange(
        state: GameState,
        inDanger: Boolean,
    ): List<GameEvent> {
        val changed = inDanger != state.stackInDanger
        return if (changed) listOf(GameEvent.StackDanger(on = inDanger)) else emptyList()
    }

    /**
     * A tick of a wait with [ticksLeft] to go, the line clear or the entry delay: on its last tick, [over]
     * ends it; before, nothing happens but the wait going on, in the phase [waiting] gives for what is left.
     */
    private inline fun countDown(
        state: GameState,
        ticksLeft: Ticks,
        waiting: (Ticks) -> Phase,
        over: (GameState) -> GameUpdate,
    ): GameUpdate {
        val left = ticksLeft.afterOneTick() ?: return over(state)
        return GameUpdate(state.copy(phase = waiting(left)), events = emptyList())
    }

    /**
     * The last tick of the line clear of [clearing]: the cleared board is shown, the lines, the level and the
     * points are counted, the stack danger changes if it must, and the entry delay begins. Its events, in
     * order: [GameEvent.LinesCleared], [GameEvent.LevelUp] if the level went up, [GameEvent.StackDanger] if
     * the danger changed.
     */
    private fun endClear(
        state: GameState,
        clearing: Phase.Clearing,
    ): GameUpdate {
        val lineClear = clearing.rowsCleared.lineClear
        val cleared = state.copy(
            board = clearing.rowsCleared.board,
            progress = state.progress.afterClear(lineClear, rules),
            phase = Phase.EntryDelay(clearing.entryDelayAfter),
            stackInDanger = clearing.stackInDangerAfter,
        )
        val events = listOf(GameEvent.LinesCleared(lineClear)) + levelUp(state, cleared) +
            dangerChange(state, clearing.stackInDangerAfter)
        return GameUpdate(cleared, events)
    }

    /**
     * At the end of the entry delay, the next piece of [state] appears: see [appearance]. If it does not fit,
     * the game is over, and [GameEvent.GameOver] is the only event of that tick.
     */
    private fun spawn(state: GameState): GameUpdate {
        val appearance = appearance(state.board, type = state.next)
        val events = if (appearance.phase is Phase.GameOver) listOf(GameEvent.GameOver) else emptyList()
        return GameUpdate(state.copy(next = appearance.next, phase = appearance.phase), events)
    }

    /**
     * A piece of [type] appears on [board]: its pivot at the spawn pivot of the rules, in the spawn
     * orientation of the rotation system. Gives the phase that follows, falling or, if the piece does not
     * fit, game over, and the following piece, drawn with [type] as previous.
     */
    private fun appearance(
        board: Board,
        type: PieceType,
    ): Appearance {
        val piece = Piece(type, rules.rotationSystem.spawnOrientation(type), rules.spawnPivot)
        val phase = if (board.fits(piece)) Phase.Falling(piece) else Phase.GameOver(piece)
        return Appearance(phase, next = randomizer.next(previous = type))
    }

    /** What an appearance gives: the [phase] that follows, and the [next] piece drawn. */
    private data class Appearance(val phase: Phase, val next: PieceType)
}
