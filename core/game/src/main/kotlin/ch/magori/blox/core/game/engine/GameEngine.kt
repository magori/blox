package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
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
public class GameEngine(private val rules: Rules, private val randomizer: Randomizer) {
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
            startLevel = startLevel,
            level = startLevel,
            lines = Lines(0),
            score = Points(0),
            phase = appearance.phase,
            tickCount = 0,
            stackInDanger = false,
            statistics = GameStatistics(),
        )
        return GameUpdate(state, events = emptyList())
    }

    /**
     * Advances [state] by one tick (1/60 s), counted in [GameState.tickCount].
     *
     * - A falling piece drops one row when the ticks per row of the level have passed. If it cannot drop,
     *   it locks on the board ([GameEvent.PieceLocked]), and the stack danger is measured on the board
     *   once cleared.
     * - Without a full row, [GameEvent.StackDanger] follows at once if the danger changed, then the entry
     *   delay computed when the piece locked.
     * - With full rows, they are cleared after an animation, without any event: the board shown keeps
     *   them until then. At the end, the cleared board is shown, the lines and the level go up, the
     *   points are counted at the new level ([GameEvent.LinesCleared]), then [GameEvent.StackDanger] if
     *   the danger changed, so that it changes as the stack goes down on screen. The entry delay follows.
     * - When the entry delay is over, the next piece appears, or the game is over if it does not fit.
     * - Once the game is over, a tick changes nothing, not even [GameState.tickCount].
     *
     * Not pure: the next piece is drawn from the randomizer (ADR 0014). Pass back only the last state
     * returned; the ViewModel is the only caller (ADR 0012).
     */
    public fun tick(state: GameState): GameUpdate = when (val phase = state.phase) {
        is Phase.Falling -> fall(state.counted(), phase)
        is Phase.Clearing -> clear(state.counted(), phase)
        is Phase.EntryDelay -> GameUpdate(wait(state.counted(), phase), events = emptyList())
        is Phase.GameOver -> GameUpdate(state, events = emptyList())
    }

    /** [GameState] with one more tick played. */
    private fun GameState.counted(): GameState = copy(tickCount = tickCount + 1)

    private fun fall(
        state: GameState,
        falling: Phase.Falling,
    ): GameUpdate {
        val ticks = falling.ticksSinceDrop + 1
        if (ticks < rules.gravityTable.ticksPerRow(state.level)) {
            return GameUpdate(state.copy(phase = falling.copy(ticksSinceDrop = ticks)), events = emptyList())
        }
        val dropped = state.board.move(falling.piece, Direction.DOWN) ?: return lock(state, falling.piece)
        return GameUpdate(state.copy(phase = Phase.Falling(dropped)), events = emptyList())
    }

    private fun lock(
        state: GameState,
        piece: Piece,
    ): GameUpdate {
        val locked = state.board.lock(piece)
        val rowsCleared = locked.clearFullRows()
        val entryDelay = rules.entryDelay.ticksAfterLock(piece)
        val inDanger = rules.stackDangerRule.inDanger(state.stackInDanger, rowsCleared.board.stackHeight)
        if (rowsCleared.clearedRows.isNotEmpty()) {
            val clearing = Phase.Clearing(
                rowsCleared,
                ticksLeft = clearDuration(state.tickCount),
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

    /** [GameEvent.StackDanger] if the stack danger changes from the one of [state] to [inDanger], else nothing. */
    private fun dangerChange(
        state: GameState,
        inDanger: Boolean,
    ): List<GameEvent> = if (inDanger !=
        state.stackInDanger
    ) {
        listOf(GameEvent.StackDanger(on = inDanger))
    } else {
        emptyList()
    }

    /**
     * The line clear animation: 16 ticks, plus the ticks up to the next multiple of 4 of [lockTick], the
     * number of the tick the piece locked on (1 to 4 ticks): 17 to 20 ticks in all. [to be checked]
     */
    private fun clearDuration(lockTick: Int): Int = CLEAR_ANIMATION_TICKS + (CLEAR_STEP - lockTick % CLEAR_STEP)

    private fun clear(
        state: GameState,
        clearing: Phase.Clearing,
    ): GameUpdate {
        if (clearing.ticksLeft > 1) {
            return GameUpdate(
                state.copy(phase = clearing.copy(ticksLeft = clearing.ticksLeft - 1)),
                events = emptyList(),
            )
        }
        val lineClear = clearing.rowsCleared.lineClear
        val lines = state.lines + lineClear
        val level = rules.levelProgression.levelFor(state.startLevel, lines)
        val cleared = state.copy(
            board = clearing.rowsCleared.board,
            lines = lines,
            level = level,
            score = state.score + rules.scoringRule.pointsForLinesCleared(lineClear, level),
            phase = Phase.EntryDelay(clearing.entryDelayAfter),
            stackInDanger = clearing.stackInDangerAfter,
            statistics = state.statistics.afterClear(lineClear),
        )
        val events = listOf(GameEvent.LinesCleared(lineClear)) + dangerChange(state, clearing.stackInDangerAfter)
        return GameUpdate(cleared, events)
    }

    private fun wait(
        state: GameState,
        delay: Phase.EntryDelay,
    ): GameState = if (delay.ticksLeft > 1) state.copy(phase = Phase.EntryDelay(delay.ticksLeft - 1)) else spawn(state)

    /** At the end of the entry delay, the next piece of [state] appears: see [appearance]. */
    private fun spawn(state: GameState): GameState {
        val appearance = appearance(state.board, type = state.next)
        return state.copy(next = appearance.next, phase = appearance.phase)
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

    private companion object {
        /** The fixed part of the line clear animation, in ticks. */
        const val CLEAR_ANIMATION_TICKS = 16

        /** The animation ends on a multiple of this number of ticks. */
        const val CLEAR_STEP = 4
    }
}
