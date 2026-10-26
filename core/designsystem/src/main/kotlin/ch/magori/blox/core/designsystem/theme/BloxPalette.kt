package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur de l'interface (hors pièces). Les valeurs du thème Couleurs, jour et nuit, viennent du design
 * system (docs/design-system/tokens.json, comparé à ce fichier par PaletteTokensTest, ADR 0019) ; celles du thème
 * Original, de docs/themes.md. Les couleurs des pièces, et le logo qui les reprend, sont associées aux types de pièces
 * dans core:board (ADR 0023), pour que le design system ne dépende pas du domaine.
 */
@Immutable
data class BloxPalette(
    val page: Color,
    val ink: Color,
    val label: Color,
    val panel: Color,
    val panelBorder: Color,
    /** A tile laid in a card (the statistics of the pause and of the end of a game), and the track of a disabled switch. */
    val panelInset: Color,
    val boardBackground: Color,
    /** An empty cell of the board, drawn on [boardBackground]. */
    val emptyCell: Color,
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
    /** The halo of the Play button, a decoration; transparent where the palette has no halo. */
    val accentRing: Color,
    /** The veil behind a dialog window, a sheet or the card of the end of a game. */
    val scrim: Color,
    /** The rows full while they are cleared. */
    val clearGlow: Color,
    /** The cells of the stack once the game is over, put out. */
    val cellDead: Color,
    /** The iridescent gradient: the logo, the sheen of four lines, the bar of a record beaten. */
    val iris: IrisColors,
    /** A cell of the grid of the music editor, under its note. */
    val cellGround: Color,
    /** The lines between the cells of the grid of the music editor. */
    val gridLine: Color,
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
    /** The tint of the top rows while the stack is in danger, over the board. */
    val dangerSoft: Color,
)

object BloxPalettes {
    /** Thème « Original » : vert sauge pastel, inspiré des écrans monochromes. */
    val Original = BloxPalette(
        page = Color(0xFFCFE3C8),
        ink = Color(0xFF34503A),
        label = Color(0xFF34503A),
        panel = Color(0xFFBFD6B6),
        panelBorder = Color(0xFF34503A),
        panelInset = Color(0xFFDCEBD5),
        boardBackground = Color(0xFFDCEBD5),
        // The background of the board: an empty cell is not drawn apart.
        emptyCell = Color(0xFFDCEBD5),
        button = Color(0xFF5F7F5C),
        // The face, 3.31:1 from the page: no outline to see.
        buttonOutline = Color(0xFF5F7F5C),
        buttonInk = Color(0xFFEEF5EA),
        buttonPressed = Color(0xFF34503A),
        accent = Color(0xFF34503A),
        accentInk = Color(0xFFEEF5EA),
        accentPressed = Color(0xFF1F3324),
        // No halo around Play in Original.
        accentRing = Color.Transparent,
        // rgba(207,227,200,0.85): the page.
        scrim = Color(0xFFCFE3C8).copy(alpha = 0.85f),
        clearGlow = Color(0xFFEEF5EA),
        cellDead = Color(0xFFBFD6B6),
        // No iridescence: the ink, flat.
        iris = IrisColors(start = Color(0xFF34503A), middle = Color(0xFF34503A), end = Color(0xFF34503A)),
        // The cells of the board, between lines of the page.
        cellGround = Color(0xFFDCEBD5),
        gridLine = Color(0xFFCFE3C8),
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
        // rgba(52,80,58,0.14): the ink, faint; the danger reads in the frame of the board.
        dangerSoft = Color(0xFF34503A).copy(alpha = 0.14f),
    )

