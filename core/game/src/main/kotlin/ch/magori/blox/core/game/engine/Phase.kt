package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Piece

/** Where the game stands. The next increments add the entry delay and the line clear. */
public sealed interface Phase {
    /** [piece] is in play and falls. The gravity and soft drop counters will join it (P1.12b, P1.12d). */
    public data class Falling(public val piece: Piece) : Phase

    /** The game is over: [piece] did not fit where it appears, and stays shown, as on the NES. */
    public data class GameOver(public val piece: Piece) : Phase
}
