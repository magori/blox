package ch.magori.blox.core.board.mascot

import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A mascot of BloX (docs/design-system/pictogrammes.md, Mascotte): one per game, made of the [piece] that names it,
 * with its accessory.
 */
enum class MascotCharacter(internal val piece: PieceType) {
    /** The Classic game: a T, crowned. */
    CLASSIC(PieceType.T),

    /** The Locksmith: an O, holding a key; it stands for the « o » of the logo. */
    LOCKSMITH(PieceType.O),

    /** The Graine du jour: a Z, a sprout growing from it. */
    DAILY(PieceType.Z),
    ;

    /** The cells of its body, as its piece comes in, from (0, 0) at the top left. */
    internal val cells: Set<Position>
        get() {
            val offsets = ClassicRotationSystem.spawnOrientation(piece).cells
            val left = offsets.minOf { it.column }
            val top = offsets.minOf { it.row }
            return offsets.map { Position(column = it.column - left, row = it.row - top) }.toSet()
        }
}
