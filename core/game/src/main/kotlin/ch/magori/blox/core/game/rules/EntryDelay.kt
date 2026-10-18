package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Piece

/**
 * Entry delay policy: how many ticks pass between a piece locking and the next one appearing.
 * The policy is interchangeable: [ClassicEntryDelay] holds the NES values.
 */
public interface EntryDelay {
    /**
     * Ticks between the lock of [lockedPiece] and the appearance of the next piece. Which cell of
     * the piece measures its height is up to the policy. The line clear animation is not included:
     * the engine adds it when lines are cleared.
     *
     * @throws IllegalArgumentException if [lockedPiece] is not on the board, hidden rows included,
     * as the policy measures it.
     */
    public fun ticksAfterLock(lockedPiece: Piece): Int
}
