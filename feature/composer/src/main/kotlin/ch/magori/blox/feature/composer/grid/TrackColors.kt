package ch.magori.blox.feature.composer.grid

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
