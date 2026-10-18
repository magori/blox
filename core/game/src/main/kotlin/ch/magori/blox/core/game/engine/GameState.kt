package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType

/**
 * The state of a game at one tick. Immutable: the engine gives a new state for each change. Only the
 * engine creates one, so a state is always one the game can reach.
 *
 * @property board the locked cells.
 * @property next the piece shown in the preview, which appears after the piece in play.
 * @property startLevel the level chosen at the start; [level] is computed from it and [lines].
 * @property level the current level.
 * @property lines the lines cleared since the start.
 * @property score the points earned since the start.
 * @property phase where the game stands, with the piece in play.
 */
@ConsistentCopyVisibility
public data class GameState internal constructor(
    public val board: Board,
    public val next: PieceType,
    public val startLevel: Level,
    public val level: Level,
    public val lines: Lines,
    public val score: Int,
    public val phase: Phase,
) {
    /** The piece in play, or shown at the end of the game; null when there is none (entry delay, P1.12b). */
    public val current: Piece?
        get() = when (phase) {
            is Phase.Falling -> phase.piece
            is Phase.GameOver -> phase.piece
            is Phase.EntryDelay -> null
        }
}
