package ch.magori.blox.core.game.model

/**
 * The immutable playfield: 10 columns × 20 rows, all visible. Row 0 is the top row.
 * Every change returns a new board.
 *
 * Rows -2 and -1 are hidden above the board and always empty. Only [fits] accepts them, so the
 * piece in play may reach them; [get] and [fill] reject them like any position outside the board.
 * [fill] stays strict: it is [lock] that drops the hidden cells of a piece, as the NES ROM does.
 */
public class Board private constructor(private val cells: List<Cell>) {
    public val columns: Int get() = COLUMNS
    public val rows: Int get() = ROWS

    /** The cell at [position]; rejects a position outside the board, hidden rows included. */
    public operator fun get(position: Position): Cell = cells[cellIndex(position)]

    /** Whether [position] is on the visible board: columns 0 to 9, rows 0 to 19. */
    public operator fun contains(position: Position): Boolean =
        position.column in 0 until columns && position.row in 0 until rows

    /**
     * Whether [piece] fits on the board without collision: every one of its cells is in columns 0
     * to 9 and rows -2 to 19, and on an empty cell. The hidden rows -2 and -1 are always empty, so
     * the piece in play may reach them (a vertical I rotated right after spawning).
     */
    public fun fits(piece: Piece): Boolean = piece.cells.all(::isFree)

    /** Returns [candidate] if it [fits], `null` otherwise. The board never changes. */
    public fun place(candidate: Piece): Piece? = candidate.takeIf(::fits)

    /**
     * Moves [piece] one cell in [direction]. Returns the moved piece if it [fits], `null` otherwise
     * (wall, floor or occupied cell). Moving within the hidden rows is allowed. [piece] itself never
     * changes. The engine uses a refused move down as the signal to [lock] (P1.12).
     */
    public fun move(
        piece: Piece,
        direction: Direction,
    ): Piece? = place(piece.moved(direction))

    private fun isFree(position: Position): Boolean = position.column in 0 until columns &&
        position.row in ROWS_WITH_HIDDEN &&
        (position.row < 0 || this[position] == Cell.Empty)

    /** Returns a new board where [position] is filled by a cell of [type]; rejects a position outside the board. */
    public fun fill(
        position: Position,
        type: PieceType,
    ): Board = withFilled(listOf(position), type)

    /**
     * Returns a new board where [piece] has joined the stack: its cells are filled with its type, in
     * a single copy. Its cells in the hidden rows -2 and -1 are lost and the game goes on, as on the
     * NES, which only stores rows 0 and below. Rejects a piece that does not [fits] (outside the
     * walls, below the floor, above row -2, or on an occupied cell). This board never changes;
     * deciding when to lock belongs to the engine.
     */
    public fun lock(piece: Piece): Board {
        require(fits(piece)) { "$piece does not fit on the board" }
        val visiblePositions = piece.cells.filter { position -> position.row >= 0 }
        return withFilled(visiblePositions, piece.type)
    }

    /** One copy of this board, then [positions] filled with [type]; rejects a position outside the board. */
    private fun withFilled(
        positions: Iterable<Position>,
        type: PieceType,
    ): Board {
        val filled = Cell.Filled(type)
        return Board(cells.toMutableList().apply { positions.forEach { position -> set(cellIndex(position), filled) } })
    }

    override fun equals(other: Any?): Boolean = other is Board && cells == other.cells

    override fun hashCode(): Int = cells.hashCode()

    /** The board in the [parse] format: one line per row, `.` for an empty cell, a piece letter otherwise. */
    override fun toString(): String =
        cells.chunked(COLUMNS).joinToString("\n") { row -> row.joinToString("", transform = ::symbolOf) }

    private fun cellIndex(position: Position): Int {
        require(position in this) { "$position is outside the board" }
        return position.row * columns + position.column
    }

    public companion object {
        private const val COLUMNS = 10
        private const val ROWS = 20
        private const val FIRST_HIDDEN_ROW = -2
        private const val EMPTY_SYMBOL = '.'

        /** Rows a piece may occupy: the hidden rows -2 and -1 above the board, then the visible rows 0 to 19. */
        public val ROWS_WITH_HIDDEN: IntRange = FIRST_HIDDEN_ROW until ROWS

        public fun empty(): Board = Board(emptyRows(ROWS))

        /**
         * Reads a board from text, one line per row, `.` for an empty cell and a piece letter for
         * a filled one. The text describes the bottom rows: missing rows above are empty.
         * Indentation and leading or trailing blank lines are ignored.
         */
        public fun parse(text: String): Board {
            val lines = text.lines()
                .map(String::trim)
                .dropWhile(String::isEmpty)
                .dropLastWhile(String::isEmpty)
            require(lines.size <= ROWS) { "A board has at most $ROWS rows, got ${lines.size}" }
            return Board(emptyRows(ROWS - lines.size) + lines.flatMap(::parseRow))
        }

        private fun symbolOf(cell: Cell): String = when (cell) {
            Cell.Empty -> EMPTY_SYMBOL.toString()
            is Cell.Filled -> cell.type.name
        }

        private fun emptyRows(count: Int): List<Cell> = List(count * COLUMNS) { Cell.Empty }

        private fun parseRow(line: String): List<Cell> {
            require(line.length == COLUMNS) { "A row has $COLUMNS characters, got \"$line\"" }
            return line.map(::parseCell)
        }

        private fun parseCell(symbol: Char): Cell {
            if (symbol == EMPTY_SYMBOL) return Cell.Empty
            val type = PieceType.entries.firstOrNull { it.name == symbol.toString() }
            requireNotNull(type) { "Unknown cell '$symbol', expected '$EMPTY_SYMBOL' or a piece letter" }
            return Cell.Filled(type)
        }
    }
}
