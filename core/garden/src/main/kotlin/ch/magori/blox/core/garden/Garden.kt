package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/**
 * A musical garden: the [pieces] placed on its grid of [COLUMNS] × [ROWS] cells, a column per eighth
 * note and a row per degree of the scale. Immutable: placing, taking back or clearing gives a new
 * garden. Built from [EMPTY] by [place] only, so every piece lies inside the grid, takes a Classic
 * orientation and overlaps no other one. There is no limit to the number of pieces.
 */
@ConsistentCopyVisibility
public data class Garden private constructor(public val pieces: Set<Piece>) {
    /**
     * Places [piece]: accepted when every cell lies inside the grid, on a free cell; refused otherwise,
     * a cell off the grid before one over a placed piece. A piece in an orientation that is not a Classic
     * one of its type is a programming error.
     */
    public fun place(piece: Piece): GardenPlacement {
        require(piece.orientation in ClassicRotationSystem.orientations(piece.type)) {
            "A piece takes a Classic orientation of its type"
        }
        val refusal = when {
            !piece.cells.all(::isInside) -> GardenRefusal.OUT_OF_GRID
            piece.cells.any { pieceAt(it) != null } -> GardenRefusal.OVERLAP
            else -> null
        }
        return if (refusal == null) GardenPlacement.Placed(Garden(pieces + piece)) else GardenPlacement.Refused(refusal)
    }

    /** The placed piece that covers [cell], or `null` when the cell is free. */
    public fun pieceAt(cell: Position): Piece? = pieces.firstOrNull { cell in it.cells }

    /** Takes the placed [piece] back off the grid, its cells free again; any other piece leaves the garden as is. */
    public fun takeBack(piece: Piece): Garden = Garden(pieces - piece)

    /** The same grid with nothing placed. */
    public fun clear(): Garden = EMPTY

    public companion object {
        /** The grid has [COLUMNS] columns, one per eighth note: 2 bars of 4/4. */
        public const val COLUMNS: Int = 16

        /** The grid has [ROWS] rows, one per degree of the scale, the highest on row 0. */
        public const val ROWS: Int = 14

        /** A garden with nothing placed. */
        public val EMPTY: Garden = Garden(emptySet())

        private const val FREE = '.'

        /** The digits that mark a piece whose type is read from its shape. */
        private val DIGITS = '1'..'9'

        /** The four cells that share an edge with a cell, as offsets. */
        private val NEIGHBOURS: List<Position> =
            listOf(Position(1, 0), Position(-1, 0), Position(0, 1), Position(0, -1))

        /** Row by row from the top, and within a row from the left. */
        private val READING_ORDER: Comparator<Position> = compareBy(Position::row, Position::column)

        /**
         * Reads a garden from [ROWS] lines of [COLUMNS] characters, for tests: `.` is a free cell, the
         * letter of a type (`I`, `O`, `T`, `S`, `Z`, `J`, `L`) a cell of a piece of that type, and a digit
         * from `1` to `9` a cell of a piece whose type is read from its shape. Each region of the same mark,
         * linked edge to edge, is one piece, recognised by its Classic orientation and its pivot: two
         * pieces of the same type that touch are told apart by writing one of them with a digit. Blank
         * lines around the grid are ignored.
         */
        public fun parse(ascii: String): Garden {
            val lines = ascii.trimIndent().lines().dropWhile(String::isBlank).dropLastWhile(String::isBlank)
            require(lines.size == ROWS) { "A garden has $ROWS rows, got ${lines.size}" }
            lines.forEachIndexed { row, line ->
                require(line.length == COLUMNS) { "A garden has $COLUMNS columns, row $row has ${line.length} cells" }
            }
            return lines
                .flatMapIndexed { row, line -> line.mapIndexed { column, char -> char to Position(column, row) } }
                .filter { (char, _) -> char != FREE }
                .onEach { (mark, cell) ->
                    require(mark in DIGITS || typeOf(mark) != null) { "Unknown mark '$mark' at ${cell.text()}" }
                }.groupBy({ it.first }, { it.second })
                .flatMap { (mark, cells) -> regions(cells.toSet()).map { pieceCovering(mark, it) } }
                // Built directly, without `place`: what it checks holds by construction. Each piece takes a
                // Classic orientation (pieceCovering), its cells come from a grid of checked size, and no two
                // regions share a cell.
                .let { pieces -> Garden(pieces.toSet()) }
        }

        private fun isInside(cell: Position): Boolean = cell.column in 0 until COLUMNS && cell.row in 0 until ROWS

        /** The type whose letter is [mark], or `null` when none is. */
        private fun typeOf(mark: Char): PieceType? = PieceType.entries.firstOrNull { it.name == "$mark" }

        /** [cells] split into regions of cells linked edge to edge. */
        private fun regions(cells: Set<Position>): List<Set<Position>> {
            val left = cells.toMutableSet()
            val regions = mutableListOf<Set<Position>>()
            while (left.isNotEmpty()) {
                val region = mutableSetOf(left.first())
                val frontier = ArrayDeque(region)
                while (frontier.isNotEmpty()) {
                    val cell = frontier.removeFirst()
                    NEIGHBOURS.map(cell::plus).filter { it in left && region.add(it) }.forEach(frontier::addLast)
                }
                left -= region
                regions += region
            }
            return regions
        }

        /**
         * The piece, in one of its Classic orientations, whose cells are exactly [cells], all marked [mark]:
         * of the type of its letter, or of any type for a digit.
         */
        private fun pieceCovering(
            mark: Char,
            cells: Set<Position>,
        ): Piece {
            val type = typeOf(mark)
            val first = cells.minWith(READING_ORDER)
            val piece = (type?.let(::listOf) ?: PieceType.entries)
                .flatMap { candidate -> ClassicRotationSystem.orientations(candidate).map { candidate to it } }
                .map { (candidate, orientation) ->
                    val offset = orientation.cells.minWith(READING_ORDER)
                    Piece(candidate, orientation, Position(first.column - offset.column, first.row - offset.row))
                }.firstOrNull { it.cells == cells }
            return requireNotNull(piece) {
                val expected = type?.let { "$it; two touching pieces of the same type take a digit" } ?: "Classic piece"
                "The cells marked '$mark' from ${first.text()} make no $expected"
            }
        }

        /** This position as `(column, row)`, for messages. */
        private fun Position.text(): String = "($column, $row)"
    }
}
