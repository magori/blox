package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import ch.magori.blox.core.designsystem.theme.BloxBorders
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
 * 1.4.1): the state is told by the check, which the selected choice alone bears and which stands out from its
 * background by 3:1 at least in every theme. The fills of the two states do not reach 3:1 between them in every
 * theme (2.6:1 by day): the unselected choice is framed, the selected one filled, but the check is the cue.
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

/** The background and the frame of a choice, in [colors], of [shape]. */
internal fun Modifier.panelFrame(
    colors: ChoiceColors,
    shape: Shape,
): Modifier = background(colors.background, shape).border(BloxBorders.frame, colors.frame, shape)
