package ch.magori.blox.core.board.lock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.CellBorder
import ch.magori.blox.core.board.CellBubble
import ch.magori.blox.core.board.CellCentre
import ch.magori.blox.core.board.CellCross
import ch.magori.blox.core.board.CellDot
import ch.magori.blox.core.board.CellStyle
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.designsystem.theme.GlossLayer
import ch.magori.blox.core.game.model.Cell

/**
 * The colours of the padlock in a Colorful variant. Its own, tokens of docs/design-system/tokens.json (LockTokensTest):
 * its [metal] (`lock-metal`), the [rivet] of a metal cell (`lock-rivet`), the inner shadow that sinks a cell of the
 * hole ([holeInset], `lock-hole-inset`) and the tint of a cell marked by a hint ([hintFill], `lock-hint`). Coded here
 * rather than in [BloxPalette]: the Locksmith is drawn in the Colorful theme only, and the palette is filled for every
 * theme. Those of the palette it marks with: the [ink] of a mark, on a halo of the [page].
 */
internal data class LockColors(
    val metal: Color,
    val rivet: Color,
    val holeInset: Gloss,
    val hintFill: Color,
    val ink: Color,
    val page: Color,
)

/** The colours of the padlock in [variant]; there are none in the Original theme. */
internal fun lockColors(variant: BloxThemeVariant): LockColors = when (variant) {
    BloxThemeVariant.ORIGINAL -> throw IllegalArgumentException("The padlock is drawn in the Colorful theme only.")
    BloxThemeVariant.COLORFUL_LIGHT -> LOCK_OF_DAY
    BloxThemeVariant.COLORFUL_DARK -> LOCK_OF_NIGHT
}

/**
 * The metal of the padlock in [variant], a Colorful variant (`lock-metal`): the colour of the rim the screen draws
 * around the cells, so that rim and metal cells make one plate.
 */
fun lockMetal(variant: BloxThemeVariant): Color = lockColors(variant).metal

/**
 * The style of [cell] of a padlock in [variant], a Colorful variant, drawn on the metal ([LockColors.metal]): a metal
 * cell is a flat square, so that metal cells side by side make one plate; the other cells are bubbles of the board,
 * framed by their joint in the metal.
 */
internal fun lockCellStyle(
    variant: BloxThemeVariant,
    cell: LockCell,
): CellStyle {
    val colors = lockColors(variant)
    val empty = cellStyle(variant, Cell.Empty)
    val hole = empty.copy(bubble = CellBubble(highlight = null, gloss = colors.holeInset))
    // The mark of a hint, the same on a hole and on a placed piece: a dot in ink on a halo of the page.
    val mark = CellDot(color = colors.ink, halo = colors.page, size = MARK_SIZE)
    return when (cell) {
        LockCell.Metal -> CellStyle(colors.metal, border = null, centre = CellCentre(colors.rivet, RIVET_SIZE))
        LockCell.Hole -> hole
        LockCell.Hint -> hole.copy(fill = colors.hintFill.compositeOver(hole.fill), dot = mark)
        is LockCell.Placed -> cellStyle(variant, Cell.Filled(cell.type)).copy(dot = mark.takeIf { cell.hinted })
        is LockCell.Preview -> preview(cellStyle(variant, Cell.Filled(cell.type)), empty.fill, colors, cell.fits)
    }
}

/**
 * The preview of a piece [placed] in this style: the same bubble, faded over [hole], the empty cell, flat and outlined
 * in ink, so that it differs from a placed piece by more than its colour; when it does not [fits], a cross in ink on a
 * halo of the page, so that the refusal does not rest on colour.
 */
private fun preview(
    placed: CellStyle,
    hole: Color,
    colors: LockColors,
    fits: Boolean,
): CellStyle {
    val faded = { color: Color -> color.copy(alpha = PREVIEW_ALPHA).compositeOver(hole) }
    return placed.copy(
        fill = faded(placed.fill),
        bubble = placed.bubble?.let { CellBubble(highlight = it.highlight?.let(faded), gloss = Gloss.None) },
        border = CellBorder(colors.ink, OUTLINE_WIDTH),
        cross = if (fits) null else CellCross(color = colors.ink, halo = colors.page, width = CROSS_WIDTH),
    )
}

/** The rivet, a square in the centre of a metal cell: 6 dp of a cell of 42 dp, the largest of the padlock. */
private const val RIVET_SIZE = 1f / 7

/** The colours of a preview over the hole: the piece shows through, apart from the pieces placed. */
private const val PREVIEW_ALPHA = 0.6f

/** The cross of a refused preview: 3 dp of a cell of 42 dp, on a halo twice as wide. */
private const val CROSS_WIDTH = 1f / 14

/** The outline of a preview: 2 dp of a cell of 42 dp. */
private const val OUTLINE_WIDTH = 1f / 21

/** The height of the inner shadow of the hole, at the top of the cell. */
private val HOLE_INSET = 2.dp

/** The padlock by day: `cell-dead` darkened, so that the shape of the hole reads. */
private val LOCK_OF_DAY = BloxPalettes.ColorfulLight.let { palette ->
    LockColors(
        metal = Color(0xFF85889B),
        rivet = Color(0xFF696C81),
        holeInset = Gloss(listOf(GlossLayer.Inner(HOLE_INSET, palette.ink.copy(alpha = 0.18f)))),
        hintFill = palette.accentRing,
        ink = palette.ink,
        page = palette.page,
    )
}

/** The padlock by night: `cell-dead` lightened, so that the shape of the hole reads. */
private val LOCK_OF_NIGHT = BloxPalettes.ColorfulDark.let { palette ->
    LockColors(
        metal = Color(0xFF60657B),
        rivet = palette.panelBorder,
        holeInset = Gloss(listOf(GlossLayer.Inner(HOLE_INSET, Color.Black.copy(alpha = 0.6f)))),
        hintFill = palette.accentRing,
        ink = palette.ink,
        page = palette.page,
    )
}

/** The dot of a hint: 10 dp of a cell of 42 dp, on a halo of 15 dp. */
private const val MARK_SIZE = 1f / 4
