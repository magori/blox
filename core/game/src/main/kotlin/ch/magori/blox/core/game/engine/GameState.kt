package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType

/**
 * The state of a game at one tick. Immutable: the engine gives a new state for each change. Only the
 * engine creates one, so a state is always one the game can reach.
 *
 * @property board the locked cells.
 * @property next the piece shown in the preview, which appears after the piece in play.
 * @property startLevel the level chosen at the start, from which the level of [progress] is computed.
 * @property progress the level, the lines, the score and the statistics.
 * @property phase where the game stands, with the piece in play.
 * @property tickCount the ticks played since the start, which give the length of the game: 0 when the game
 *   starts, then each [GameEngine.tick] adds 1 before it does anything else, so the tick being played
 *   counts. It no longer changes once the game is over.
 * @property stackInDanger whether the stack is high enough to be in danger, as the stack danger rule of the
 *   rules measured it when the last piece locked, on the board once its full rows are cleared. While those
 *   rows are being cleared, it keeps its former value and takes the new one when the cleared board is shown.
 * @property controls the state of the controller. It lives here, not in the phase, so that a held button
 *   outlives the lock of a piece.
 */
@ConsistentCopyVisibility
public data class GameState internal constructor(
    public val board: Board,
    public val next: PieceType,
    public val startLevel: Level,
    public val progress: GameProgress,
    public val phase: Phase,
    public val tickCount: Int,
    public val stackInDanger: Boolean,
    public val controls: Controls,
) {
    /** The piece in play, or shown at the end of the game; null when there is none (line clear, entry delay). */
    public val current: Piece?
        get() = when (phase) {
            is Phase.Falling -> phase.piece
            is Phase.GameOver -> phase.piece
            is Phase.EntryDelay -> null
            is Phase.Clearing -> null
        }
}
