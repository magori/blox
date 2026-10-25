package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Les tailles partagées par les écrans : les jetons `size` de docs/design-system/tokens.json (ShapeTokensTest). */
object BloxSizes {
    /**
     * La plus petite cible tactile, en largeur comme en hauteur : 48 dp visibles, une règle de BloX plus stricte
     * que WCAG 2.2 (2.5.8, 24 px) et qu'Android, dont les 48 dp sont une zone tactile qui peut déborder du dessin.
     * Jeton `touch-target`.
     */
    val minTouchTarget = 48.dp

    /** Entre une fenêtre (`WindowDialog`) et les bords de l'écran. */
    val windowMargin = 16.dp

    /** The least height of a row of the settings (`setting-row`). */
    val settingRow = 56.dp

    /** The least height of a row of the settings on a tablet (`setting-row-tablet`). */
    val settingRowTablet = 64.dp

    /** The width of the track of a switch, a pill (`switch-track`). */
    val switchTrack = 52.dp

    /** The thumb of a switch on, with its check (`switch-thumb`). */
    val switchThumb = 24.dp

    /** The side of the arrow keys of the game on a phone (`key-arrow`). */
    val keyArrow = 60.dp

    /** The side of the arrow keys in a narrow window (`key-arrow-compact`). */
    val keyArrowCompact = 52.dp

    /** The side of the arrow keys on a tablet (`key-arrow-tablet`). */
    val keyArrowTablet = 76.dp

    /** The diameter of the keys B and A on a phone (`key-rotate`). */
    val keyRotate = 68.dp

    /** The diameter of the keys B and A in a narrow window (`key-rotate-compact`). */
    val keyRotateCompact = 56.dp

    /** The diameter of the keys B and A on a tablet (`key-rotate-tablet`). */
    val keyRotateTablet = 84.dp
}

/** The spaces, on a grid of 4: the `spacing` tokens of docs/design-system/tokens.json. There is no `space-5`. */
object BloxSpaces {
    /** Between the label and the value of a panel; between the bars of the gauge of the level. */
    val space1 = 4.dp

    /** Between two keys, two panels, two choices. */
    val space2 = 8.dp

    /** The inner margin, left and right, of a choice. */
    val space3 = 12.dp

    /** Between the board and the column of panels; between two sections; the margin of the settings. */
    val space4 = 16.dp

    /** The margin of the game screen on a tablet and lying; the margin of the cards of the pause and of the end. */
    val space6 = 24.dp
}
