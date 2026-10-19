package ch.magori.blox.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colours of a track of the music editor: the attack of a [note], and the band of its [held] part, in a paler
 * tint (docs/themes.md, Éditeur de musique), both standing out from the cells of the grid, `boardBackground`; and the
 * [tab] of the track when it is open, with its label in [tabInk].
 */
@Immutable
data class TrackColors(val note: Color, val held: Color, val tab: Color, val tabInk: Color)
