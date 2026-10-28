package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.AsciiGrid
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.pieceCovering

/**
 * A padlock: the pieces of its [solution] and the [hole] they fill, on a grid of [SIZE] × [SIZE]
 * cells whose outer ring is always metal. Every padlock has a solution: the hole is built from it.
 */
public data class Padlock(public val solution: List<Piece>) {
    /** The cells to fill: the union of the cells of the [solution]. */
    public val hole: Set<Position> = solution.flatMap(Piece::cells).toSet()

    /**
     * The pieces given to the player: those of the [solution], in the order of [PieceType], which
     * tells nothing of the solution.
     */
    public val givenPieces: List<PieceType> = solution.map(Piece::type).sorted()

    init {
        require(solution.size in PIECE_COUNTS) {
            "A padlock holds ${PIECE_COUNTS.first} to ${PIECE_COUNTS.last} pieces, got ${solution.size}"
        }
        require(solution.all { it.orientation in ClassicRotationSystem.orientations(it.type) }) {
            "Every piece of a solution takes a Classic orientation of its type"
        }
        require(hole.size == solution.sumOf { it.cells.size }) { "The pieces of a solution cannot overlap" }
        require(hole.all(::isInside)) { "The hole stays inside the outer ring of metal" }
        require(solution.indices.drop(1).all(::touchesAnEarlierPiece)) {
            "Every piece but the first touches an earlier one edge to edge"
        }
    }

    private fun touchesAnEarlierPiece(index: Int): Boolean {
        val earlier = solution.take(index).flatMap(Piece::cells).toSet()
        return touchesByAnEdge(solution[index].cells, earlier)
    }

    public companion object {
        /** The grid is [SIZE] × [SIZE] cells. */
        public const val SIZE: Int = 10

        /** How many pieces a padlock holds. */
        internal val PIECE_COUNTS: IntRange = 3..8

        /** The rows and columns inside the outer ring of metal. */
        internal val INSIDE: IntRange = 1..SIZE - 2

        private const val METAL = '#'

        /** The characters that mark the pieces of a solution, by rank. */
        private val RANKS = '1'..'8'

        /**
         * Padlock [level] (from 1) of [seed], as the screen draws it until P7.30: the best of the padlocks of
         * `PadlockGenerator.pieceCount` pieces (6 at most, as before the tiers), composed from that seed on
         * `kotlin.random.Random`, whose sequence is no contract, without the types, the shapes nor the preference of
         * solutions of the tiers. [ofLevel] replaces it.
         */
        public fun generate(
            level: Int,
            seed: Long,
        ): Padlock = PadlockGenerator.generate(PadlockGenerator.pieceCount(level), seed)

        /**
         * Padlock [level] (from 1), the same for everyone and for ever: its seed is the level, and its generation is a
         * contract, changed only by an ADR (`PadlockGenerator.ofLevel`, ADR 0014 amended by P7.28).
         */
        public fun ofLevel(level: Int): Padlock = PadlockGenerator.ofLevel(level)

        /** Whether [position] is inside the outer ring of metal, where the hole may lie. */
        internal fun isInside(position: Position): Boolean = position.column in INSIDE && position.row in INSIDE

        /** Whether one of [cells] shares an edge with one of [others]. */
        internal fun touchesByAnEdge(
            cells: Set<Position>,
            others: Set<Position>,
        ): Boolean = cells.any { cell -> cell.neighbours().any { it in others } }

        /**
         * Reads a padlock from [SIZE] lines of [SIZE] characters, for tests: `#` is metal, a digit from
         * `1` to `8` is a cell of the piece of that rank in the solution, ranks following each other from
         * `1`. Each piece takes the Classic orientation and the pivot that cover its cells. Blank lines
         * around the grid are ignored.
         */
        public fun parse(ascii: String): Padlock {
            val grid = AsciiGrid.parse(ascii)
            require(grid.lines.size == SIZE && grid.lines.all { it.length == SIZE }) {
                "A padlock is $SIZE × $SIZE cells"
            }
            val cellsByRank = grid.cells.entries
                .onEach { (_, char) -> require(char == METAL || char in RANKS) { "Unknown cell '$char'" } }
                .filter { (_, char) -> char != METAL }
                .groupBy({ it.value }, { it.key })
                .toSortedMap()
            require(cellsByRank.keys.toList() == RANKS.take(cellsByRank.size)) {
                "Ranks follow each other from 1, got ${cellsByRank.keys}"
            }
            return Padlock(
                cellsByRank.map { (rank, cells) ->
                    requireNotNull(ClassicRotationSystem.pieceCovering(cells.toSet())) {
                        "Rank $rank is not a Classic piece"
                    }
                },
            )
        }
    }
}
