package ch.magori.blox.feature.game.controls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The colours of a key: its face, [background], its edge, [outline], drawn over the face, and its glyph, [ink]. */
@Immutable
internal data class KeyColors(val background: Color, val outline: Color, val ink: Color)

/**
 * The colours of the key of [button] in [palette]: A in `accent`, the others in `button` framed by
 * `buttonOutline`, A framed by `accentPressed`. A [held] key takes the pressed shade of its colour and keeps its
 * outline, its glyph unchanged: never the colour of the page.
 */
internal fun keyColors(
    palette: BloxPalette,
    button: ControlButton,
    held: Boolean,
): KeyColors {
    if (button != ControlButton.A) {
        val atRest = keyAtRestColors(palette)
        return if (held) atRest.copy(background = palette.buttonPressed) else atRest
    }
    // Framed in its pressed shade: the accent alone stands out from the page of the day by 2.3:1 only.
    val face = if (held) palette.accentPressed else palette.accent
    return KeyColors(face, palette.accentPressed, palette.accentInk)
}

/**
 * The colours of a key drawn in `button`, not held: its face in `button`, framed by `buttonOutline`, which
 * stands out from the page by 3:1 (WCAG 1.4.11). The pause button of the top bar wears the same.
 */
internal fun keyAtRestColors(palette: BloxPalette): KeyColors =
    KeyColors(palette.button, palette.buttonOutline, palette.buttonInk)
