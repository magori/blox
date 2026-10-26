package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import kotlin.random.Random

/**
 * Composes padlocks the way the music is composed: candidates drawn at random, ruled out or rated,
 * the best one kept.
 */
internal object PadlockGenerator {
    /** How many draws each piece of a candidate gets before the candidate is given up. */
    const val ATTEMPTS_PER_PIECE: Int = 200

    /**
     * One candidate padlock of [pieceCount] pieces, composed with [random]: each piece is drawn (type,
     * orientation, pivot inside the outer ring) until it lies inside the ring, on free cells, and, but
     * for the first, touches an earlier piece by an edge. `null` when a piece is not placed within
     * [attempts] draws of its own.
     */
    fun candidate(
        pieceCount: Int,
        random: Random,
        attempts: Int = ATTEMPTS_PER_PIECE,
    ): Padlock? {
        val solution = mutableListOf<Piece>()
        repeat(pieceCount) {
            val taken = solution.flatMap(Piece::cells).toSet()
            val piece = (1..attempts).asSequence()
                .map { draw(random) }
                .firstOrNull { fits(it, taken) }
                ?: return null
            solution += piece
        }
        return Padlock(solution)
    }

    /**
     * A piece of any type, in any of its Classic orientations, its pivot inside the outer ring. The
     * pivot being one of the cells of the piece, no placement inside the ring is left out.
     */
    private fun draw(random: Random): Piece {
        val type = PieceType.entries.random(random)
        val orientation = ClassicRotationSystem.orientations(type).random(random)
        return Piece(type, orientation, Position(Padlock.INSIDE.random(random), Padlock.INSIDE.random(random)))
    }

    /**
     * Whether [piece] may join the cells already [taken]: inside the ring, on free cells, touching them
     * by an edge unless it is the first piece. [Padlock] checks the same rules on a whole solution; a
     * candidate checks them piece by piece, on a solution of fewer than three pieces.
     */
    private fun fits(
        piece: Piece,
        taken: Set<Position>,
    ): Boolean = piece.cells.all { Padlock.isInside(it) && it !in taken } &&
        (taken.isEmpty() || Padlock.touchesByAnEdge(piece.cells, taken))
}
