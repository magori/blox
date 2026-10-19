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
