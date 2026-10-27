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

/**
 * What a switch is: [checked] or not, [enabled] or not, [depth], how far its row is pressed, from 0 to 1, and
 * [focused] by the keyboard or not.
 */
@Immutable
internal data class SwitchState(
    val checked: Boolean,
    val enabled: Boolean = true,
    val depth: Float = 0f,
    val focused: Boolean = false,
)

/**
 * How a switch is drawn: the fill of its row, [rowFill], the ink of its [label] and of its [subLine]; its [track],
 * the frame and the relief of the track, [trackFrame] and [trackGloss]; its [thumb], [thumbSize], at the end of its
 * track if [thumbAtEnd], bearing a [check] if any, and [thumbHollow], drawn as its outline alone, or full; the ring
 * of the keyboard focus around the row, [focusRing], if it has the focus.
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
    val thumbHollow: Boolean,
    val focusRing: FocusRing?,
)

/**
 * How a switch in [state] is drawn in [variant] (docs/design-system/components/Switch). Off, its track is a secondary
 * button at rest (on the page in Original), its thumb in `label`; on, its track is in `accent`, its thumb in
 * `accentInk`, checked in `accent`. Pressed, its row takes the pressed fill and ink of a secondary button, and its
 * track is framed in that ink, which stands out from the fill as `buttonOutline` does from the page. Disabled, it is
 * flat, in `panelInset` framed in `panelBorder`, its thumb and its label in `label`, and it does not press; in
 * Original, where these are the colours of an enabled row, its thumb is hollow.
 */
internal fun switchLook(
    variant: BloxThemeVariant,
    state: SwitchState,
): SwitchLook {
    val palette = paletteOf(variant)
    val buttons = buttonColorsOf(variant)
    val bubble = buttons.secondary
    val on = state.checked
    val size = thumbSize(on, state.enabled, state.depth)
    val focusRing = if (state.focused) buttonFocusRing(buttons.focus) else null
    return if (state.enabled) {
        val down = state.depth > 0f
        // Original, whose outline is the face of its buttons, frames its tracks in ink (P5.57).
        val outline = if (variant == BloxThemeVariant.ORIGINAL) palette.ink else palette.buttonOutline
        SwitchLook(
            rowFill = if (down) bubble.pressedFill else Color.Transparent,
            label = if (down) bubble.pressedInk else palette.ink,
            subLine = if (down) bubble.pressedInk else palette.label,
            track = if (on) palette.accent else bubble.fill,
            trackFrame = if (down) bubble.pressedInk else outline,
            trackGloss = if (on) glossesOf(variant).button else Gloss.None,
            thumb = if (on) palette.accentInk else palette.label,
            thumbSize = size,
            thumbAtEnd = on,
            check = if (on) palette.accent else null,
            thumbHollow = false,
            focusRing = focusRing,
        )
    } else {
        val hollow = variant == BloxThemeVariant.ORIGINAL
        SwitchLook(
            rowFill = Color.Transparent,
            label = palette.label,
            subLine = palette.label,
            track = palette.panelInset,
            trackFrame = palette.panelBorder,
            trackGloss = Gloss.None,
            thumb = palette.label,
            thumbSize = size,
            thumbAtEnd = on,
            // On a hollow thumb, the check takes the colour of its outline: the track shows through the thumb.
            check = if (on) (if (hollow) palette.label else palette.panelInset) else null,
            thumbHollow = hollow,
            focusRing = focusRing,
        )
    }
}

/**
 * The size of the thumb of a switch [checked] or not: 24 dp on, 20 off, growing along its track to 28 dp as its row is
 * [depth] pressed, unless it is not [enabled]. Read at each frame of its way back, without the rest of its look.
 */
internal fun thumbSize(
    checked: Boolean,
    enabled: Boolean,
    depth: Float,
): DpSize {
    val side = if (checked) BloxSizes.switchThumb else OFF_THUMB
    return DpSize(if (enabled) lerp(side, PRESSED_THUMB, depth) else side, side)
}

/** The thumb of a switch off. */
private val OFF_THUMB = 20.dp

/** How long the thumb of a pressed switch grows, along its track. */
private val PRESSED_THUMB = 28.dp

/** The outline of a hollow thumb. */
internal val HOLLOW_THUMB_OUTLINE = 2.dp
