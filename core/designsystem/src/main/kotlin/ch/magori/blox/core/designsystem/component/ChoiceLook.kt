package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxButtonColors

/**
 * How a segment of a group of choices is drawn: its [fill], null when clear on its group, its label, [ink], and its
 * [check], null when it bears none.
 */
@Immutable
internal data class SegmentLook(val fill: Color?, val ink: Color, val check: Color?)

/**
 * How a segment is drawn with the [colors] of the buttons of its variant (docs/design-system/README.md, Groupe de
 * choix): [selected], it is filled as a main button, in `accent`, and checked in the ink of its label; else clear on
 * its group, its label in the ink of a secondary button, or in `label`, as a disabled button, when it is not
 * [available]. Colour is not the only cue (WCAG 2.1, 1.4.1): the selected segment alone bears a check.
 */
internal fun segmentLook(
    colors: BloxButtonColors,
    selected: Boolean,
    available: Boolean,
): SegmentLook = when {
    selected -> SegmentLook(fill = colors.primary.fill, ink = colors.primary.ink, check = colors.primary.ink)
    available -> SegmentLook(fill = null, ink = colors.secondary.ink, check = null)
    else -> SegmentLook(fill = null, ink = colors.disabled.ink, check = null)
}

/** How a group of choices is drawn: its [fill], behind its segments, and its [frame], around them all. */
@Immutable
internal data class GroupLook(val fill: Color, val frame: Color)

/**
 * How a group of choices is drawn with the [colors] of the buttons of its variant: as a secondary button, filled in
 * `button` and framed by `button-outline` (in Original, on the page and framed by the face of its buttons).
 */
internal fun groupLook(colors: BloxButtonColors): GroupLook = colors.secondary.let { button ->
    GroupLook(fill = button.fill, frame = checkNotNull(button.frame) { "A secondary button is framed." })
}
