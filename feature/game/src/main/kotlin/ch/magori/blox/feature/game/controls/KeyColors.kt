package ch.magori.blox.feature.game.controls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The colours of a key: its face, [background], its edge, [outline], drawn over the face, and its glyph, [ink]. */
@Immutable
internal data class KeyColors(val background: Color, val outline: Color, val ink: Color)

/**
 * The colours of the key of [button] in [palette]: A in `accent`, the others in `button` framed by
 * `buttonOutline`. A [held] key takes the pressed shade of its colour up to its edge, its glyph unchanged:
 * never the colour of the page.
 */
internal fun keyColors(
    palette: BloxPalette,
    button: ControlButton,
    held: Boolean,
): KeyColors {
    val accent = button == ControlButton.A
    val face = when {
        accent && held -> palette.accentPressed
        accent -> palette.accent
        held -> palette.buttonPressed
        else -> palette.button
    }
    // A has no frame of its own: the accent already stands out from the page by 3:1.
    val outline = if (held || accent) face else palette.buttonOutline
    return KeyColors(face, outline, if (accent) palette.accentInk else palette.buttonInk)
}
