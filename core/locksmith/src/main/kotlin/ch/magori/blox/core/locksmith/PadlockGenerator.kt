package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.StableRandom
import kotlin.random.Random

/**
 * Composes padlocks the way the music is composed: candidates drawn at random, ruled out or rated,
 * the best one kept.
 */
internal object PadlockGenerator {
    /** How many draws each piece of a candidate gets before the candidate is given up. */
    const val ATTEMPTS_PER_PIECE: Int = 200

    /** How many candidates a try composes, those given up included. */
    const val CANDIDATES: Int = 20

    /** How many tries a padlock takes, one after the other, before the generation gives up. */
    const val TRIES: Int = 100

    /** How many solutions of a padlock its rating counts, at most. */
    const val SOLUTIONS_COUNTED: Int = 5

    /**
     * Padlock [level], the same for everyone and for ever (contract of generation, ADR 0014 amended by P7.28): the
     * best rated, for its [PadlockProfile], of [CANDIDATES] candidates, the first one composed among those of equal
     * rating. Each candidate is composed by [compose] with its own [StableRandom], seeded by the next number of a
     * master [StableRandom] seeded by the level. When every candidate is given up or ruled out, the next try draws on a
     * seed of its own ([seedOf]), [tries] tries at most; beyond, an `IllegalStateException`.
     */
    fun ofLevel(
        level: Int,
        tries: Int = TRIES,
        compose: (PadlockProfile, Random) -> Padlock? = { profile, random ->
            candidate(profile.pieceCount, random, types = profile.types)
        },
    ): Padlock {
        require(tries >= 1) { "A padlock takes one try at least, got $tries" }
        val profile = PadlockProfile.of(level)
        repeat(tries) { tried ->
            val master = StableRandom(seedOf(level, tried))
            best(List(CANDIDATES) { compose(profile, StableRandom(master.nextLong())) }) {
                rating(it, profile.shape, profile.solutions)
            }?.let { return it }
        }
        error("No padlock $level in $tries tries")
    }

    /**
     * The seed of try [tried] of padlock [level]: the level itself, then a number of its own for each pair of the
     * level and the try, the first number of a [StableRandom] seeded by the pair (level in the high 32 bits, try in
     * the low ones). Never `level + tried`, which is the seed of another padlock.
     */
    fun seedOf(
        level: Int,
        tried: Int,
    ): Long = if (tried == 0) level.toLong() else StableRandom((level.toLong() shl 32) or tried.toLong()).nextLong()

    /** The best rated of [candidates] by [rate], the first one among equals; `null` when all are given up or ruled out. */
    private fun best(
        candidates: List<Padlock?>,
        rate: (Padlock) -> Int?,
    ): Padlock? = candidates
        .filterNotNull()
        .mapNotNull { padlock -> rate(padlock)?.let { padlock to it } }
        .maxByOrNull { it.second }
        ?.first

    /**
     * One candidate padlock of [pieceCount] pieces, composed with [random]: each piece is drawn (type among those
     * [types] leaves to it, orientation, pivot inside the outer ring) until it lies inside the ring, on free cells,
     * and, but for the first, touches an earlier piece by an edge. `null` when a piece is not placed within
     * [attempts] draws of its own.
     */
    fun candidate(
        pieceCount: Int,
        random: Random,
        attempts: Int = ATTEMPTS_PER_PIECE,
        types: PieceSet = PieceSet.ALL,
    ): Padlock? {
        val solution = mutableListOf<Piece>()
        repeat(pieceCount) {
            val taken = solution.flatMap(Piece::cells).toSet()
            val drawable = types.drawable(solution.map(Piece::type))
            val piece = (1..attempts).asSequence()
                .map { draw(random, drawable) }
                .firstOrNull { fits(it, taken) }
                ?: return null
            solution += piece
        }
        return Padlock(solution)
    }

    /**
     * How interesting [padlock] is, for a hole of [shape] and a [preference] of solutions: the weight of the shape
     * per edge of the outline of its hole (a side of a hole cell that gives onto metal), 30 per kind of piece, minus
     * 3 per cell of the box around the hole, minus the weight of the preference per solution ([SOLUTIONS_COUNTED] at
     * most). `null` when the padlock is ruled out: a rectangular hole, which fills its box, is no padlock.
     */
    fun rating(
        padlock: Padlock,
        shape: HoleShape,
        preference: SolutionPreference,
    ): Int? {
        val hole = padlock.hole
        val box = (hole.maxOf { it.column } - hole.minOf { it.column } + 1) *
            (hole.maxOf { it.row } - hole.minOf { it.row } + 1)
        if (box == hole.size) return null
        val outline = hole.sumOf { cell -> cell.neighbours().count { it !in hole } }
        val kinds = padlock.givenPieces.distinct().size
        val weightPerSolution = preference.weightPerSolution
        val solutions = if (weightPerSolution == 0) 0 else solutions(padlock)
        return shape.outlineWeight * outline + 30 * kinds - 3 * box - weightPerSolution * solutions
    }

    /** How many solutions [padlock] has, [SOLUTIONS_COUNTED] at most. */
    private fun solutions(padlock: Padlock): Int =
        PadlockSolver.count(padlock.hole, padlock.givenPieces, SOLUTIONS_COUNTED)

    /**
     * A piece of one of [types], in any of its Classic orientations, its pivot inside the outer ring. The
     * pivot being one of the cells of the piece, no placement inside the ring is left out.
     */
    private fun draw(
        random: Random,
        types: List<PieceType>,
    ): Piece {
        val type = types.random(random)
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
