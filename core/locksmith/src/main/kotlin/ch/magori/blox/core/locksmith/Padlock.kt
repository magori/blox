package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A padlock: the pieces of its [solution] and the [hole] they fill, on a grid of [SIZE] × [SIZE]
 * cells whose outer ring is always metal. Every padlock has a solution: the hole is built from it.
 */
public data class Padlock(public val solution: List<Piece>) {
    /** The cells to fill: the union of the cells of the [solution]. */
    public val hole: Set<Position> = solution.flatMap(Piece::cells).toSet()

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
        return solution[index].cells.any { cell -> NEIGHBOURS.any { cell + it in earlier } }
    }

    public companion object {
        /** The grid is [SIZE] × [SIZE] cells. */
        public const val SIZE: Int = 10

        private val PIECE_COUNTS = 3..6

        private val INSIDE = 1..SIZE - 2

        /** The four cells that share an edge with a cell, as offsets. */
        private val NEIGHBOURS = listOf(Position(1, 0), Position(-1, 0), Position(0, 1), Position(0, -1))

        private const val METAL = '#'

        /** The characters that mark the pieces of a solution, by rank. */
        private val RANKS = '1'..'6'

        /** Whether [position] is inside the outer ring of metal, where the hole may lie. */
        private fun isInside(position: Position): Boolean = position.column in INSIDE && position.row in INSIDE

        /**
         * Reads a padlock from [SIZE] lines of [SIZE] characters, for tests: `#` is metal, a digit from
         * `1` to `6` is a cell of the piece of that rank in the solution, ranks following each other from
         * `1`. Each piece takes the Classic orientation and the pivot that cover its cells. Blank lines
         * around the grid are ignored.
         */
        public fun parse(ascii: String): Padlock {
            val lines = ascii.trimIndent().lines().dropWhile(String::isBlank).dropLastWhile(String::isBlank)
            require(lines.size == SIZE && lines.all { it.length == SIZE }) { "A padlock is $SIZE × $SIZE cells" }
            val cellsByRank = lines
                .flatMapIndexed { row, line -> line.mapIndexed { column, char -> char to Position(column, row) } }
                .onEach { (char, _) -> require(char == METAL || char in RANKS) { "Unknown cell '$char'" } }
                .filter { (char, _) -> char != METAL }
                .groupBy({ it.first }, { it.second })
                .toSortedMap()
            require(cellsByRank.keys.toList() == RANKS.take(cellsByRank.size)) {
                "Ranks follow each other from 1, got ${cellsByRank.keys}"
            }
            return Padlock(cellsByRank.map { (rank, cells) -> pieceCovering(rank, cells.toSet()) })
        }

        /** The Classic piece, in one of its orientations, whose cells are exactly [cells]. */
        private fun pieceCovering(
            rank: Char,
            cells: Set<Position>,
        ): Piece {
            val piece = PieceType.entries
                .flatMap { type -> ClassicRotationSystem.orientations(type).map { Piece(type, it, Position(0, 0)) } }
                .map { piece -> piece.copy(pivot = first(cells) - first(piece.cells)) }
                .firstOrNull { it.cells == cells }
            return requireNotNull(piece) { "Rank $rank is not a Classic piece" }
        }

        /** The first of [cells] in reading order. */
        private fun first(cells: Set<Position>): Position = cells.minWith(compareBy(Position::row, Position::column))

        private operator fun Position.minus(other: Position): Position =
            Position(column - other.column, row - other.row)
    }
}