    /** Thème « Couleurs », le jour : nacre, lilas et pastels brillants. */
    val ColorfulLight = BloxPalette(
        page = Color(0xFFEEF0F7),
        ink = Color(0xFF1C1B2E),
        label = Color(0xFF4F4E68),
        panel = Color(0xFFFFFFFF),
        panelBorder = Color(0xFFC9CCDC),
        panelInset = Color(0xFFF1F2F8),
        boardBackground = Color(0xFFFFFFFF),
        emptyCell = Color(0xFFF1F2F8),
        button = Color(0xFFFFFFFF),
        buttonOutline = Color(0xFF7A7894),
        buttonInk = Color(0xFF1C1B2E),
        buttonPressed = Color(0xFFD9C2FF),
        accent = Color(0xFFB48CFF),
        accentInk = Color(0xFF1C1B2E),
        accentPressed = Color(0xFF9A6CF0),
        // rgba(180,140,255,0.55): the accent.
        accentRing = Color(0xFFB48CFF).copy(alpha = 0.55f),
        // rgba(238,240,247,0.85): the page.
        scrim = Color(0xFFEEF0F7).copy(alpha = 0.85f),
        clearGlow = Color(0xFFFFFFFF),
        cellDead = Color(0xFFD9DBE6),
        iris = IrisColors(start = Color(0xFFC9A7FF), middle = Color(0xFF9FF0D0), end = Color(0xFFFFC6A8)),
        cellGround = Color(0xFFF6F7FB),
        gridLine = Color(0xFFFFFFFF),
        // The open tab takes the colour of its notes.
        melody = TrackColors(
            note = Color(0xFF7C4DEB),
            held = Color(0xFFB7A0F2),
            tab = Color(0xFF7C4DEB),
            tabInk = Color(0xFFFFFFFF),
        ),
        bass = TrackColors(
            note = Color(0xFFB4501A),
            held = Color(0xFFE8A57C),
            tab = Color(0xFFB4501A),
            tabInk = Color(0xFFFFFFFF),
        ),
        outOfScale = Color(0xFF6A6982),
        danger = Color(0xFFC2264B),
        dangerInk = Color(0xFFFFFFFF),
        // rgba(194,38,75,0.14): the danger.
        dangerSoft = Color(0xFFC2264B).copy(alpha = 0.14f),
    )

    /** Thème « Couleurs », la nuit : fond profond, vert acide et néons. */
    val ColorfulDark = BloxPalette(
        page = Color(0xFF07080D),
        ink = Color(0xFFF2F4FA),
        label = Color(0xFFA9AEC2),
        panel = Color(0xFF141621),
        panelBorder = Color(0xFF2A2D3A),
        panelInset = Color(0xFF1B1D28),
        boardBackground = Color(0xFF0E0F16),
        emptyCell = Color(0xFF14151E),
        button = Color(0xFF1B1D28),
        buttonOutline = Color(0xFF6B7088),
        buttonInk = Color(0xFFF2F4FA),
        buttonPressed = Color(0xFF3A3E50),
        accent = Color(0xFFC6FF3D),
        accentInk = Color(0xFF07080D),
        accentPressed = Color(0xFF8CC40F),
        // rgba(198,255,61,0.5): the accent.
        accentRing = Color(0xFFC6FF3D).copy(alpha = 0.5f),
        // rgba(7,8,13,0.8): the page.
        scrim = Color(0xFF07080D).copy(alpha = 0.8f),
        // rgba(242,244,250,0.9): the ink.
        clearGlow = Color(0xFFF2F4FA).copy(alpha = 0.9f),
        cellDead = Color(0xFF2A2D3A),
        iris = IrisColors(start = Color(0xFFC6FF3D), middle = Color(0xFF3DFFD0), end = Color(0xFFA77BFF)),
        cellGround = Color(0xFF12131B),
        gridLine = Color(0xFF141621),
        melody = TrackColors(
            note = Color(0xFFA77BFF),
            held = Color(0xFF6B4FB0),
            tab = Color(0xFFA77BFF),
            tabInk = Color(0xFF07080D),
        ),
        bass = TrackColors(
            note = Color(0xFFFF9E3D),
            held = Color(0xFFA8662A),
            tab = Color(0xFFFF9E3D),
            tabInk = Color(0xFF07080D),
        ),
        outOfScale = Color(0xFF7E8399),
        danger = Color(0xFFFF4D6D),
        dangerInk = Color(0xFF07080D),
        // rgba(255,77,109,0.18): the danger.
        dangerSoft = Color(0xFFFF4D6D).copy(alpha = 0.18f),
    )
}
