package ch.magori.blox.core.board.lock

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellStyle
import ch.magori.blox.core.board.cellLayout
import ch.magori.blox.core.board.crossHalo
import ch.magori.blox.core.board.wholeCross
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.PieceType
import kotlin.math.floor

/**
 * The cross of a refused preview in a cell cut at the edge of the screen, in pixels.
 *
 * @property side the side of the square its two diagonals span, from end to end of their strokes' axes.
 * @property halo the width of the stroke of the page drawn under each diagonal; its round ends pass the square by half
 *   of it on each side.
 * @property fill the width of the fill of the cut cell, between its two joints, that the cross and its halo must hold.
 */
@Immutable
data class LockCutCross(val side: Float, internal val halo: Float, internal val fill: Float) {
    /** The side of the square the cross covers with its halo, its round ends included. */
    internal val reach: Float get() = side + halo

    /** Whether the cross and its halo hold whole in [width] pixels: what is shown of a cell narrowed by a shake. */
    fun fitsWithin(width: Float): Boolean = reach <= width
}

/**
 * The cross of a refused preview in a cell of a padlock of [side] pixels cut to [width] pixels, at [density]: the fill
 * less the halo and [CROSS_GAP] on each side, in whole pixels, so that it never covers the outline; at most the cross of
 * a whole cell, across half its fill, as the cells that are not cut. The same in every Colorful variant and for every
 * piece: the joint and the stroke depend on neither (LockCutCrossTest).
 */
fun lockCutCross(
    side: Float,
    width: Float,
    density: Density,
): LockCutCross = lockCutCross(
    side,
    width,
    density,
    lockCellStyle(BloxThemeVariant.COLORFUL_LIGHT, LockCell.Preview(PieceType.T, fits = false)),
)

/** The cross of a refused preview in [style], as [lockCutCross] takes it. */
internal fun lockCutCross(
    side: Float,
    width: Float,
    density: Density,
    style: CellStyle,
): LockCutCross {
    val fill = cellLayout(style, side, density, width).fill
    val halo = crossHalo(checkNotNull(style.cross), side)
    val gap = with(density) { CROSS_GAP.toPx() }
    val cross = minOf(floor(fill.width - halo - 2 * gap), wholeCross(fill.height))
    return LockCutCross(side = cross, halo = halo, fill = fill.width)
}

/** What is left of the fill between the halo of the cross and each side of a cut cell. */
private val CROSS_GAP = 2.dp
