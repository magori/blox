package ch.magori.blox.core.game.model

/** A cell coordinate. The origin is the top-left corner: [row] grows downwards. */
public data class Position(public val column: Int, public val row: Int) {
    /** This position shifted by [offset], column by column and row by row. */
    public operator fun plus(offset: Position): Position = Position(column + offset.column, row + offset.row)

    /** The offset that leads from [other] to this position: `other + (this - other) == this`. */
    public operator fun minus(other: Position): Position = Position(column - other.column, row - other.row)

    /** The four cells that share an edge with this one. */
    public fun neighbours(): List<Position> = EDGE_OFFSETS.map(::plus)

    private companion object {
        val EDGE_OFFSETS = listOf(Position(1, 0), Position(-1, 0), Position(0, 1), Position(0, -1))
    }
}

/** The reading order of a grid: row by row from the top, and within a row from the left. */
private val READING_ORDER: Comparator<Position> = compareBy(Position::row, Position::column)

/** The first of these cells in reading order: on the topmost row, the leftmost one. Fails when there is none. */
public fun Iterable<Position>.firstInReadingOrder(): Position = minWith(READING_ORDER)
