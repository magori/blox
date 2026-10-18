package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.RotationSystem

/** What the game screen shows. The domain values pass as they are: the screen draws them. */
@Immutable
internal data class GameUiState(
    val board: Board,
    val piece: Piece?,
    val next: PieceType,
    val level: Level,
    val lines: Lines,
    val score: Int,
    val rotationSystem: RotationSystem,
)

/**
 * The screen state of [this] game state. The only place that reads [GameState]: a change of the
 * engine (P1.12c: points, phases) is adapted here.
 */
internal fun GameState.toUiState(rotationSystem: RotationSystem): GameUiState = GameUiState(
    board = board,
    piece = current,
    next = next,
    level = level,
    lines = lines,
    score = score,
    rotationSystem = rotationSystem,
)
