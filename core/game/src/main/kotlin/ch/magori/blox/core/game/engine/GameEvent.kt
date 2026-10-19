package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.LineClear
import ch.magori.blox.core.game.model.Piece

/** Something that happened in the game, for the screen and the sound. Starting a game gives none. */
public sealed interface GameEvent {
    /** [piece] could not drop any further and joined the board, where it now lies. */
    public data class PieceLocked(public val piece: Piece) : GameEvent

    /** The full rows of [lineClear] were cleared, once their animation was over. */
    public data class LinesCleared(public val lineClear: LineClear) : GameEvent

    /**
     * The stack entered the danger ([on] true) or left it ([on] false), as measured when a piece locked, on
     * the board once its full rows are cleared. Without a full row, it follows [PieceLocked] on the tick of
     * the lock; with full rows, it follows [LinesCleared] at the end of their animation, when the cleared
     * board is shown.
     */
    public data class StackDanger(public val on: Boolean) : GameEvent

    /**
     * The game is over: the next piece could not appear. It is the last event of the game, sent once, on
     * the tick the state turns to [Phase.GameOver], and the only event of that tick; it is never preceded by
     * [StackDanger] off. It carries nothing: the score, lines, level, statistics and length of the game are
     * in that state.
     */
    public data object GameOver : GameEvent
}
