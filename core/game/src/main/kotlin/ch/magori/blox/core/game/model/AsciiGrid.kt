package ch.magori.blox.core.game.model

/**
 * A grid drawn as text, for tests: one line per row from the top, one character per cell from the left. What each
 * character means, and the size of the grid, are for the reader to check.
 */
public class AsciiGrid private constructor(public val lines: List<String>) {
    /** The character of each cell, by its position, in reading order: row by row from the top, then from the left. */
    public val cells: Map<Position, Char> = lines
        .flatMapIndexed { row, line -> line.mapIndexed { column, char -> Position(column, row) to char } }
        .toMap()

    public companion object {
        /** Reads [ascii] as a grid: the indentation common to its lines and the blank lines around them are ignored. */
        public fun parse(ascii: String): AsciiGrid =
            AsciiGrid(ascii.trimIndent().lines().dropWhile(String::isBlank).dropLastWhile(String::isBlank))
    }
}
