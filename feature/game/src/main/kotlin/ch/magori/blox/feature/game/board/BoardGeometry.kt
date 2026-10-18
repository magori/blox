package ch.magori.blox.feature.game.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import ch.magori.blox.core.game.model.Position
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Where the board is drawn: square cells of [cellSize], the top-left corner of the board at
 * [origin], in whole pixels of the space given to [fit], so that cells meet without blurred seams.
 */
@Immutable
internal data class BoardGeometry(val cellSize: Float, val origin: Offset) {
    /** The top-left corner of the cell at [position]. */
    fun cellTopLeft(position: Position): Offset = origin + Offset(position.column * cellSize, position.row * cellSize)

    companion object {
        /** The largest board of [columns] × [rows] square cells that fits in [width] × [height], centred in it. */
        fun fit(
            width: Float,
            height: Float,
            columns: Int,
            rows: Int,
        ): BoardGeometry {
            val cellSize = floor(minOf(width / columns, height / rows))
            val left = ((width - columns * cellSize) / 2).roundToPixel()
            val top = ((height - rows * cellSize) / 2).roundToPixel()
            val origin = Offset(left, top)
            return BoardGeometry(cellSize, origin)
        }
    }
}

private fun Float.roundToPixel(): Float = roundToInt().toFloat()
