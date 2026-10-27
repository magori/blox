package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxGlossSets
import ch.magori.blox.core.designsystem.theme.BloxGlosses
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.designsystem.theme.Gloss
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/**
 * The style of [cell] in [variant]: in Original, from the tables of docs/themes.md, whose widths are given for a
 * 24 px cell; in Colorful, the bubbles of the design system (docs/design-system/components/Piece). The design system
 * knows the board background and the reliefs, not the pieces (ADR 0005, ADR 0023). The styles are built once: reading
 * one allocates nothing, so it can be done for every cell of every frame.
 */
internal fun cellStyle(
    variant: BloxThemeVariant,
    cell: Cell,
): CellStyle = when (variant) {
    BloxThemeVariant.ORIGINAL -> when (cell) {
        Cell.Empty -> ORIGINAL_EMPTY
        is Cell.Filled -> ORIGINAL_PIECES.getValue(cell.type)
    }

    BloxThemeVariant.COLORFUL_LIGHT -> when (cell) {
        Cell.Empty -> COLORFUL_LIGHT_EMPTY
        is Cell.Filled -> COLORFUL_LIGHT_PIECES.getValue(cell.type)
    }

    BloxThemeVariant.COLORFUL_DARK -> when (cell) {
        Cell.Empty -> COLORFUL_DARK_EMPTY
        is Cell.Filled -> COLORFUL_DARK_PIECES.getValue(cell.type)
    }
}

/**
 * The colour of a piece of [type] in [variant], the fill of its cells: for a drawing made of the colours of the pieces
 * outside the board, as the knobs of the console of the choice of the game.
 */
fun pieceColor(
    variant: BloxThemeVariant,
    type: PieceType,
): Color = cellStyle(variant, Cell.Filled(type)).fill

private val ORIGINAL_DARK = Color(0xFF34503A)
private val ORIGINAL_MEDIUM = Color(0xFF87A67F)
private val ORIGINAL_LIGHT = Color(0xFFBFD6B6)
private val ORIGINAL_PALE = Color(0xFFEEF5EA)
private const val ORIGINAL_BORDER_WIDTH = 2f / 24
private const val ORIGINAL_CENTRE_SIZE = 8f / 24

/**
 * A joint around each cell, 1 px of 24 on each edge, 2 between two cells, in the colour under the cell:
 * flat cells side by side never merge.
 */
private const val ORIGINAL_JOINT_WIDTH = 1f / 24

private fun originalStyle(type: PieceType): CellStyle {
    val (fill, border, centre) = when (type) {
        PieceType.I -> Triple(ORIGINAL_DARK, ORIGINAL_DARK, ORIGINAL_DARK)
        PieceType.O -> Triple(ORIGINAL_MEDIUM, ORIGINAL_DARK, ORIGINAL_DARK)
        PieceType.T -> Triple(ORIGINAL_PALE, ORIGINAL_DARK, ORIGINAL_DARK)
        PieceType.S -> Triple(ORIGINAL_MEDIUM, ORIGINAL_MEDIUM, ORIGINAL_MEDIUM)
        PieceType.Z -> Triple(ORIGINAL_DARK, ORIGINAL_DARK, ORIGINAL_LIGHT)
        PieceType.J -> Triple(ORIGINAL_LIGHT, ORIGINAL_DARK, ORIGINAL_MEDIUM)
        PieceType.L -> Triple(ORIGINAL_MEDIUM, ORIGINAL_DARK, ORIGINAL_PALE)
    }
    return CellStyle(
        fill = fill,
        border = CellBorder(border, ORIGINAL_BORDER_WIDTH),
        centre = CellCentre(centre, ORIGINAL_CENTRE_SIZE),
        joint = CellJoint.Fraction(ORIGINAL_JOINT_WIDTH),
    )
}

/** The margin around a Colorful cell, piece or empty, in the colour under it: 1 px of the design system. */
private val COLORFUL_MARGIN = 1.dp

