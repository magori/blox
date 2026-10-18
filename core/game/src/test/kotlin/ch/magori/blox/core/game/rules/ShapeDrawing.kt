package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.Position

private const val PIVOT = '@'
private const val EMPTY = '.'

/**
 * Reads a piece drawn in ASCII and returns its cells as offsets from the pivot. `@` is the pivot
 * (always a cell of the piece), any letter is another cell, `.` is empty. Rows grow downwards.
 * The drawing has exactly one pivot and no blank row inside; indentation is ignored.
 */
internal fun cellsOf(drawing: String): Set<Position> {
    val lines = drawing.trimIndent().lines()
    require(lines.none(String::isBlank)) { "A drawing has no blank row inside: \"$drawing\"" }
    lines.flatMap(String::toList).forEach { symbol ->
        require(symbol == PIVOT || symbol == EMPTY || symbol.isLetter()) { "Unknown symbol '$symbol'" }
    }
    require(lines.sumOf { line -> line.count { it == PIVOT } } == 1) { "A drawing has exactly one pivot '$PIVOT'" }
    val pivotRow = lines.indexOfFirst { PIVOT in it }
    val pivotColumn = lines[pivotRow].indexOf(PIVOT)
    return lines.flatMapIndexed { row, line ->
        line.mapIndexedNotNull { column, symbol ->
            if (symbol == EMPTY) null else Position(column - pivotColumn, row - pivotRow)
        }
    }.toSet()
}
