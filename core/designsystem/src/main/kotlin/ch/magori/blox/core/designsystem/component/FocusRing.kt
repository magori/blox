package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.grownOutline

/** The ring of the keyboard focus around a button or a row: [width] wide in [color], [gap] off its edge. */
@Immutable
internal data class FocusRing(val color: Color, val width: Dp, val gap: Dp)

/** Draws [ring], if any, around [shape], off its edge: drawn outside the bounds, it takes no room. Built once a size. */
internal fun Modifier.focusRing(
    ring: FocusRing?,
    shape: Shape,
): Modifier = if (ring == null) this else focusRing(shape) { ring }

/**
 * Draws the ring [ring] gives, if any, around [shape], as above; [ring] is read where the ring is drawn, so that the
 * ring comes and goes without composing again. Built once a size and a ring.
 */
internal fun Modifier.focusRing(
    shape: Shape,
    ring: () -> FocusRing?,
): Modifier = drawWithCache {
    val current = ring() ?: return@drawWithCache onDrawBehind {}
    val width = current.width.toPx()
    val outline = grownOutline(shape, size, current.gap.toPx() + width / 2, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val stroke = Stroke(width)
    onDrawBehind { drawPath(path, current.color, style = stroke) }
}

/** The ring of the buttons, in the `focus` of [colors], around what the keyboard [focused]; none otherwise. */
internal fun buttonFocusRing(
    focused: Boolean,
    colors: BloxButtonColors,
): FocusRing? = if (focused) FocusRing(colors.focus, BloxBorders.focus, BloxBorders.focusGap) else null
