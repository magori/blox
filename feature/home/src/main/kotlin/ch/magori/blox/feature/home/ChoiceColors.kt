package ch.magori.blox.feature.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette

/** The colours of a choice of the home screen: its [background], its [frame] and its label, [ink]. */
@Immutable
internal data class ChoiceColors(val background: Color, val frame: Color, val ink: Color)

/**
 * The colours of a choice in [palette], [selected] or not. A selected choice is filled in `accent`, as the
 * play button; an unselected one is drawn as a panel of the game screen, `panel` framed in `panelBorder`, with
 * its label in `ink`. The two stand out from each other by 3:1 at least in every theme; colour is not the only
 * cue, as the selected choice is also underlined.
 */
internal fun choiceColors(
    palette: BloxPalette,
    selected: Boolean,
): ChoiceColors = if (selected) {
    ChoiceColors(background = palette.accent, frame = palette.accent, ink = palette.accentInk)
} else {
    ChoiceColors(background = palette.panel, frame = palette.panelBorder, ink = palette.ink)
}
