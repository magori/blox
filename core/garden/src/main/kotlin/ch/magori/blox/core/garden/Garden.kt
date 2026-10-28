package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.AsciiGrid
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.firstInReadingOrder
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.pieceCovering

/**
 * A musical garden: the [pieces] placed on its grid of [COLUMNS] × [ROWS] cells, a column per eighth
 * note and a row per degree of the scale of its [mood]. Immutable: placing, taking back, clearing or
 * changing the mood gives a new garden. Built from [EMPTY] by [place] and [withMood] only, so every
 * piece lies inside the grid, takes a Classic orientation and overlaps no other one. There is no limit
 * to the number of pieces.
 */
@ConsistentCopyVisibility
public data class Garden private constructor(public val pieces: Set<Piece>, public val mood: GardenMood) {
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
        return refusal?.let(GardenPlacement::Refused) ?: GardenPlacement.Placed(copy(pieces = pieces + piece))
    }

    /** The placed piece that covers [cell], or `null` when the cell is free. */
    public fun pieceAt(cell: Position): Piece? = pieces.firstOrNull { cell in it.cells }

    /** Takes the placed [piece] back off the grid, its cells free again; any other piece leaves the garden as is. */
    public fun takeBack(piece: Piece): Garden = copy(pieces = pieces - piece)

    /** The same pieces where they are, heard in [mood]. */
    public fun withMood(mood: GardenMood): Garden = copy(mood = mood)

    /** The same grid with nothing placed, in the same mood. */
    public fun clear(): Garden = copy(pieces = emptySet())

    public companion object {
        /** The grid has [COLUMNS] columns, one per eighth note: 2 bars of 4/4. */
        public const val COLUMNS: Int = 16

        /** The grid has [ROWS] rows, one per degree of the scale, the highest on row 0. */
        public const val ROWS: Int = 14

        /** A garden with nothing placed, in the default mood: calm. */
        public val EMPTY: Garden = Garden(emptySet(), GardenMood.CALM)

        private const val FREE = '.'

        /** The digits that mark a piece whose type is read from its shape. */
        private val DIGITS = '1'..'9'

        /**
         * Reads a garden from [ROWS] lines of [COLUMNS] characters, for tests: `.` is a free cell, the
         * letter of a type (`I`, `O`, `T`, `S`, `Z`, `J`, `L`) a cell of a piece of that type, and a digit
         * from `1` to `9` a cell of a piece whose type is read from its shape. Each region of the same mark,
         * linked edge to edge, is one piece, recognised by its Classic orientation and its pivot: two
         * pieces of the same type that touch are told apart by writing one of them with a digit. Blank
         * lines around the grid are ignored. The garden read is calm, like [EMPTY].
         */
        public fun parse(ascii: String): Garden {
            val grid = AsciiGrid.parse(ascii)
            require(grid.lines.size == ROWS) { "A garden has $ROWS rows, got ${grid.lines.size}" }
            grid.lines.forEachIndexed { row, line ->
                require(line.length == COLUMNS) { "A garden has $COLUMNS columns, row $row has ${line.length} cells" }
            }
            return grid.cells.entries
                .filter { (_, char) -> char != FREE }
                .onEach { (cell, mark) ->
                    require(mark in DIGITS || typeOf(mark) != null) { "Unknown mark '$mark' at ${cell.text()}" }
                }.groupBy({ it.value }, { it.key })
                .flatMap { (mark, cells) -> regions(cells.toSet()).map { pieceCovering(mark, it) } }
                // Built directly, without `place`: what it checks holds by construction. Each piece takes a
                // Classic orientation (pieceCovering), its cells come from a grid of checked size, and no two
                // regions share a cell.
                .let { pieces -> EMPTY.copy(pieces = pieces.toSet()) }
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
                    cell.neighbours().filter { it in left && region.add(it) }.forEach(frontier::addLast)
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
            val piece = ClassicRotationSystem.pieceCovering(cells, types = type?.let(::listOf) ?: PieceType.entries)
            return requireNotNull(piece) {
                val expected = type?.let { "$it; two touching pieces of the same type take a digit" } ?: "Classic piece"
                "The cells marked '$mark' from ${cells.firstInReadingOrder().text()} make no $expected"
            }
        }

        /** This position as `(column, row)`, for messages. */
        private fun Position.text(): String = "($column, $row)"
    }
}
