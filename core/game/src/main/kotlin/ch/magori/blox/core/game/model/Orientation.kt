package ch.magori.blox.core.game.model

/** The four distinct cells of a piece in one orientation, as offsets from its pivot. */
public data class Orientation(public val cells: Set<Position>) {
    init {
        require(cells.size == CELL_COUNT) { "An orientation has $CELL_COUNT cells, got ${cells.size}" }
    }

    private companion object {
        const val CELL_COUNT = 4
    }
}
