package ch.magori.blox.core.board.mascot

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ch.magori.blox.core.board.SOFT_SHADOW
import ch.magori.blox.core.board.cellStyle
import ch.magori.blox.core.board.pieceColor
import ch.magori.blox.core.designsystem.theme.BloxPalettes
import ch.magori.blox.core.designsystem.theme.BloxThemeVariant
import ch.magori.blox.core.game.model.Cell
import ch.magori.blox.core.game.model.PieceType

/**
 * How a mascot is drawn (docs/design-system/pictogrammes.md, Mascotte): its cells lit from the top, from [highlight]
 * to [fill], their corners [corner] of a cell; its eyes, mouth and « z » in [face]; its accessory filled in [accessory],
 * none when asleep; its [shadow], none when asleep. The fractions are of a cell.
 */
@Immutable
internal data class MascotLook(
    val fill: Color,
    val highlight: Color,
    val corner: Float,
    val accessory: Color?,
    val face: Color,
    val shadow: MascotShadow?,
)

/** A shadow of [color] under the body, [drop] lower and blurred over [blur], fractions of a cell. */
internal data class MascotShadow(val color: Color, val drop: Float, val blur: Float)

/**
 * The look of [character] in Colorful, by day or at [night], awake or [asleep]: the colours of its piece (`piece-*`,
 * read from the styles of the cells, written once), its accessory in those of the O, or of the S for the sprout;
 * asleep, in `cell-dead`.
 */
internal fun mascotLook(
    character: MascotCharacter,
    night: Boolean,
    asleep: Boolean,
): MascotLook {
    val variant = if (night) BloxThemeVariant.COLORFUL_DARK else BloxThemeVariant.COLORFUL_LIGHT
    val corner = if (night) NIGHT_CORNER else DAY_CORNER
    if (asleep) {
        val palette = if (night) BloxPalettes.ColorfulDark else BloxPalettes.ColorfulLight
        return MascotLook(
            fill = palette.cellDead,
            highlight = palette.cellDead,
            corner = corner,
            accessory = null,
            face = palette.ink,
            shadow = null,
        )
    }
    val body = cellStyle(variant, Cell.Filled(character.piece))
    val shadow = if (night) {
        MascotShadow(color = body.fill.copy(alpha = NIGHT_HALO_ALPHA), drop = 0f, blur = NIGHT_HALO_BLUR)
    } else {
        MascotShadow(color = SOFT_SHADOW, drop = DAY_SHADOW_DROP, blur = DAY_SHADOW_BLUR)
    }
    return MascotLook(
        fill = body.fill,
        highlight = body.bubble?.highlight ?: body.fill,
        corner = corner,
        accessory = pieceColor(variant, accessoryPiece(character)),
        // The pastels and the neons are light: the face keeps the dark ink of the day on them, by night too.
        face = BloxPalettes.ColorfulLight.ink,
        shadow = shadow,
    )
}

/**
 * The piece whose colour fills the accessory: the yellow of the O for the crown and the key, the mint of the S for the
 * sprout.
 */
private fun accessoryPiece(character: MascotCharacter): PieceType = when (character) {
    MascotCharacter.CLASSIC, MascotCharacter.LOCKSMITH -> PieceType.O
    MascotCharacter.DAILY -> PieceType.S
}

private const val DAY_CORNER = 0.3f
private const val NIGHT_CORNER = 0.26f

private const val DAY_SHADOW_DROP = 0.12f
private const val DAY_SHADOW_BLUR = 0.16f
private const val NIGHT_HALO_ALPHA = 0.6f
private const val NIGHT_HALO_BLUR = 0.25f
