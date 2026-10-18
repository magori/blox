package ch.magori.blox.feature.game.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * How a board cell is drawn: a [fill], an optional [border] and an optional square in the [centre];
 * around them all, an optional [joint], which keeps two cells side by side apart.
 * Widths are fractions of the cell size, so the style does not depend on the screen.
 */
@Immutable
internal data class CellStyle(
    val fill: Color,
    val border: CellBorder?,
    val centre: CellCentre?,
    val joint: CellJoint? = null,
)

/**
 * A frame on each edge of the cell, inside it, in the colour under the cell: the board background, or the
 * panel of the next piece. Its [width] is a fraction of the cell size, 1 px at least.
 */
internal data class CellJoint(val width: Float)

/** A square of [color] in the centre of the cell; its [size] is a fraction of the cell size. */
internal data class CellCentre(val color: Color, val size: Float)

internal sealed interface CellBorder {
    /** Width as a fraction of the cell size. */
    val width: Float

    /** A border of a single [color]. */
    data class Plain(val color: Color, override val width: Float) : CellBorder

    /** A bevel: [light] on the top and left edges, [dark] on the bottom and right edges. */
    data class Bevel(val light: Color, val dark: Color, override val width: Float) : CellBorder
}
