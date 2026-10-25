package ch.magori.blox.core.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/**
 * The four cells of the logo of the home screen, a square of 2 × 2 cells: exactly four colours, one per
 * position.
 */
@Immutable
data class LogoColors(val topLeft: Color, val topRight: Color, val bottomLeft: Color, val bottomRight: Color)

/**
 * The logo of [variant]: the fills of four of its pieces, read from their styles so that they are written once.
 * I, O, J and T in Original, its four greens from dark to pale; I, O, Z and S in Colorful.
 */
fun logoColors(variant: BloxThemeVariant): LogoColors = when (variant) {
    BloxThemeVariant.ORIGINAL -> ORIGINAL_LOGO
    BloxThemeVariant.COLORFUL_LIGHT -> COLORFUL_LIGHT_LOGO
    BloxThemeVariant.COLORFUL_DARK -> COLORFUL_DARK_LOGO
}

private fun logoOf(
    variant: BloxThemeVariant,
    vararg pieces: PieceType,
): LogoColors {
    val (topLeft, topRight, bottomLeft, bottomRight) = pieces.map { cellStyle(variant, Cell.Filled(it)).fill }
    return LogoColors(topLeft, topRight, bottomLeft, bottomRight)
}

private val ORIGINAL_LOGO = logoOf(BloxThemeVariant.ORIGINAL, PieceType.I, PieceType.O, PieceType.J, PieceType.T)
private val COLORFUL_LIGHT_LOGO =
    logoOf(BloxThemeVariant.COLORFUL_LIGHT, PieceType.I, PieceType.O, PieceType.Z, PieceType.S)
private val COLORFUL_DARK_LOGO =
    logoOf(BloxThemeVariant.COLORFUL_DARK, PieceType.I, PieceType.O, PieceType.Z, PieceType.S)
