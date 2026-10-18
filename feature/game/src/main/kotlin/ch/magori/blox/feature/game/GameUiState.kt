package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.gameover.GameOverUiState

/**
 * What the game screen shows. The domain values pass as they are: the screen draws them. [gameOver] is
 * set once the game is over and its score submitted to the records: the screen then shows the game over.
 */
@Immutable
internal data class GameUiState(
    val board: Board,
    val piece: Piece?,
    val next: PieceType,
    val level: Level,
    val lines: Lines,
    val score: Points,
    val rotationSystem: RotationSystem,
    val gameOver: GameOverUiState? = null,
)

/**
 * The screen state of [this] game state. The only place that reads [GameState]: a change of the
 * engine (P1.12c: points, phases) is adapted here.
 */
internal fun GameState.toUiState(rotationSystem: RotationSystem): GameUiState = GameUiState(
    board = board,
    piece = current,
    next = next,
    level = progress.level,
    lines = progress.lines,
    score = progress.score,
    rotationSystem = rotationSystem,
)

/** The game over screen state of [this] final state, whose score made a [newRecord] or not. */
internal fun GameState.toGameOverUiState(newRecord: Boolean): GameOverUiState = GameOverUiState(
    score = progress.score.value,
    lines = progress.lines.value,
    level = progress.level.value,
    fourLineClears = progress.statistics.fourLineClears,
    ticksPlayed = tickCount,
    newRecord = newRecord,
)
