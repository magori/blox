package ch.magori.blox.core.board.garden

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant

/**
 * The colours of the musical garden in a Colorful variant. Its own, tokens of docs/design-system/tokens.json
 * (GardenTokensTest): the [band] behind a group of 4 eighths out of two (`garden-band`), a free cell of that group
 * ([bandCell], `garden-band-cell`), the tint of the column played ([playHead], `garden-play-head`), the dot of a note of
 * the chord ([chordDot], `garden-chord-dot`) on its halo ([chordHalo], `garden-chord-halo`). Coded here rather than in
 * [BloxPalette]: the garden is drawn in the Colorful theme only, and the palette is filled for every theme. Those of the
 * palette it draws with: the [board] under the cells, the [ink] of the marks, on a halo of the [page].
 */
internal data class GardenColors(
    val band: Color,
    val bandCell: Color,
    val playHead: Color,
    val chordDot: Color,
    val chordHalo: Color,
    val board: Color,
    val ink: Color,
    val page: Color,
)

/** The colours of the garden in [variant]; there are none in the Original theme. */
internal fun gardenColors(variant: BloxThemeVariant): GardenColors = when (variant) {
    BloxThemeVariant.ORIGINAL -> throw IllegalArgumentException("The garden is drawn in the Colorful theme only.")
    BloxThemeVariant.COLORFUL_LIGHT -> GARDEN_OF_DAY
    BloxThemeVariant.COLORFUL_DARK -> GARDEN_OF_NIGHT
}

/** The sizes of the musical garden, tokens of docs/design-system/tokens.json (GardenTokensTest). */
object GardenSizes {
    /** A cell of the grid of 16 × 14 (`garden-cell`): a bubble of 20 dp in a margin of 1 dp. */
    val cell: Dp = 22.dp

    /** The dot of a note of the chord in a cell of [cell] (`garden-chord-dot-size`), on a halo of 2 dp more. */
    val chordDot: Dp = 6.dp

    /** A cell of the thumbnail of a garden (`garden-thumb-cell`), flat, without a margin. */
    val thumbnailCell: Dp = 3.dp
}

/** The garden by day: a free cell of a band a notch darker than the band, as the empty cell on the board. */
private val GARDEN_OF_DAY = colorsOn(BloxPalettes.ColorfulLight, bandCell = Color(0xFFE3E5EF))

/** The garden by night: a free cell of a band a notch lighter than the band, as the empty cell on the board. */
private val GARDEN_OF_NIGHT = colorsOn(BloxPalettes.ColorfulDark, bandCell = Color(0xFF232634))

/** The colours of the garden on [palette]: its own [bandCell], the others are values of the palette. */
private fun colorsOn(
    palette: BloxPalette,
    bandCell: Color,
): GardenColors = GardenColors(
    band = palette.panelInset,
    bandCell = bandCell,
    playHead = palette.accentRing,
    chordDot = palette.ink,
    chordHalo = palette.page,
    board = palette.boardBackground,
    ink = palette.ink,
    page = palette.page,
)
