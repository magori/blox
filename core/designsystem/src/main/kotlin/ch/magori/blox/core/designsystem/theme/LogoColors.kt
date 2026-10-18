package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The four cells of the logo of the home screen, a square of 2 × 2 cells: exactly four colours, one per
 * position. They take colours of the pieces of the theme (docs/themes.md); a test of feature:game, which
 * knows the pieces, keeps them in step with the piece styles.
 */
@Immutable
data class LogoColors(val topLeft: Color, val topRight: Color, val bottomLeft: Color, val bottomRight: Color)
