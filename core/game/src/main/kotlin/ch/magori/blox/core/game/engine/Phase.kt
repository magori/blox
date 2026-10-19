package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.RowsCleared
import ch.magori.blox.core.game.model.Ticks

/** Where the game stands. */
public sealed interface Phase {
    /**
     * [piece] is in play and falls. [ticksSinceDrop] counts the ticks since it appeared or last
     * dropped: it drops when that count reaches the ticks per row of the level. [softDrop] is the repeat
     * of the soft drop, null when none is going on: only a new press of down starts it. [softDropRows] is
     * the rows it dropped the piece in a row, which score when the piece locks. A piece appears with
     * neither.
     */
    @ConsistentCopyVisibility
    public data class Falling internal constructor(
        public val piece: Piece,
        public val ticksSinceDrop: Int = 0,
        internal val softDrop: AutoRepeat? = null,
        public val softDropRows: Int = 0,
    ) : Phase {
        init {
            require(ticksSinceDrop >= 0) { "ticksSinceDrop cannot be negative: $ticksSinceDrop" }
            require(softDropRows >= 0) { "softDropRows cannot be negative: $softDropRows" }
        }
    }

    /** No piece is in play: the next one appears in [ticksLeft] ticks, at least one. */
    @ConsistentCopyVisibility
    public data class EntryDelay internal constructor(public val ticksLeft: Ticks) : Phase

    /**
     * Full rows are being cleared: [rowsCleared] holds the rows and the board once cleared, which the
     * state publishes when the animation is over, [ticksLeft] ticks from now, at least one. The entry delay
     * that follows, [entryDelayAfter] ticks, and the stack danger once cleared, [stackInDangerAfter], were
     * computed when the piece locked; the state takes them when the animation is over.
     */
    @ConsistentCopyVisibility
    public data class Clearing internal constructor(
        public val rowsCleared: RowsCleared,
        public val ticksLeft: Ticks,
        public val entryDelayAfter: Ticks,
        public val stackInDangerAfter: Boolean,
    ) : Phase

    /** The game is over: [piece] did not fit where it appears, and stays shown, as on the NES. */
    @ConsistentCopyVisibility
    public data class GameOver internal constructor(public val piece: Piece) : Phase
}
