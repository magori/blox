package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** The contrast ratio of WCAG 2.1 between two colours, from 1 (the same) to 21 (black on white). */
fun contrast(
    first: Color,
    second: Color,
): Double {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * The palette of each variant of each theme, named after the theme as shown on the home screen, for parameterised
 * tests.
 */
val namedPalettes: List<Pair<String, BloxPalette>> = listOf(
    "Original" to BloxPalettes.Original,
    "Couleurs, jour" to BloxPalettes.ColorfulLight,
    "Couleurs, nuit" to BloxPalettes.ColorfulDark,
)
