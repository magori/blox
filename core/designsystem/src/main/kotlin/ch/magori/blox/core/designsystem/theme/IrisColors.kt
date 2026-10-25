package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The three stops of the iridescent gradient (`iris-1` to `iris-3` of the design system): the logo, the sheen of a clear
 * of four lines, the bar of a record beaten. A decoration only, never behind a text. A palette without iridescence
 * gives the same colour to the three stops: a flat colour.
 */
@Immutable
data class IrisColors(val start: Color, val middle: Color, val end: Color)
