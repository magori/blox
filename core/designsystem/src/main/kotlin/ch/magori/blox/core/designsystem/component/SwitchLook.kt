package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.buttonColorsOf
import ch.magori.blox.core.designsystem.theme.glossesOf
import ch.magori.blox.core.designsystem.theme.paletteOf

/** What a switch is: [checked] or not, [enabled] or not, and [depth], how far its row is pressed, from 0 to 1. */
@Immutable
internal data class SwitchState(val checked: Boolean, val enabled: Boolean = true, val depth: Float = 0f)

/**
 * How a switch is drawn: the fill of its row, [rowFill], the ink of its [label] and of its [subLine]; its [track],
 * the frame and the relief of the track, [trackFrame] and [trackGloss]; its [thumb], [thumbSize], at the end of its
 * track if [thumbAtEnd], bearing a [check] if any.
 */
@Immutable
internal data class SwitchLook(
    val rowFill: Color,
    val label: Color,
    val subLine: Color,
    val track: Color,
    val trackFrame: Color,
    val trackGloss: Gloss,
    val thumb: Color,
    val thumbSize: DpSize,
    val thumbAtEnd: Boolean,
    val check: Color?,
)

/**
 * How a switch in [state] is drawn in [variant] (docs/design-system/components/Switch). Off, its track is a secondary
 * button at rest (on the page in Original), its thumb in `label`; on, its track is in `accent`, its thumb in
 * `accentInk`, checked in `accent`. Disabled, it is flat, in `panelInset` framed in `panelBorder`, its thumb and its
 * label in `label`, and it does not press.
 */
internal fun switchLook(
    variant: BloxThemeVariant,
    state: SwitchState,
): SwitchLook {
    val palette = paletteOf(variant)
    val bubble = buttonColorsOf(variant).secondary
    val side = if (state.checked) BloxSizes.switchThumb else OFF_THUMB
    val depth = if (state.enabled) state.depth else 0f
    val down = depth > 0f
    val on = state.checked
    return if (state.enabled) {
        SwitchLook(
            rowFill = if (down) bubble.pressedFill else Color.Transparent,
            label = if (down) bubble.pressedInk else palette.ink,
            subLine = if (down) bubble.pressedInk else palette.label,
            track = if (on) palette.accent else bubble.fill,
            // Original, whose outline is the face of its buttons, frames its tracks in ink (P5.57).
            trackFrame = if (variant == BloxThemeVariant.ORIGINAL) palette.ink else palette.buttonOutline,
            trackGloss = if (on) glossesOf(variant).button else Gloss.None,
            thumb = if (on) palette.accentInk else palette.label,
            thumbSize = DpSize(lerp(side, PRESSED_THUMB, depth), side),
            thumbAtEnd = on,
            check = if (on) palette.accent else null,
        )
    } else {
        SwitchLook(
            rowFill = Color.Transparent,
            label = palette.label,
            subLine = palette.label,
            track = palette.panelInset,
            trackFrame = palette.panelBorder,
            trackGloss = Gloss.None,
            thumb = palette.label,
            thumbSize = DpSize(side, side),
            thumbAtEnd = on,
            check = if (on) palette.panelInset else null,
        )
    }
}

/** The thumb of a switch off. */
private val OFF_THUMB = 20.dp

/** How long the thumb of a pressed switch grows, along its track. */
private val PRESSED_THUMB = 28.dp
