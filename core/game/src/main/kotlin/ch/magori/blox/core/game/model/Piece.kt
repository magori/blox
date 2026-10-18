package ch.magori.blox.core.game.model

/**
 * A piece on the board: its [type], its [orientation] and the position of its [pivot].
 * Immutable: moving or rotating a piece gives a new one.
 *
 * Nothing checks that [orientation] belongs to [type]: the rotation system that provides the
 * orientation is responsible for that.
 */
public data class Piece(public val type: PieceType, public val orientation: Orientation, public val pivot: Position) {
    /** The cells the piece covers: its pivot plus each offset of its orientation. */
    public val cells: Set<Position> =
        orientation.cells.map(pivot::plus).toSet()

    /** This piece one cell further in [direction], wherever that lands: [Board.move] checks it. */
    public fun moved(direction: Direction): Piece = copy(pivot = pivot + direction.offset)
}
