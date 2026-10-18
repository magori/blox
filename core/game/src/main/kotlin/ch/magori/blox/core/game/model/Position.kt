package ch.magori.blox.core.game.model

/** A cell coordinate. The origin is the top-left corner: [row] grows downwards. */
public data class Position(public val column: Int, public val row: Int) {
    /** This position shifted by [offset], column by column and row by row. */
    public operator fun plus(offset: Position): Position = Position(column + offset.column, row + offset.row)
}
