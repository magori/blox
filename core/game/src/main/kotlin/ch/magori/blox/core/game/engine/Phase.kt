package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.RowsCleared

/** Where the game stands. */
public sealed interface Phase {
    /**
     * [piece] is in play and falls. [ticksSinceDrop] counts the ticks since it appeared or last
     * dropped: it drops when that count reaches the ticks per row of the level. The soft drop counter
     * will join it (P1.12d).
     */
    public data class Falling(public val piece: Piece, public val ticksSinceDrop: Int = 0) : Phase {
        init {
            require(ticksSinceDrop >= 0) { "ticksSinceDrop cannot be negative: $ticksSinceDrop" }
        }
    }

    /** No piece is in play: the next one appears in [ticksLeft] ticks, at least one. */
    public data class EntryDelay(public val ticksLeft: Int) : Phase {
        init {
            require(ticksLeft >= 1) { "ticksLeft must be at least 1: $ticksLeft" }
        }
    }

    /**
     * Full rows are being cleared: [rowsCleared] holds the rows and the board once cleared, which the
     * state publishes when the animation is over, [ticksLeft] ticks from now. The entry delay that
     * follows, [entryDelayAfter] ticks, and the stack danger once cleared, [stackInDangerAfter], were
     * computed when the piece locked; the state takes them when the animation is over.
     */
    public data class Clearing(
        public val rowsCleared: RowsCleared,
        public val ticksLeft: Int,
        public val entryDelayAfter: Int,
        public val stackInDangerAfter: Boolean,
    ) : Phase {
        init {
            require(ticksLeft >= 1) { "ticksLeft must be at least 1: $ticksLeft" }
            require(entryDelayAfter >= 1) { "entryDelayAfter must be at least 1: $entryDelayAfter" }
        }
    }

    /** The game is over: [piece] did not fit where it appears, and stays shown, as on the NES. */
    public data class GameOver(public val piece: Piece) : Phase
}
