package ch.magori.blox.core.board.mascot

import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A mascot of BloX (docs/design-system/pictogrammes.md, Mascotte): one per game, made of the [piece] that names it,
 * with its accessory. It stands as its piece comes in, or that turned a quarter [turn].
 */
enum class MascotCharacter(internal val piece: PieceType, private val turn: Rotation? = null) {
    /** The Classic game: a T, crowned. */
    CLASSIC(PieceType.T),

    /** The Locksmith: an O, holding a key; it stands for the « o » of the logo. */
    LOCKSMITH(PieceType.O),

    /** The Jardin musical: an L standing on its foot, the foot to the right, a leaf above it. */
    GARDEN(PieceType.L, turn = Rotation.COUNTERCLOCKWISE),

    /** The Graine du jour: a Z, a sprout growing from it. */
    DAILY(PieceType.Z),
    ;

    /** The cells of its body, from (0, 0) at the top left. */
    internal val cells: Set<Position>
        get() {
            val spawn = ClassicRotationSystem.spawnOrientation(piece)
            val offsets = turn?.let { ClassicRotationSystem.rotate(piece, spawn, it) }?.cells ?: spawn.cells
            val left = offsets.minOf { it.column }
            val top = offsets.minOf { it.row }
            return offsets.map { Position(column = it.column - left, row = it.row - top) }.toSet()
        }
}
