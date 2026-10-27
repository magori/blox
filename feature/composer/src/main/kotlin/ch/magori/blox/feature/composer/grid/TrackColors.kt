package ch.magori.blox.feature.composer.grid

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxGlosses
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
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

/**
 * The colours of a cell of the grid: its [background], the [line] around it, between it and the cells next to it, the
 * [held] part of a note of the open track (its attack has its own look, [NoteLook]), and the watermark of the other,
 * [echo].
 */
@Immutable
internal data class CellColors(val background: Color, val line: Color, val held: HeldPart, val echo: Color)

/** The colours of a cell of the grid in [palette], the open track being [track]. */
internal fun cellColors(
    palette: BloxPalette,
    track: Track,
): CellColors = CellColors(
    background = palette.cellGround,
    line = palette.gridLine,
    held = heldPart(palette, track),
    echo = trackColors(palette, track.other).note,
)

/** The bubble of a note: lit from [highlight] at its top down to the colour of its track, in the relief [gloss]. */
@Immutable
internal data class NoteBubble(val highlight: Color, val gloss: Gloss)

/**
 * How the attack of a note is drawn: in its [color], as a [bubble] (Colorful, V33), or flat without one (Original, whose
 * grid stays as it was).
 */
@Immutable
internal data class NoteLook(val color: Color, val bubble: NoteBubble?)

/**
 * The look of a note of [track] in [variant], whose colours are [palette] and reliefs [glosses]: in Colorful, a bubble
 * as a cell of a piece (docs/design-system/components/Piece), in the colours of its track and the relief of the cells.
 */
internal fun noteLook(
    variant: BloxThemeVariant,
    palette: BloxPalette,
    glosses: BloxGlosses,
    track: Track,
): NoteLook = with(trackColors(palette, track)) {
    NoteLook(
        color = note,
        bubble = if (variant == BloxThemeVariant.ORIGINAL) null else NoteBubble(highlight, glosses.cell),
    )
}
