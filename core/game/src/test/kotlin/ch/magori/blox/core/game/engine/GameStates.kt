package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.tPointingDown
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A game state for tests: an empty board, a T falling high on it, at level 0, unless told otherwise.
 * The start level stays 0.
 */
internal fun gameState(
    board: Board = Board.empty(),
    next: PieceType = PieceType.T,
    level: Level = Level(0),
    lines: Lines = Lines(0),
    tickCount: Int = 0,
    stackInDanger: Boolean = false,
    phase: Phase = Phase.Falling(tPointingDown(at = Position(5, 0))),
): GameState = GameState(
    board = board,
    next = next,
    startLevel = Level(0),
    level = level,
    lines = lines,
    score = Points(0),
    phase = phase,
    tickCount = tickCount,
    stackInDanger = stackInDanger,
    statistics = GameStatistics(),
)

/** A piece of [type] as it appears in the Classic mode: pivot at (5, 0), in its spawn orientation. */
internal fun spawned(type: PieceType): Piece = Piece(type, ClassicRotationSystem.spawnOrientation(type), Position(5, 0))

/** The state after [count] ticks from [state]. */
internal fun GameEngine.ticks(
    state: GameState,
    count: Int,
): GameState = (1..count).fold(state) { current, _ -> tick(current).state }
