package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.tPointingDown

/** A game state for tests: an empty board, a T falling high on it, at level 0, unless told otherwise. */
internal fun gameState(
    board: Board = Board.empty(),
    next: PieceType = PieceType.T,
    phase: Phase = Phase.Falling(tPointingDown(at = Position(5, 0))),
): GameState = GameState(
    board = board,
    next = next,
    startLevel = Level(0),
    level = Level(0),
    lines = Lines(0),
    score = 0,
    phase = phase,
)
