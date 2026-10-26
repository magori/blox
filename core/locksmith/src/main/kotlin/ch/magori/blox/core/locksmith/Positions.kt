package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.Position

/** The reading order of the grid: row by row from the top, and within a row from the left. */
internal val READING_ORDER: Comparator<Position> = compareBy(Position::row, Position::column)

/** The first of these cells in reading order. */
internal fun Set<Position>.firstInReadingOrder(): Position = minWith(READING_ORDER)

/** The offset that leads from [other] to this position. */
internal operator fun Position.minus(other: Position): Position = Position(column - other.column, row - other.row)