/**
 * The colours of a piece in a Colorful variant, the tokens of docs/design-system/tokens.json (PieceTokensTest): its
 * [fill] (`piece-<x>`), and its [highlight] at the top of the bubble (`piece-<x>-hi`).
 */
private class PieceColors(val fill: Long, val highlight: Long)

/** A piece of a Colorful variant: a bubble in its [colors], in the relief of the cells of [glosses]. */
private fun colorfulStyle(
    colors: PieceColors,
    glosses: BloxGlosses,
): CellStyle = CellStyle(
    fill = Color(colors.fill),
    border = null,
    centre = null,
    joint = CellJoint.Fixed(COLORFUL_MARGIN),
    bubble = CellBubble(highlight = Color(colors.highlight), gloss = glosses.cell),
)

/** An empty cell of a Colorful variant: its own colour, rounded as a piece, flat, apart from the background by a joint. */
private fun colorfulEmpty(palette: BloxPalette): CellStyle = CellStyle(
    fill = palette.emptyCell,
    border = null,
    centre = null,
    joint = CellJoint.Fixed(COLORFUL_MARGIN),
    bubble = CellBubble(highlight = null, gloss = Gloss.None),
)

/** The pieces by day: pastels. */
private val COLORFUL_LIGHT_COLORS = mapOf(
    PieceType.I to PieceColors(fill = 0xFF8FD8FF, highlight = 0xFFE3F6FF),
    PieceType.O to PieceColors(fill = 0xFFFFE680, highlight = 0xFFFFF8D6),
    PieceType.T to PieceColors(fill = 0xFFC3A2FF, highlight = 0xFFEFE6FF),
    PieceType.S to PieceColors(fill = 0xFF9DEDB9, highlight = 0xFFE2FAEA),
    PieceType.Z to PieceColors(fill = 0xFFFF94A6, highlight = 0xFFFFE0E6),
    PieceType.J to PieceColors(fill = 0xFF9DB6FF, highlight = 0xFFE3EAFF),
    PieceType.L to PieceColors(fill = 0xFFFFB896, highlight = 0xFFFFE9DE),
)

/** The pieces by night: neons. */
private val COLORFUL_DARK_COLORS = mapOf(
    PieceType.I to PieceColors(fill = 0xFF4DD8FF, highlight = 0xFFB8F0FF),
    PieceType.O to PieceColors(fill = 0xFFFFE45E, highlight = 0xFFFFF5BF),
    PieceType.T to PieceColors(fill = 0xFFA77BFF, highlight = 0xFFDCCBFF),
    PieceType.S to PieceColors(fill = 0xFF3DFF8F, highlight = 0xFFB8FFD6),
    PieceType.Z to PieceColors(fill = 0xFFFF4D6D, highlight = 0xFFFFB8C5),
    PieceType.J to PieceColors(fill = 0xFF5C8DFF, highlight = 0xFFC2D4FF),
    PieceType.L to PieceColors(fill = 0xFFFF9E3D, highlight = 0xFFFFD7B0),
)

// The tables come last: top-level properties are initialised in the order of the file.
private val ORIGINAL_EMPTY = CellStyle(BloxPalettes.Original.boardBackground, border = null, centre = null)
private val ORIGINAL_PIECES: Map<PieceType, CellStyle> = PieceType.entries.associateWith(::originalStyle)
private val COLORFUL_LIGHT_EMPTY = colorfulEmpty(BloxPalettes.ColorfulLight)
private val COLORFUL_DARK_EMPTY = colorfulEmpty(BloxPalettes.ColorfulDark)
private val COLORFUL_LIGHT_PIECES: Map<PieceType, CellStyle> =
    COLORFUL_LIGHT_COLORS.mapValues { (_, colors) -> colorfulStyle(colors, BloxGlossSets.ColorfulLight) }
private val COLORFUL_DARK_PIECES: Map<PieceType, CellStyle> =
    COLORFUL_DARK_COLORS.mapValues { (_, colors) -> colorfulStyle(colors, BloxGlossSets.ColorfulDark) }
