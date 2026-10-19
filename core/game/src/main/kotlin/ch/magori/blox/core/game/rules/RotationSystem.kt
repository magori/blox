package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Rotation

/** Rotation policy: the orientations each piece can take, the one it spawns in, and how a piece turns. */
public interface RotationSystem {
    /**
     * All orientations of [type], in clockwise order: orientation `i + 1` is orientation `i` turned a
     * quarter turn clockwise, and the first one follows the last (the list is circular). Index 0 is
     * not necessarily the spawn orientation.
     */
    public fun orientations(type: PieceType): List<Orientation>

    /**
     * The orientation [type] spawns in; it is one of [orientations]. It depends on [type] alone: the same
     * orientation for the same type, however often and whenever it is asked. The engine asks it when a
     * piece spawns, the screen to draw the pieces to come.
     */
    public fun spawnOrientation(type: PieceType): Orientation

    /**
     * [orientation] turned a quarter turn: the next one in [orientations] when [rotation] is
     * clockwise, the previous one when it is counterclockwise, going round the circular list. A piece
     * with a single orientation (O) keeps it. Rejects an [orientation] that is not one of [type]'s.
     */
    public fun rotate(
        type: PieceType,
        orientation: Orientation,
        rotation: Rotation,
    ): Orientation {
        val orientations = orientations(type)
        val index = orientations.indexOf(orientation)
        require(index >= 0) { "$orientation is not an orientation of $type" }
        val step = when (rotation) {
            Rotation.CLOCKWISE -> 1
            Rotation.COUNTERCLOCKWISE -> -1
        }
        return orientations[(index + step).mod(orientations.size)]
    }

    /**
     * The pieces to try, in order, when [piece] turns by [rotation]. The engine keeps the first one
     * that fits the board (`candidates(piece, rotation).firstNotNullOfOrNull(board::place)`); if none
     * fits, the rotation is refused and the piece stays as it was.
     */
    public fun candidates(
        piece: Piece,
        rotation: Rotation,
    ): List<Piece>
}
