package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Piece

/**
 * Something that happened in the game, for the screen and the sound. The next increments add
 * `LinesCleared` and `GameOver`. Starting a game gives none.
 */
public sealed interface GameEvent {
    /** [piece] could not drop any further and joined the board, where it now lies. */
    public data class PieceLocked(public val piece: Piece) : GameEvent
}
