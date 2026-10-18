package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation

/**
 * Orientations of the Classic mode, read from the NES ROM orientation table at $8A9C
 * (meatfighter.com, "Applying AI to Nintendo Tetris"). Offsets are (column, row) around the
 * pivot, rows growing downwards; each piece lists its orientations in clockwise order.
 *
 * A rotation has a single candidate: the same pivot, the next or previous orientation. There is no
 * kick: if that candidate does not fit, the rotation is refused.
 */
public object ClassicRotationSystem : RotationSystem {
    override fun candidates(
        piece: Piece,
        rotation: Rotation,
    ): List<Piece> = listOf(piece.copy(orientation = rotate(piece.type, piece.orientation, rotation)))

    private val t = PieceOrientations(
        spawnIndex = 2,
        orientation(-1 to 0, 0 to 0, 1 to 0, 0 to -1), // up
        orientation(0 to -1, 0 to 0, 1 to 0, 0 to 1), // right
        orientation(-1 to 0, 0 to 0, 1 to 0, 0 to 1), // down
        orientation(0 to -1, -1 to 0, 0 to 0, 0 to 1), // left
    )
    private val j = PieceOrientations(
        spawnIndex = 3,
        orientation(0 to -1, 0 to 0, -1 to 1, 0 to 1), // left
        orientation(-1 to -1, -1 to 0, 0 to 0, 1 to 0), // up
        orientation(0 to -1, 1 to -1, 0 to 0, 0 to 1), // right
        orientation(-1 to 0, 0 to 0, 1 to 0, 1 to 1), // down
    )
    private val z = PieceOrientations(
        spawnIndex = 0,
        orientation(-1 to 0, 0 to 0, 0 to 1, 1 to 1), // horizontal
        orientation(1 to -1, 0 to 0, 1 to 0, 0 to 1), // vertical
    )
    private val o = PieceOrientations(
        spawnIndex = 0,
        orientation(-1 to 0, 0 to 0, -1 to 1, 0 to 1),
    )
    private val s = PieceOrientations(
        spawnIndex = 0,
        orientation(0 to 0, 1 to 0, -1 to 1, 0 to 1), // horizontal
        orientation(0 to -1, 0 to 0, 1 to 0, 1 to 1), // vertical
    )
    private val l = PieceOrientations(
        spawnIndex = 1,
        orientation(0 to -1, 0 to 0, 0 to 1, 1 to 1), // right
        orientation(-1 to 0, 0 to 0, 1 to 0, -1 to 1), // down
        orientation(-1 to -1, 0 to -1, 0 to 0, 0 to 1), // left
        orientation(1 to -1, -1 to 0, 0 to 0, 1 to 0), // up
    )
    private val i = PieceOrientations(
        spawnIndex = 1,
        orientation(0 to -2, 0 to -1, 0 to 0, 0 to 1), // vertical
        orientation(-2 to 0, -1 to 0, 0 to 0, 1 to 0), // horizontal
    )

    override fun orientations(type: PieceType): List<Orientation> = of(type).orientations

    override fun spawnOrientation(type: PieceType): Orientation = of(type).spawn

    private fun of(type: PieceType): PieceOrientations = when (type) {
        PieceType.I -> i
        PieceType.O -> o
        PieceType.T -> t
        PieceType.S -> s
        PieceType.Z -> z
        PieceType.J -> j
        PieceType.L -> l
    }

    /** Builds an orientation from (column, row) offsets, written as in the NES table. */
    private fun orientation(vararg offsets: Pair<Int, Int>): Orientation =
        Orientation(offsets.map { (column, row) -> Position(column, row) }.toSet())

    private class PieceOrientations(spawnIndex: Int, vararg orientations: Orientation) {
        val orientations: List<Orientation> = orientations.toList()
        val spawn: Orientation = this.orientations[spawnIndex]
    }
}
