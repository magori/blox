package ch.magori.blox.feature.game.controls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The colours of a key: its face, [background], and its glyph, [ink]. */
@Immutable
internal data class KeyColors(val background: Color, val ink: Color)

/**
 * The colours of the key of [button] in [palette]: A in `accent`, the others in `button`. A [held]
 * key takes the pressed shade of its colour, its glyph unchanged: never the colour of the page.
 */
internal fun keyColors(
    palette: BloxPalette,
    button: ControlButton,
    held: Boolean,
): KeyColors = when (button) {
    ControlButton.A -> KeyColors(if (held) palette.accentPressed else palette.accent, palette.accentInk)
    else -> KeyColors(if (held) palette.buttonPressed else palette.button, palette.buttonInk)
}
