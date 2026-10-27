package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color

/**
 * The soft shadow of the day, rgba(90,80,160,0.3), under the letters of the logo and under the mascots
 * (docs/design-system/pictogrammes.md); blurred, it never draws a hard edge.
 */
internal val SOFT_SHADOW: Color = Color(red = 90, green = 80, blue = 160).copy(alpha = 0.3f)
