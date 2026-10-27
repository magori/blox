package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colours of a bubble button: its [fill] and [ink] at rest, [pressedFill] and [pressedInk] held, its [frame] at rest
 * if it has one, and its [pressedFrame] held, the same unless said otherwise.
 */
@Immutable
data class BubbleColors(
    val fill: Color,
    val ink: Color,
    val pressedFill: Color,
    val pressedInk: Color,
    val frame: Color?,
    val pressedFrame: Color? = frame,
)

/**
 * The colours of the bubble buttons of a variant (docs/design-system/README.md, Boutons): [primary] for Principal XL
 * and Principal, [secondary] for Secondaire and Icône, [danger] for what cannot be undone (Effacer, the record), filled
 * as a secondary button, [dangerFilled] for the answer of a window that deletes, filled in `danger` (held, only its
 * relief and its drop change: tokens.json has no pressed danger), [selected] for any of them that is the choice made
 * (a measure, Utiliser), [bare] for a disc that shows nothing but its pictogram until it is pressed (« … », − and + of
 * the tempo, the cross of a message), [disabled] for any of them that cannot be touched, and the ring of the keyboard
 * focus, [focus].
 */
@Immutable
data class BloxButtonColors(
    val primary: BubbleColors,
    val secondary: BubbleColors,
    val danger: BubbleColors,
    val dangerFilled: BubbleColors,
    val selected: BubbleColors,
    val bare: BubbleColors,
    val disabled: BubbleColors,
    val focus: Color,
)

internal object BloxButtonColorSets {
    /**
     * The Original theme. Its `buttonInk` is for pictograms on `button` (4.0:1, enough for them, not for a label): a
     * secondary button stands at rest on the page, its label in `ink`, framed by `buttonOutline` (3.3:1 on the page),
     * as before Couleurs v2, and is filled in `buttonPressed` when held; so does a danger button, in `danger`.
     */
    val Original = BloxPalettes.Original.let { palette ->
        val colors = of(palette)
        colors.copy(
            secondary = colors.secondary.copy(fill = palette.page, ink = palette.ink),
            danger = colors.danger.copy(fill = palette.page),
        )
    }

    /** The Colorful theme by day. */
    val ColorfulLight = of(BloxPalettes.ColorfulLight)

    /** The Colorful theme by night. */
    val ColorfulDark = of(BloxPalettes.ColorfulDark)

    /** The buttons in [palette]: the roles of docs/design-system/README.md. */
    private fun of(palette: BloxPalette) = BloxButtonColors(
        primary = BubbleColors(palette.accent, palette.accentInk, palette.accentPressed, palette.accentInk, null),
        secondary = BubbleColors(
            palette.button,
            palette.buttonInk,
            palette.buttonPressed,
            palette.buttonInk,
            palette.buttonOutline,
        ),
        danger = BubbleColors(palette.button, palette.danger, palette.danger, palette.dangerInk, palette.danger),
        dangerFilled = BubbleColors(
            palette.danger,
            palette.dangerInk,
            palette.danger,
            palette.dangerInk,
            palette.danger,
        ),
        selected = BubbleColors(
            palette.accent,
            palette.accentInk,
            palette.accentPressed,
            palette.accentInk,
            palette.accentPressed,
        ),
        bare = BubbleColors(
            Color.Transparent,
            palette.ink,
            palette.buttonPressed,
            palette.buttonInk,
            frame = null,
            pressedFrame = palette.buttonOutline,
        ),
        disabled = flat(palette.panelInset, palette.label, palette.panelBorder),
        focus = palette.ink,
    )

    /** What does not move when pressed: [fill], [ink] and [frame], held as at rest. */
    private fun flat(
        fill: Color,
        ink: Color,
        frame: Color,
    ) = BubbleColors(fill, ink, fill, ink, frame)
}
