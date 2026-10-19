package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur de l'interface (hors pièces). Les valeurs viennent de la maquette,
 * voir docs/themes.md. Les couleurs des pièces sont associées aux types de pièces
 * dans feature:game, pour que le design system ne dépende pas du domaine.
 */
@Immutable
data class BloxPalette(
    val page: Color,
    val ink: Color,
    val label: Color,
    val panel: Color,
    val panelBorder: Color,
    val frame: Color,
    val boardBackground: Color,
    val button: Color,
    /**
     * The edge of a button at rest, drawn over its [button] face: it stands out from [page] by 3:1 at least
     * (WCAG 1.4.11), as `KeyColorsTest` (feature:game) checks for every theme. In Original it is [button] on
     * purpose: the face already stands out from the page, so no outline shows.
     */
    val buttonOutline: Color,
    val buttonInk: Color,
    /**
     * A button held down: it stands out from [page] by 3:1 at least, [buttonInk] on it too, and from
     * [button], the same button at rest, by 1.5:1 at least.
     */
    val buttonPressed: Color,
    val accent: Color,
    val accentInk: Color,
    /**
     * An accent button held down: it stands out from [page] by 3:1 at least, [accentInk] on it too, and from
     * [accent], the same button at rest, by 1.5:1 at least.
     */
    val accentPressed: Color,
    /** The four cells of the logo of the home screen. */
    val logoCells: LogoColors,
    /** The notes of the melody in the music editor. */
    val melody: TrackColors,
    /** The notes of the bass in the music editor. */
    val bass: TrackColors,
    /** The name of a line of the music editor out of the scale: dimmer than [label], still readable on [page]. */
    val outOfScale: Color,
    /**
     * What deletes: the confirmation button, filled in it, its label in [dangerInk], and the item of a menu, its label in
     * it on [panel]. Red where the palette has one; [ink] in Original, which has none.
     */
    val danger: Color,
    val dangerInk: Color,
)

object BloxPalettes {
    /** Thème « Original » : vert sauge pastel, inspiré des écrans monochromes. */
    val Original = BloxPalette(
        page = Color(0xFFCFE3C8),
        ink = Color(0xFF34503A),
        label = Color(0xFF34503A),
        panel = Color(0xFFBFD6B6),
        panelBorder = Color(0xFF34503A),
        frame = Color(0xFF34503A),
        boardBackground = Color(0xFFDCEBD5),
        button = Color(0xFF5F7F5C),
        // The face, 3.31:1 from the page: no outline to see.
        buttonOutline = Color(0xFF5F7F5C),
        buttonInk = Color(0xFFEEF5EA),
        buttonPressed = Color(0xFF34503A),
        accent = Color(0xFF34503A),
        accentInk = Color(0xFFEEF5EA),
        accentPressed = Color(0xFF1F3324),
        // The four greens of the pieces, the fills of I, O, J and T: dark, medium, light, pale.
        logoCells = LogoColors(
            topLeft = Color(0xFF34503A),
            topRight = Color(0xFF87A67F),
            bottomLeft = Color(0xFFBFD6B6),
            bottomRight = Color(0xFFEEF5EA),
        ),
        // The held tints of the mockup, #6E8F6A and #87A67F, stood out from the cells by 2.9:1 and 2.2:1 only: one
        // darker green, 3.1:1, for both, paler than both attacks.
        // The open tab is filled in the accent, as every selected choice of this palette.
        melody = TrackColors(
            note = Color(0xFF34503A),
            held = Color(0xFF6A8A66),
            tab = Color(0xFF34503A),
            tabInk = Color(0xFFEEF5EA),
        ),
        bass = TrackColors(
            note = Color(0xFF5F7F5C),
            held = Color(0xFF6A8A66),
            tab = Color(0xFF34503A),
            tabInk = Color(0xFFEEF5EA),
        ),
        // #5F7F5C in the mockup, 3.3:1 on the page, too faint for a text: 4.6:1, still dimmer than the label.
        outOfScale = Color(0xFF486848),
        // No red in the greens of the screen: the ink, as the mockup does.
        danger = Color(0xFF34503A),
        dangerInk = Color(0xFFEEF5EA),
    )

    /** Thème « Couleurs » : fond sombre, une couleur vive par pièce. */
    val Colorful = BloxPalette(
        page = Color(0xFF12141C),
        ink = Color(0xFFF1F1F4),
        label = Color(0xFF9AA0B4),
        panel = Color(0xFF1C1F2B),
        panelBorder = Color(0xFF2A2E3E),
        frame = Color(0xFF2A2E3E),
        boardBackground = Color(0xFF0B0C12),
        button = Color(0xFF2A2E3E),
        // The face, 1.36:1 from the page, is lost in it: a rim in the held shade, 3.50:1, draws the key.
        buttonOutline = Color(0xFF646B87),
        buttonInk = Color(0xFFF1F1F4),
        buttonPressed = Color(0xFF646B87),
        accent = Color(0xFFEF8D3B),
        accentInk = Color(0xFF12141C),
        // A darker orange: 1.67:1 from the accent at rest, 4.48:1 from the page and under the ink.
        accentPressed = Color(0xFFC0651C),
        // The fills of the I, O, Z and S pieces.
        logoCells = LogoColors(
            topLeft = Color(0xFF3FC5E8),
            topRight = Color(0xFFF2C641),
            bottomLeft = Color(0xFFE5534B),
            bottomRight = Color(0xFF5BC46A),
        ),
        // The open tab takes the colour of its notes, the label in the ink of the page.
        melody = TrackColors(
            note = Color(0xFF3FC5E8),
            held = Color(0xFF2A8BA6),
            tab = Color(0xFF3FC5E8),
            tabInk = Color(0xFF12141C),
        ),
        bass = TrackColors(
            note = Color(0xFFEF8D3B),
            held = Color(0xFFA8642A),
            tab = Color(0xFFEF8D3B),
            tabInk = Color(0xFF12141C),
        ),
        outOfScale = Color(0xFF7C8193),
        // #E5534B in the mockup, the red of the Z piece, 4.43:1 on the panel, where a menu offers to delete: a lighter
        // red, 5.29:1 there, 5.93:1 under the ink.
        danger = Color(0xFFEB6A62),
        dangerInk = Color(0xFF12141C),
    )
}
