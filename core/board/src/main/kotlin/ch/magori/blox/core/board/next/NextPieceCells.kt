package ch.magori.blox.core.board.next

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.RotationSystem

/**
 * The [cells] of a piece of [type] brought to the origin: columns 0 to [columns] − 1, rows 0 to
 * [rows] − 1. What the screen draws of a piece to come, without the rotation system that gave them.
 *
 * @constructor Takes the values as they are, unchecked: only [nextPieceCells] makes them consistent (cells at
 * the origin, their size). The constructor stays open for the tests, which describe the cells they expect.
 */
@Immutable
data class NextPieceCells(val type: PieceType, val cells: Set<Position>, val columns: Int, val rows: Int)

/**
 * The cells of the next piece of [type] in the spawn orientation of [rotationSystem], brought to
 * the origin, with their size. Centring them in the preview belongs to the drawing.
 */
fun nextPieceCells(
    type: PieceType,
    rotationSystem: RotationSystem,
): NextPieceCells {
    val cells = rotationSystem.spawnOrientation(type).cells
    val left = cells.minOf(Position::column)
    val top = cells.minOf(Position::row)
    val normalised = cells.map { Position(it.column - left, it.row - top) }.toSet()
    return NextPieceCells(
        type = type,
        cells = normalised,
        columns = normalised.maxOf(Position::column) + 1,
        rows = normalised.maxOf(Position::row) + 1,
    )
}
