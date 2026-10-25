package ch.magori.blox.core.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.designsystem.theme.Gloss

/**
 * How a board cell is drawn: a [fill], an optional [border] and an optional square in the [centre];
 * around them all, an optional [joint], which keeps two cells side by side apart. A [bubble] rounds the
 * fill and lights it. Widths are fractions of the cell size, so the style does not depend on the screen.
 */
@Immutable
internal data class CellStyle(
    val fill: Color,
    val border: CellBorder?,
    val centre: CellCentre?,
    val joint: CellJoint? = null,
    val bubble: CellBubble? = null,
)

/**
 * A bubble (docs/design-system/components/Piece): a fill with rounded corners, lit from the top in [highlight] when
 * there is one, its [gloss] over it. Its corners and its gloss depend on the size of the cell ([cellLayout]).
 */
internal data class CellBubble(val highlight: Color?, val gloss: Gloss)

/**
 * A frame on each edge of the cell, inside it, in the colour under the cell: the board background, or the
 * panel of the next piece. 1 px at least.
 */
internal sealed interface CellJoint {
    /** A joint of [width], a fraction of the cell size: the flat cells of Original. */
    data class Fraction(val width: Float) : CellJoint

    /** A joint of [width] whatever the size of the cell: the margin of a bubble, 1 px of the design system. */
    data class Fixed(val width: Dp) : CellJoint
}

/** A square of [color] in the centre of the cell; its [size] is a fraction of the cell size. */
internal data class CellCentre(val color: Color, val size: Float)

/** A border of [color] inside the fill; its [width] is a fraction of the cell size. */
internal data class CellBorder(val color: Color, val width: Float)
