package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
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
     * Makes the next piece of [state] appear on its board, and draws the following one, as [appear]
     * does. It will end the entry delay (P1.12b).
     */
    internal fun spawn(state: GameState): GameState = appear(
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
