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

    /** How many candidates a seed composes, those given up included. */
    const val CANDIDATES: Int = 20

    /** How many seeds a generation tries, one after the other, before it gives up. */
    const val SEEDS: Int = 100

    /** How many pieces padlock [level] holds, from 1: two more than its level, six at most. */
    fun pieceCount(level: Int): Int {
        require(level >= 1) { "Padlocks are numbered from 1, got $level" }
        return minOf(Padlock.PIECE_COUNTS.last, 2 + level)
    }

    /**
     * The padlock of [pieceCount] pieces composed from [seed]: the best rated of [CANDIDATES]
     * candidates, the first one composed among those of equal rating. Each candidate is composed by
     * [compose] with its own random, drawn from a master random built on the seed, so that it does
     * not depend on the draws of the previous ones. When every candidate is given up or ruled out,
     * the next seed (`seed + 1`, the largest seed followed by the smallest one) is tried, [seeds]
     * seeds at most; beyond, an `IllegalStateException`: it would take thousands of candidates in a
     * row to fail.
     */
    fun generate(
        pieceCount: Int,
        seed: Long,
        seeds: Int = SEEDS,
        compose: (Random) -> Padlock? = { candidate(pieceCount, it) },
    ): Padlock {
        require(pieceCount in Padlock.PIECE_COUNTS) { "A padlock holds 3 to 6 pieces, got $pieceCount" }
        require(seeds >= 1) { "A generation tries one seed at least, got $seeds" }
        repeat(seeds) { tried ->
            val master = Random(seed + tried)
            val best = List(CANDIDATES) { compose(Random(master.nextLong())) }
                .filterNotNull()
                .mapNotNull { padlock -> rating(padlock)?.let { padlock to it } }
                .maxByOrNull { it.second }
            if (best != null) return best.first
        }
        error("No padlock of $pieceCount pieces in $seeds seeds from seed $seed")
    }

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
     * How interesting [padlock] is: 10 per edge of the outline of its hole (a side of a hole cell
     * that gives onto metal), 30 per kind of piece, minus 3 per cell of the box around the hole.
     * `null` when the padlock is ruled out: a rectangular hole, which fills its box, is no padlock.
     */
    fun rating(padlock: Padlock): Int? {
        val hole = padlock.hole
        val box = (hole.maxOf { it.column } - hole.minOf { it.column } + 1) *
            (hole.maxOf { it.row } - hole.minOf { it.row } + 1)
        if (box == hole.size) return null
        val outline = hole.sumOf { cell -> cell.neighbours().count { it !in hole } }
        val kinds = padlock.givenPieces.distinct().size
        return 10 * outline + 30 * kinds - 3 * box
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
