package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.PieceType

/** Piece randomizer policy: which piece comes next. */
public interface Randomizer {
    /**
     * Draws the next piece.
     *
     * @param previous the last piece drawn by this randomizer, that is the piece shown in the preview,
     * not the piece that has just locked; null for the first draw of a game.
     */
    public fun next(previous: PieceType?): PieceType
}
