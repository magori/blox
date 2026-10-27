package ch.magori.blox.core.board.piece

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.game.model.Orientation
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.RotationSystem

/**
 * The [cells] of a piece of [type] brought to the origin: columns 0 to [columns] − 1, rows 0 to
 * [rows] − 1. What the screen draws of a piece to come, the next piece or that of a tile, without the
 * rotation system that gave them.
 *
 * @constructor Takes the values as they are, unchecked: only [pieceCells] makes them consistent (cells at
 * the origin, their size). The constructor stays open for the tests, which describe the cells they expect.
 */
@Immutable
data class PieceCells(val type: PieceType, val cells: Set<Position>, val columns: Int, val rows: Int)

/**
 * The cells of the next piece of [type] in the spawn orientation of [rotationSystem], brought to
 * the origin, with their size. Centring them in the preview belongs to the drawing.
 */
fun nextPieceCells(
    type: PieceType,
    rotationSystem: RotationSystem,
): PieceCells = pieceCells(type, rotationSystem.spawnOrientation(type))

/**
 * The cells of a piece of [type] in [orientation], as a tile shows it
 * ([ch.magori.blox.core.board.tile.PieceTile]), brought to the origin, with their size.
 */
fun pieceCells(
    type: PieceType,
    orientation: Orientation,
): PieceCells {
    val normalised = orientation.broughtToOrigin().cells
    return PieceCells(
        type = type,
        cells = normalised,
        columns = normalised.maxOf(Position::column) + 1,
        rows = normalised.maxOf(Position::row) + 1,
    )
}
