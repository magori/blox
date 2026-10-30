package ch.magori.blox.core.data

import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.GardenPlacement
import okio.BufferedSink
import okio.BufferedSource

/*
 * The encoding of a single garden, without the header of a file: the body of the garden file (GardenSerializer), and
 * of each garden of a file holding several. A garden is the name of its mood, the count of its pieces, then each piece
 * in turn: the name of its type, the column then the row of its pivot, then its [CELLS_PER_PIECE] cells as offsets from
 * its pivot (column, row), row by row from the top, then from the left. The enums are written by their names, which
 * must not change; a text is its length in bytes, then its bytes in UTF-8; a number is its 32 bits. The cells are
 * written whole, not as the number of an orientation: the encoding does not depend on the order in which the rotation
 * system lists them. The count of pieces tells where a garden ends: gardens may follow one another.
 */

/** The cells of a piece. */
private const val CELLS_PER_PIECE = 4

/** The most pieces a garden holds, every cell of its grid covered. */
private const val MOST_PIECES = Garden.COLUMNS * Garden.ROWS / CELLS_PER_PIECE

/** The most bytes of a name, far beyond the longest name of a mood or of a type. */
private const val MOST_NAME_BYTES = 64

/** Row by row from the top, then from the left: the order the cells of a piece are written in. */
private val READING_ORDER: Comparator<Position> = compareBy(Position::row, Position::column)

/**
 * The garden that comes next in this source, each piece placed in turn as the player would; the source stops right
 * after its last piece. An [java.io.EOFException] if the source is cut short, an [IllegalArgumentException] if it holds
 * what no garden can be: an unknown mood or type, a count or a name out of bounds, a piece off the grid, over another
 * one, or of a shape not of its type.
 */
internal fun BufferedSource.readGarden(): Garden {
    val mood = GardenMood.valueOf(readText(MOST_NAME_BYTES))
    return List(readCount(MOST_PIECES)) { readPiece() }.fold(Garden.EMPTY.withMood(mood), ::placing)
}

/** Writes [garden], without the header of a file. */
internal fun BufferedSink.writeGarden(garden: Garden) {
    writeText(garden.mood.name)
    writeInt(garden.pieces.size)
    garden.pieces.forEach { writePiece(it) }
}

/** [garden] with [piece] placed; an [IllegalArgumentException] if the garden refuses it. */
private fun placing(
    garden: Garden,
    piece: Piece,
): Garden = when (val placement = garden.place(piece)) {
    is GardenPlacement.Placed -> placement.garden
    is GardenPlacement.Refused -> throw IllegalArgumentException("A piece of the garden is ${placement.reason}")
}

private fun BufferedSource.readPiece(): Piece {
    val type = PieceType.valueOf(readText(MOST_NAME_BYTES))
    val pivot = readPosition()
    val orientation = Orientation(List(CELLS_PER_PIECE) { readPosition() }.toSet())
    return Piece(type, orientation, pivot)
}

private fun BufferedSink.writePiece(piece: Piece) {
    writeText(piece.type.name)
    writePosition(piece.pivot)
    piece.orientation.cells.sortedWith(READING_ORDER).forEach { writePosition(it) }
}

private fun BufferedSource.readPosition(): Position = Position(readInt(), readInt())

private fun BufferedSink.writePosition(position: Position) {
    writeInt(position.column)
    writeInt(position.row)
}
