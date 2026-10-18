package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
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
        val state = appear(
            board = Board.empty(),
            type = randomizer.next(previous = null),
            startLevel = startLevel,
            level = startLevel,
            lines = Lines(0),
            score = 0,
        )
        return GameUpdate(state, events = emptyList())
    }

    /**
     * Advances [state] by one tick (1/60 s). A falling piece drops one row when the ticks per row of
     * the level have passed; if it cannot drop, it locks on the board ([GameEvent.PieceLocked]) and the
     * entry delay of the rules begins. When the delay is over, the next piece appears, or the game is
     * over if it does not fit.
     *
     * Not pure: the next piece is drawn from the randomizer (ADR 0014). Pass back only the last state
     * returned; the ViewModel is the only caller (ADR 0012).
     */
    public fun tick(state: GameState): GameUpdate = when (val phase = state.phase) {
        is Phase.Falling -> fall(state, phase)
        is Phase.EntryDelay -> GameUpdate(wait(state, phase), events = emptyList())
        is Phase.GameOver -> GameUpdate(state, events = emptyList())
    }

    private fun fall(
        state: GameState,
        falling: Phase.Falling,
    ): GameUpdate {
        val ticks = falling.ticksSinceDrop + 1
        if (ticks < rules.gravityTable.ticksPerRow(state.level)) {
            return GameUpdate(state.copy(phase = falling.copy(ticksSinceDrop = ticks)), events = emptyList())
        }
        val dropped = state.board.move(falling.piece, Direction.DOWN)
            ?: return GameUpdate(
                state.copy(
                    board = state.board.lock(falling.piece),
                    phase = Phase.EntryDelay(rules.entryDelay.ticksAfterLock(falling.piece)),
                ),
                events = listOf(GameEvent.PieceLocked(falling.piece)),
            )
        return GameUpdate(state.copy(phase = Phase.Falling(dropped)), events = emptyList())
    }

    private fun wait(
        state: GameState,
        delay: Phase.EntryDelay,
    ): GameState = if (delay.ticksLeft > 1) state.copy(phase = Phase.EntryDelay(delay.ticksLeft - 1)) else spawn(state)

    /**
     * At the end of the entry delay, the next piece of [state] appears and the following one is drawn:
     * see [appear].
     */
    private fun spawn(state: GameState): GameState = appear(
        board = state.board,
        type = state.next,
        startLevel = state.startLevel,
        level = state.level,
        lines = state.lines,
        score = state.score,
    )

    /**
     * A piece of [type] appears: its pivot at the spawn pivot of the rules, in the spawn orientation of
     * the rotation system. The following piece is drawn with [type] as previous. If the piece does not
     * fit on [board], the game is over.
     */
    private fun appear(
        board: Board,
        type: PieceType,
        startLevel: Level,
        level: Level,
        lines: Lines,
        score: Int,
    ): GameState {
        val piece = Piece(type, rules.rotationSystem.spawnOrientation(type), rules.spawnPivot)
        return GameState(
            board = board,
            next = randomizer.next(previous = type),
            startLevel = startLevel,
            level = level,
            lines = lines,
            score = score,
            phase = if (board.fits(piece)) Phase.Falling(piece) else Phase.GameOver(piece),
        )
    }
}
