package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import ch.magori.blox.core.designsystem.theme.BloxButtonColors
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxGlosses
import ch.magori.blox.core.designsystem.theme.BloxMotion
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BubbleColors
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
    ;

    /**
     * How high a button of this family is at least in [size]: [BloxSizes.minTouchTarget] when it is compact, which only
     * a main or a secondary button may be.
     */
    internal fun minHeight(size: ButtonSize): Dp = when (size) {
        ButtonSize.REGULAR -> minHeight

        ButtonSize.COMPACT -> {
            require(this == PRIMARY || this == SECONDARY) { "Only Principal and Secondaire may be compact, not $this" }
            BloxSizes.minTouchTarget
        }
    }
}

/** How large a button is: [REGULAR], as high as its family says; [COMPACT], 48 dp, for Principal and Secondaire. */
enum class ButtonSize { REGULAR, COMPACT }

/**
 * How a button stands out: [PLAIN], in the colours of its family; [SELECTED], the choice made (a measure, Utiliser),
 * in `accent`; [BARE], a disc of its pictogram alone until it is pressed (« … », − and + of the tempo, a cross).
 */
enum class ButtonEmphasis { PLAIN, SELECTED, BARE }

/**
 * What a button is: [enabled] or not, [depth], how far down it is, from 0 (at rest) to 1 (pressed), [focused] by the
 * keyboard or not, [active], at work (Écouter while it plays), or not, how it stands out, [emphasis], and whether it
 * stands [onInk], the ground of a message, rather than on the page or a panel.
 */
@Immutable
internal data class ButtonState(
    val enabled: Boolean = true,
    val depth: Float = 0f,
    val focused: Boolean = false,
    val active: Boolean = false,
    val emphasis: ButtonEmphasis = ButtonEmphasis.PLAIN,
    val onInk: Boolean = false,
)

/**
 * How a button is drawn: its [fill], its [ink], its [frame] if it has one, its [gloss], and the ring of the keyboard
 * focus around it, [focusRing], if it has the focus. How far down it goes is [buttonDrop]'s alone: read where the
 * button is placed, frame by frame, while these change only twice.
 */
@Immutable
internal data class ButtonLook(
    val fill: Color,
    val ink: Color,
    val frame: Color?,
    val gloss: Gloss,
    val focusRing: FocusRing?,
)

/**
 * How a button of [family] is drawn in [state], with the [colors] and the [glosses] of its variant. Only its drop
 * ([buttonDrop]) moves on its way back up: its fill, its ink and its relief stay those of a pressed button until it is
 * back at rest, so that a label and its fill never cross (in Original, the fill of a secondary button at rest is the
 * colour of its label pressed). An active button looks pressed, but does not go down (its depth stays 0). A bare one
 * has no relief at rest. A disabled button is flat.
 */
internal fun buttonLook(
    family: ButtonFamily,
    colors: BloxButtonColors,
    glosses: BloxGlosses,
    state: ButtonState,
): ButtonLook {
    val bubble = bubbleColors(family, colors, state)
    val held = state.enabled && (state.depth > 0f || state.active)
    return ButtonLook(
        fill = if (held) bubble.pressedFill else bubble.fill,
        ink = if (held) bubble.pressedInk else bubble.ink,
        frame = if (held) bubble.pressedFrame else bubble.frame,
        gloss = when {
            held -> glosses.buttonPressed
            !state.enabled || state.emphasis == ButtonEmphasis.BARE -> Gloss.None
            else -> glosses.button
        },
        focusRing = if (state.focused) buttonFocusRing(if (state.onInk) colors.focusOnInk else colors.focus) else null,
    )
}

/**
 * The colours of a button of [family] in [state], among the [colors] of its variant. A selected button keeps its own
 * disabled: the choice made still shows; a bare one stays bare, its pictogram in the ink of the disabled, and on ink
 * its pictogram is in the colour of the page.
 */
private fun bubbleColors(
    family: ButtonFamily,
    colors: BloxButtonColors,
    state: ButtonState,
): BubbleColors = when (state.emphasis) {
    ButtonEmphasis.SELECTED -> colors.selected

    ButtonEmphasis.BARE -> {
        val bare = if (state.onInk) colors.bareOnInk else colors.bare
        if (state.enabled) bare else bare.copy(ink = colors.disabled.ink)
    }

    ButtonEmphasis.PLAIN -> when {
        !state.enabled -> colors.disabled
        family.isPrimary -> colors.primary
        family == ButtonFamily.DANGER -> colors.danger
        else -> colors.secondary
    }
}

/**
 * How far down a button [depth] down is drawn: [BloxSizes.pressDrop] all the way down; not at all when disabled, nor in
 * reduced [effects], where its fill alone says it is pressed (a key of the game held is another matter: it keeps its
 * drop, HeldKeyTest).
 */
internal fun buttonDrop(
    enabled: Boolean,
    depth: Float,
    effects: BloxEffects,
): Dp = if (enabled && !effects.reduced) lerp(0.dp, BloxSizes.pressDrop, depth) else 0.dp

/** The ticks a released button takes to come back: `motion-press`, or none in reduced [effects], back at once. */
internal fun pressReturnTicks(effects: BloxEffects): Int = if (effects.reduced) 0 else BloxMotion.press

/**
 * How far down a button is, from 0 (at rest) to 1: all the way while [pressed], at once; released, it comes back up
 * in [returnTicks] ticks (`motion-press`), [ticksSinceRelease] after it was let go. With no tick to come back in, it is
 * back at once, as in reduced effects ([pressReturnTicks]).
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
