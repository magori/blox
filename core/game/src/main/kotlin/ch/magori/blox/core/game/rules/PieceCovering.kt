package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.firstInReadingOrder

/**
 * The piece of one of [types], in one of the [orientations][RotationSystem.orientations] of its type, whose cells are
 * exactly [cells]; `null` when none covers them, as for no cells at all. Types are tried in the order given, then
 * orientations in the order of [RotationSystem.orientations]: that order only matters for a rotation system in which
 * two orientations, of one type or of two, share a shape.
 */
public fun RotationSystem.pieceCovering(
    cells: Set<Position>,
    types: List<PieceType> = PieceType.entries,
): Piece? {
    if (cells.isEmpty()) return null
    val first = cells.firstInReadingOrder()
    return types
        .flatMap { type -> orientations(type).map { orientation -> type to orientation } }
        .map { (type, orientation) -> Piece(type, orientation, first - orientation.cells.firstInReadingOrder()) }
        .firstOrNull { it.cells == cells }
}
