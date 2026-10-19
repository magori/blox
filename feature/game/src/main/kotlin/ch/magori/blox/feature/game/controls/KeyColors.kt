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
    if (!accent && !held) return keyAtRestColors(palette)
    val face = when {
        accent && held -> palette.accentPressed
        accent -> palette.accent
        else -> palette.buttonPressed
    }
    // No frame of its own: the accent already stands out from the page by 3:1, a held key is filled to its edge.
    return KeyColors(face, face, if (accent) palette.accentInk else palette.buttonInk)
}

/**
 * The colours of a key drawn in `button`, not held: its face in `button`, framed by `buttonOutline`, which
 * stands out from the page by 3:1 (WCAG 1.4.11). The pause button of the top bar wears the same.
 */
internal fun keyAtRestColors(palette: BloxPalette): KeyColors =
    KeyColors(palette.button, palette.buttonOutline, palette.buttonInk)
