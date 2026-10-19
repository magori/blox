package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.tPointingDown

/** Rows 18 and 19, which a T pointing down with its pivot at (5, 18) fills. */
internal val TWO_ROWS_TO_FILL: Board = Board.parse(
    """
    IIII...III
    IIIII.IIII
    """,
)

/**
 * The lines that bring a game started at level 0 to level 29 in the Classic mode, where the piece drops one
 * row on every tick.
 */
internal val LINES_TO_LEVEL_29: Lines = Lines(290)

/**
 * A game state for tests: an empty board, a T falling high on it, in a game started at level 0 with no line,
 * unless told otherwise. The level is the one the Classic level progression gives for [startLevel] and
 * [lines], so that the state is one a game can reach.
 */
internal fun gameState(
    board: Board = Board.empty(),
    next: PieceType = PieceType.T,
    startLevel: Level = Level(0),
    lines: Lines = Lines(0),
    tickCount: Int = 0,
    stackInDanger: Boolean = false,
    paused: Boolean = false,
    phase: Phase = Phase.Falling(tPointingDown(at = Position(5, 0))),
): GameState = GameState(
    board = board,
    next = next,
    progress = GameProgress.of(startLevel, lines, ClassicRules.levelProgression),
    phase = phase,
    tickCount = tickCount,
    stackInDanger = stackInDanger,
    controls = Controls.NONE,
    paused = paused,
)

/** The state after [count] ticks from [state]. */
internal fun GameEngine.ticks(
    state: GameState,
    count: Int,
): GameState = (1..count).fold(state) { current, _ -> tick(current).state }

/** The state once [buttons] are pressed, in turn, from [state]. */
internal fun GameEngine.press(
    state: GameState,
    vararg buttons: Button,
): GameState = buttons.fold(state) { current, button -> apply(current, Input.Press(button)).state }
