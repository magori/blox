package ch.magori.blox.feature.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette

/**
 * The colours of a choice of the home screen: its [background], its [frame], its label, [ink], and its [check],
 * null when the choice bears none.
 */
@Immutable
internal data class ChoiceColors(val background: Color, val frame: Color, val ink: Color, val check: Color?)

/**
 * The colours of a choice in [palette], [selected] or not. A selected choice is filled in `accent`, as the
 * play button, and checked in the ink of its label; an unselected one is drawn as a panel of the game screen,
 * `panel` framed in `panelBorder`, with its label in `ink`, and no check. Colour is not the only cue (WCAG 2.1,
 * 1.4.1): the selected choice bears a check, and in every theme the two states differ by 3:1 at least in
 * luminance, not in hue only, the unselected choice framed, the selected one filled.
 */
internal fun choiceColors(
    palette: BloxPalette,
    selected: Boolean,
): ChoiceColors = if (selected) {
    ChoiceColors(
        background = palette.accent,
        frame = palette.accent,
        ink = palette.accentInk,
        check = palette.accentInk,
    )
} else {
    ChoiceColors(background = palette.panel, frame = palette.panelBorder, ink = palette.ink, check = null)
}
