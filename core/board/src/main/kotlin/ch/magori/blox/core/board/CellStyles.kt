package ch.magori.blox.core.board

import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/**
 * The style of [cell] in [variant], from the tables of docs/themes.md. Widths are given there for a
 * 24 px cell. The design system knows the board background, not the pieces (ADR 0005). The styles
 * are built once: reading one allocates nothing, so it can be done for every cell of every frame.
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
        is Cell.Filled -> COLORFUL_PIECES.getValue(cell.type)
    }

    BloxThemeVariant.COLORFUL_DARK -> when (cell) {
        Cell.Empty -> COLORFUL_DARK_EMPTY
        is Cell.Filled -> COLORFUL_PIECES.getValue(cell.type)
    }
}

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
        border = CellBorder.Plain(border, ORIGINAL_BORDER_WIDTH),
        centre = CellCentre(centre, ORIGINAL_CENTRE_SIZE),
        joint = CellJoint(ORIGINAL_JOINT_WIDTH),
    )
}

/** The margin around an empty cell: a joint, in the colour under the cell, as around a piece. */
private const val COLORFUL_EMPTY_JOINT_WIDTH = 1f / 24
private const val COLORFUL_BEVEL_WIDTH = 3f / 24

private fun colorfulStyle(type: PieceType): CellStyle {
    val (fill, light, dark) = when (type) {
        PieceType.I -> Triple(0xFF3FC5E8, 0xFF9BE6F6, 0xFF1D8FAE)
        PieceType.O -> Triple(0xFFF2C641, 0xFFFAE08F, 0xFFB38A17)
        PieceType.T -> Triple(0xFFA467E0, 0xFFCFA6F1, 0xFF6E36A8)
        PieceType.S -> Triple(0xFF5BC46A, 0xFFA3E3AB, 0xFF2F8A3C)
        PieceType.Z -> Triple(0xFFE5534B, 0xFFF29A94, 0xFFA32A24)
        PieceType.J -> Triple(0xFF3F7EE8, 0xFF93B8F4, 0xFF1F4FA6)
        PieceType.L -> Triple(0xFFEF8D3B, 0xFFF6C18F, 0xFFAD5A14)
    }
    return CellStyle(Color(fill), CellBorder.Bevel(Color(light), Color(dark), COLORFUL_BEVEL_WIDTH), centre = null)
}

/** An empty cell of a Colorful variant: its own colour, apart from the background by a joint of it. */
private fun colorfulEmpty(palette: BloxPalette): CellStyle =
    CellStyle(palette.emptyCell, border = null, centre = null, joint = CellJoint(COLORFUL_EMPTY_JOINT_WIDTH))

// The tables come last: top-level properties are initialised in the order of the file.
private val ORIGINAL_EMPTY = CellStyle(BloxPalettes.Original.boardBackground, border = null, centre = null)
private val ORIGINAL_PIECES: Map<PieceType, CellStyle> = PieceType.entries.associateWith(::originalStyle)
private val COLORFUL_LIGHT_EMPTY = colorfulEmpty(BloxPalettes.ColorfulLight)
private val COLORFUL_DARK_EMPTY = colorfulEmpty(BloxPalettes.ColorfulDark)
private val COLORFUL_PIECES: Map<PieceType, CellStyle> = PieceType.entries.associateWith(::colorfulStyle)
