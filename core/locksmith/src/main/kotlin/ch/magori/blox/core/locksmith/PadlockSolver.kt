package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * Fills the free cells of a hole with free pieces, by an exact cover searched always in the same
 * order: the first free cell in reading order; the pieces by rank; for each one, its orientations in
 * the order of [ClassicRotationSystem.orientations], each with the pivot that makes that cell the
 * first of the piece in reading order (no other pivot can fit: the cell is the first free one). Of
 * several free pieces of a type, only the first rank is tried at each step: the others would explore
 * the same covers. A hole holds 24 cells and 6 pieces at most: plain backtracking is enough.
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
            for (orientation in ClassicRotationSystem.orientations(type)) {
                val piece = Piece(type, orientation, first - orientation.cells.firstInReadingOrder())
                if (!free.containsAll(piece.cells)) continue
                val rest = solve(free - piece.cells, pieces - rank) ?: continue
                return listOf(rank to piece) + rest
            }
        }
        return null
    }
}
