package ch.magori.blox.core.game.model

/** The four distinct cells of a piece in one orientation, as offsets from its pivot. */
public data class Orientation(public val cells: Set<Position>) {
    init {
        require(cells.size == CELL_COUNT) { "An orientation has $CELL_COUNT cells, got ${cells.size}" }
    }

    /**
     * The same cells, shifted so that the leftmost column and the topmost row are 0: the shape alone, without its pivot,
     * as a tile or a preview draws it. Its cells are no longer offsets from the pivot of the piece: do not give it to a
     * [Piece], whose cells would move.
     */
    public fun broughtToOrigin(): Orientation {
        val offset = Position(-cells.minOf(Position::column), -cells.minOf(Position::row))
        return Orientation(cells.map { it + offset }.toSet())
    }

    private companion object {
        const val CELL_COUNT = 4
    }
}
