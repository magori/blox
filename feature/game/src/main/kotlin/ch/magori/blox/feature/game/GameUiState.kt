package ch.magori.blox.feature.game

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.game.engine.GameState
import ch.magori.blox.core.game.engine.Phase
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.gameover.GameOverUiState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * What the game screen shows. The domain values pass as they are: the screen draws them. The [next] piece
 * comes as its cells, so that the screen needs no rule to draw it. [gameOver] is set once the game is over
 * and its score submitted to the records: the screen then shows the game over. [running] says whether the
 * game runs, neither paused nor over: the screen hands frames only then (ADR 0012), and draws none for nothing
 * on the pause or the game over screen; it turns false as soon as the engine ends the game, before the records
 * answer. [leftHanded] lays the keys out for the left hand: B and A on the left, the arrows on the right; null while the
 * settings have not answered, when the screen shows no key and the game does not run.
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
    val recordBeaten: Boolean = false,
    val gameOver: GameOverUiState? = null,
    val leftHanded: Boolean? = false,
)

/**
 * [this] state, of a game that runs as [underWay] says, for the keys laid out as [leftHanded] says: the game runs only
 * once it is known, so that no key is shown, nor any tick played, before the keys stand where the player wants them.
 */
internal fun GameUiState.withLeftHanded(
    leftHanded: Boolean?,
    underWay: Boolean,
): GameUiState = copy(leftHanded = leftHanded, running = underWay && leftHanded != null)

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

/**
 * [this] state with its [GameUiState.recordBeaten] up to date: beaten once its score is above a record above 0, and
 * from then on, as [wasBeaten] says, even once the game over saves the score as the record. A record of 0, or not
 * read yet, is never beaten during the game: the first point would beat it (P5.56).
 */
internal fun GameUiState.withRecordBeaten(wasBeaten: Boolean): GameUiState {
    val beaten = wasBeaten || (record.isRecord() && score.value > record)
    return if (beaten == recordBeaten) this else copy(recordBeaten = beaten)
}

/**
 * Whether [this] record, from the records, is one: above 0. A record of 0, or not read yet, is none: the first point
 * would beat it (P5.56).
 */
@OptIn(ExperimentalContracts::class)
internal fun Int?.isRecord(): Boolean {
    contract { returns(true) implies (this@isRecord != null) }
    return this != null && this > 0
}

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
