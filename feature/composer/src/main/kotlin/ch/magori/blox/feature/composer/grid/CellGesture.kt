package ch.magori.blox.feature.composer.grid

import ch.magori.blox.feature.composer.model.CELLS_PER_BAR
import kotlin.math.abs

/** What a finger did on a line of the grid, once lifted. */
internal sealed interface CellGesture {
    /** It touched the [eighth] cell, counted from 0, without sliding. */
    data class Touch(val eighth: Int) : CellGesture

    /** It touched the [fromEighth] cell and slid along the line to the [toEighth] cell. */
    data class Slide(val fromEighth: Int, val toEighth: Int) : CellGesture
}

/**
 * Follows one finger on a line of eight cells [cellWidth] wide, from where it went down, [downX] and [downY]. A finger
 * lifted within [touchSlop] touches its cell; one slid farther along the line slides to the cell where it is lifted,
 * unless it is brought back to its own cell, which does nothing. A finger moved up or down before sliding is left to the
 * scroll of the grid, and a cancelled gesture (the system back gesture, another gesture that takes it) does nothing.
 */
internal class CellGestureTracker(
    private val cellWidth: Float,
    private val touchSlop: Float,
    private val downX: Float,
    private val downY: Float,
) {
    private val from = cellAt(downX)
    private var to = from

    /** Whether the finger slid along the line farther than the touch slop. */
    var isSliding: Boolean = false
        private set

    /** Whether the gesture was given up: it will do nothing. */
    var isAbandoned: Boolean = false
        private set

    /** The finger moved to [x], [y]. */
    fun move(
        x: Float,
        y: Float,
    ) {
        if (isAbandoned) return
        val dx = abs(x - downX)
        val dy = abs(y - downY)
        if (!isSliding && dy > touchSlop && dy >= dx) {
            isAbandoned = true
            return
        }
        if (dx > touchSlop) isSliding = true
        if (isSliding) to = cellAt(x)
    }

    /** The gesture was cancelled, or taken by another one. */
    fun cancel() {
        isAbandoned = true
    }

    /** What the finger did, once lifted where it last moved; `null` for nothing. */
    fun lift(): CellGesture? = when {
        isAbandoned -> null
        !isSliding -> CellGesture.Touch(from)
        to == from -> null
        else -> CellGesture.Slide(from, to)
    }

    private fun cellAt(x: Float) = (x / cellWidth).toInt().coerceIn(0, CELLS_PER_BAR - 1)
}
