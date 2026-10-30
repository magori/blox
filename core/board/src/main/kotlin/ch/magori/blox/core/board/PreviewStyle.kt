package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import ch.magori.blox.core.designsystem.theme.Gloss

/**
 * The preview of a piece [placed] in this style, before it is placed, as the Locksmith and the musical garden show it:
 * the same bubble, faded over [under], the free cell, flat and outlined in [ink] of [outlineWidth], so that it differs
 * from a placed piece by more than its colour; when it does not [fits], a cross in [ink] of [crossWidth] on a halo of
 * the [page], so that the refusal does not rest on colour. The widths are fractions of the cell size.
 */
internal fun previewStyle(
    placed: CellStyle,
    under: Color,
    ink: Color,
    page: Color,
    outlineWidth: Float,
    crossWidth: Float,
    fits: Boolean,
): CellStyle {
    val faded = { color: Color -> color.copy(alpha = PREVIEW_ALPHA).compositeOver(under) }
    return placed.copy(
        fill = faded(placed.fill),
        bubble = placed.bubble?.let { CellBubble(highlight = it.highlight?.let(faded), gloss = Gloss.None) },
        border = CellBorder(ink, outlineWidth),
        cross = if (fits) null else CellCross(color = ink, halo = page, width = crossWidth),
    )
}

/** The colours of a preview over the free cell: the piece shows through, apart from the pieces placed. */
private const val PREVIEW_ALPHA = 0.6f
