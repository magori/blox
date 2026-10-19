package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Les tailles partagées par les écrans. */
object BloxSizes {
    /**
     * La plus petite cible tactile, en largeur comme en hauteur : 48 dp visibles, une règle de BloX plus stricte
     * que WCAG 2.2 (2.5.8, 24 px) et qu'Android, dont les 48 dp sont une zone tactile qui peut déborder du dessin.
     */
    val minTouchTarget = 48.dp
}
