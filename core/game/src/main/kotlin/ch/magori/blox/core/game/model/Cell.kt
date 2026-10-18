package ch.magori.blox.core.game.model

/** The content of a board cell: empty, or filled by a piece that keeps its type. */
public sealed interface Cell {
    public data object Empty : Cell

    public data class Filled(public val type: PieceType) : Cell
}
