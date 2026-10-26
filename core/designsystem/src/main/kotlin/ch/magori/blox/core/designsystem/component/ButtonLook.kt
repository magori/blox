package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxGlosses
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.Gloss

/**
 * The families of the bubble buttons (docs/design-system/README.md, Boutons): at least [minHeight] high and
 * [BloxSizes.minTouchTarget] wide, in `accent` when [isPrimary], else in `button`. The sizes are those of a phone; a
 * tablet's come with P5.84.
 */
enum class ButtonFamily(val minHeight: Dp, internal val isPrimary: Boolean) {
    /** Principal XL: Jouer, a pill of 72 dp. */
    PRIMARY_XL(minHeight = 72.dp, isPrimary = true),

    /** Principal: Reprendre, Rejouer, a pill of 56 dp; one per screen. */
    PRIMARY(minHeight = 56.dp, isPrimary = true),

    /** Secondaire: Menu, Changer, the tiles, a pill of 56 dp. */
    SECONDARY(minHeight = 56.dp, isPrimary = false),

    /** Danger: Effacer (the record), a secondary pill framed and labelled in `danger`, for what cannot be undone. */
    DANGER(minHeight = 56.dp, isPrimary = false),

    /** Icône: a disc of 48 dp, of a pictogram alone (Paramètres, Écouter, Pause). */
    ICON(minHeight = BloxSizes.minTouchTarget, isPrimary = false),
}

/**
 * What a button is: [enabled] or not, [depth], how far down it is, from 0 (at rest) to 1 (pressed), and [focused] by
 * the keyboard or not.
 */
@Immutable
internal data class ButtonState(val enabled: Boolean = true, val depth: Float = 0f, val focused: Boolean = false)

/**
 * How a button is drawn: its [fill], its [ink], its [frame] if it has one, its [gloss], how far down it goes, [drop],
 * and the ring of the keyboard focus around it, [focusRing], if it has the focus.
 */
@Immutable
internal data class ButtonLook(
    val fill: Color,
    val ink: Color,
    val frame: Color?,
    val gloss: Gloss,
    val drop: Dp,
    val focusRing: FocusRing?,
)

/**
 * How a button of [family] is drawn in [state], with the [colors] and the [glosses] of its variant. Only its drop moves
 * on its way back up: its fill, its ink and its relief stay those of a pressed button until it is back at rest, so that
 * a label and its fill never cross (in Original, the fill of a secondary button at rest is the colour of its label
 * pressed). A disabled button is flat and does not go down.
 */
internal fun buttonLook(
    family: ButtonFamily,
    colors: BloxButtonColors,
    glosses: BloxGlosses,
    state: ButtonState,
): ButtonLook {
    val bubble = when {
        !state.enabled -> colors.disabled
        family.isPrimary -> colors.primary
        family == ButtonFamily.DANGER -> colors.danger
        else -> colors.secondary
    }
    val down = state.enabled && state.depth > 0f
    return ButtonLook(
        fill = if (down) bubble.pressedFill else bubble.fill,
        ink = if (down) bubble.pressedInk else bubble.ink,
        frame = bubble.frame,
        gloss = when {
            !state.enabled -> Gloss.None
            down -> glosses.buttonPressed
            else -> glosses.button
        },
        drop = buttonDrop(state.enabled, state.depth),
        focusRing = if (state.focused) FocusRing(colors.focus, BloxBorders.focus, BloxBorders.focusGap) else null,
    )
}

/** How far down a button [depth] down is drawn: [BloxSizes.pressDrop] all the way down, not at all when disabled. */
internal fun buttonDrop(
    enabled: Boolean,
    depth: Float,
): Dp = if (enabled) lerp(0.dp, BloxSizes.pressDrop, depth) else 0.dp

/**
 * How far down a button is, from 0 (at rest) to 1: all the way while [pressed], at once; released, it comes back up
 * in [returnTicks] ticks (`motion-press`), [ticksSinceRelease] after it was let go. With no tick to come back in, it is
 * back at once: what reduced effects will ask for, if they do (P5.71).
 */
internal fun pressDepth(
    pressed: Boolean,
    ticksSinceRelease: Int,
    returnTicks: Int = BloxMotion.press,
): Float = when {
    pressed -> 1f
    ticksSinceRelease >= returnTicks -> 0f
    else -> 1f - ticksSinceRelease.toFloat() / returnTicks
}
