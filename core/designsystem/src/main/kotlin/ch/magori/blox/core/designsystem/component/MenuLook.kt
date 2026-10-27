package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** How an item of a menu is drawn: its [fill] behind it, and its [ink], that of its icon and its label. */
@Immutable
internal data class MenuItemLook(val fill: Color, val ink: Color)

/**
 * How an item of a menu is drawn in [palette]: on the panel in `ink`, or in `danger` when it deletes. [pressed], it
 * takes the fill and the ink of a pressed secondary button, or, when it deletes, of a pressed danger button: `danger`
 * on `button-pressed` would not be read (4,3 to 1 by day).
 */
internal fun menuItemLook(
    palette: BloxPalette,
    danger: Boolean,
    pressed: Boolean,
): MenuItemLook = when {
    pressed && danger -> MenuItemLook(fill = palette.danger, ink = palette.dangerInk)
    pressed -> MenuItemLook(fill = palette.buttonPressed, ink = palette.buttonInk)
    danger -> MenuItemLook(fill = Color.Transparent, ink = palette.danger)
    else -> MenuItemLook(fill = Color.Transparent, ink = palette.ink)
}

/**
 * Where the [panel] of a menu opened by [anchor] stands in its [window], its top left corner: under the anchor, its end
 * at the end of the anchor; above it when there is no room under it; within the window always, against its edges if
 * need be.
 */
internal fun menuPosition(
    anchor: IntRect,
    window: IntSize,
    panel: IntSize,
): IntOffset {
    val x = anchor.right - panel.width
    val y = when {
        anchor.bottom + panel.height <= window.height -> anchor.bottom
        anchor.top - panel.height >= 0 -> anchor.top - panel.height
        else -> window.height - panel.height
    }
    return IntOffset(
        x = x.coerceIn(0, (window.width - panel.width).coerceAtLeast(0)),
        y = y.coerceIn(0, (window.height - panel.height).coerceAtLeast(0)),
    )
}
