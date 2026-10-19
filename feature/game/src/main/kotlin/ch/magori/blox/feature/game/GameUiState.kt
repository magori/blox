package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.gameover.GameOverUiState
import ch.magori.blox.feature.game.sidepanel.NextPieceCells

/**
 * What the game screen shows. The domain values pass as they are: the screen draws them. The [next] piece
 * comes as its cells, so that the screen needs no rule to draw it. [gameOver] is set once the game is over
 * and its score submitted to the records: the screen then shows the game over. [running] says whether the
 * game runs, neither paused nor over: the screen hands frames only then (ADR 0012), and draws none for nothing
 * on the pause or the game over screen; it turns false as soon as the engine ends the game, before the records
 * answer.
 */
@Immutable
internal data class GameUiState(
    val board: Board,
    val piece: Piece?,
    val next: NextPieceCells,
    val level: Level,
    val lines: Lines,
    val score: Points,
    val paused: Boolean = false,
    val running: Boolean = !paused,
    val record: Int? = null,
    val gameOver: GameOverUiState? = null,
)

/**
 * The screen state of [this] game state, its next piece drawn with the cells [nextPiece] gives for its type: the
 * screen never reads [GameState] itself, nor the rules. The view model reads it too (whether the game is
 * paused or over, the score it submits to the records), and the game over screen state comes from
 * [toGameOverUiState].
 */
internal fun GameState.toUiState(nextPiece: (PieceType) -> NextPieceCells): GameUiState = GameUiState(
    board = board,
    piece = current,
    next = nextPiece(next),
    level = progress.level,
    lines = progress.lines,
    score = progress.score,
    paused = paused,
    running = isUnderWay,
)

/** Whether [this] game goes on: neither paused nor over. */
internal val GameState.isUnderWay: Boolean get() = !paused && phase !is Phase.GameOver

/** The game over screen state of [this] final state, whose score made a [newRecord] or not. */
internal fun GameState.toGameOverUiState(newRecord: Boolean): GameOverUiState = GameOverUiState(
    score = progress.score,
    lines = progress.lines,
    level = progress.level,
    fourLineClears = progress.statistics.fourLineClears,
    ticksPlayed = tickCount,
    newRecord = newRecord,
)
