package ch.magori.blox.feature.composer.grid

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.TrackColors
import ch.magori.blox.feature.composer.model.Track

/** The colours of the notes of [track] in [palette]. */
internal fun trackColors(
    palette: BloxPalette,
    track: Track,
): TrackColors = when (track) {
    Track.MELODY -> palette.melody
    Track.BASS -> palette.bass
}

/**
 * The outline of the column played: a [line], with a [halo] inside it, so that one of them stands out from the cells,
 * empty or filled, whatever their colour.
 */
@Immutable
internal data class PlayedOutline(val line: Color, val halo: Color)

/** The outline of the column played while the music is listened to, in [palette]: the ink of the page, in the cells. */
internal fun playedOutline(palette: BloxPalette): PlayedOutline = PlayedOutline(palette.ink, palette.boardBackground)

/**
 * The held part of a note: a [band] in the paler tint of its track, edged above and below in the colour of its track,
 * [edge], which stands out from the cell where the band alone may not (the pastels of the day, 2.1:1 to 2.3:1).
 */
@Immutable
internal data class HeldPart(val band: Color, val edge: Color)

/** The held part of a note of [track] in [palette]. */
internal fun heldPart(
    palette: BloxPalette,
    track: Track,
): HeldPart = with(trackColors(palette, track)) { HeldPart(band = held, edge = note) }
