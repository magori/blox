package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.PieceType

/** Rotation policy: the orientations each piece can take, and the one it spawns in. */
public interface RotationSystem {
    /**
     * All orientations of [type], in clockwise order: orientation `i + 1` is orientation `i` turned a
     * quarter turn clockwise, and the first one follows the last (the list is circular). Index 0 is
     * not necessarily the spawn orientation.
     */
    public fun orientations(type: PieceType): List<Orientation>

    /** The orientation [type] spawns in; it is one of [orientations]. */
    public fun spawnOrientation(type: PieceType): Orientation
}
