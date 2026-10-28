package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.firstInReadingOrder
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * Fills the free cells of a hole with free pieces, by an exact cover searched always in the same
 * order: the first free cell in reading order; the pieces by rank; for each one, its orientations in
 * the order of [ClassicRotationSystem.orientations], each with the pivot that makes that cell the
 * first of the piece in reading order (no other pivot can fit: the cell is the first free one). Of
 * several free pieces of a type, only the first rank is tried at each step: the others would explore
 * the same covers. A hole holds 32 cells and 8 pieces at most: plain backtracking is enough.
 */
internal object PadlockSolver {
    /** The first way to cover [free] exactly with the [pieces] of the given ranks, or `null`. */
    fun solve(
        free: Set<Position>,
        pieces: Map<Int, PieceType>,
    ): List<Pair<Int, Piece>>? {
        if (free.isEmpty()) return if (pieces.isEmpty()) emptyList() else null
        val first = free.firstInReadingOrder()
        for ((rank, type) in pieces.toSortedMap().entries.distinctBy { it.value }) {
            for (piece in startingAt(first, type)) {
                if (!free.containsAll(piece.cells)) continue
                val rest = solve(free - piece.cells, pieces - rank) ?: continue
                return listOf(rank to piece) + rest
            }
        }
        return null
    }

    /**
     * How many ways, [cap] at most, cover [free] exactly with [pieces], searched in the same order as [solve]. Two
     * pieces of a type being alike, swapping them gives the same way.
     */
    fun count(
        free: Set<Position>,
        pieces: List<PieceType>,
        cap: Int,
    ): Int {
        if (free.isEmpty()) return if (pieces.isEmpty()) 1 else 0
        val first = free.firstInReadingOrder()
        var found = 0
        for (type in pieces.distinct()) {
            for (piece in startingAt(first, type)) {
                if (!free.containsAll(piece.cells)) continue
                found += count(free - piece.cells, pieces - type, cap - found)
                if (found >= cap) return cap
            }
        }
        return found
    }

    /** The pieces of [type], in the order of its orientations, whose first cell in reading order is [cell]. */
    private fun startingAt(
        cell: Position,
        type: PieceType,
    ): List<Piece> = ClassicRotationSystem.orientations(type).map { orientation ->
        Piece(type, orientation, cell - orientation.cells.firstInReadingOrder())
    }
}
