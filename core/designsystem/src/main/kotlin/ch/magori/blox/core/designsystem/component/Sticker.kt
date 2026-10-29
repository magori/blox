package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.takeOrElse
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/** A sticker leans, as stuck by hand: NOUVEAU RECORD, NOUVELLE, PARFAITE, « +points ». Not in Original. */
const val STICKER_TILT: Float = -4f

/** Around the word of a sticker: NOUVELLE on the cartridges, NOUVEAU MEILLEUR or PARFAITE on the Serrurier's card. */
private val StickerPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp)

/** The colours of a sticker and its lean: its [frame], if any, the colour around it. */
internal data class StickerLook(val fill: Color, val ink: Color, val frame: Color?, val tilt: Float)

/**
 * A sticker in [palette]: `accent-ink` on `accent`, [framed] in the colour of the page, which cuts it out of what it
 * stands on; leaning by [tilt] degrees, except in Original ([kind]), whose pixels would blur.
 */
internal fun stickerLook(
    palette: BloxPalette,
    kind: BloxThemeKind,
    tilt: Float,
    framed: Boolean,
): StickerLook = StickerLook(
    fill = palette.accent,
    ink = palette.accentInk,
    frame = if (framed) palette.page else null,
    tilt = if (kind == BloxThemeKind.ORIGINAL) 0f else tilt,
)

/**
 * A sticker (NOUVEAU RECORD in the game, NOUVELLE on a cartridge, NOUVEAU MEILLEUR or PARFAITE on the Serrurier's
 * card): [text] in capitals, as [stickerLook] colours and leans it, in a pill, in [style] (`points-pop`, the font of
 * the stickers, unless told otherwise). The size of [style] is the size it starts from and shrinks from, rather than
 * grow past its place: on one line, or, with [maxLines] over one, on as few lines as it can, aligned by [textAlign].
 * [padding] is the room around its text. What TalkBack reads of it, and how it moves, belong to the screen, in
 * [modifier], which comes before its lean.
 */
@Composable
fun Sticker(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = BloxTheme.typography.pointsPop,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Unspecified,
    tilt: Float = STICKER_TILT,
    framed: Boolean = true,
    padding: PaddingValues = StickerPadding,
) {
    val look = stickerLook(BloxTheme.palette, BloxTheme.kind, tilt = tilt, framed = framed)
    val shape = BloxTheme.shapes.pill
    // Original gives `points-pop` no size of its own: the sticker then starts from the size of the text around it,
    // and, if that has none either, keeps the size the text is given, without shrinking.
    val largest = style.fontSize.takeOrElse { LocalTextStyle.current.fontSize }
    val autoSize = when {
        largest.isUnspecified -> null
        maxLines == 1 -> shrinkToFit(largest)
        else -> shrinkToFewestLines(largest)
    }
    Text(
        text = text.uppercase(),
        color = look.ink,
        style = style,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        textAlign = textAlign,
        autoSize = autoSize,
        modifier = modifier
            .graphicsLayer { rotationZ = look.tilt }
            .background(look.fill, shape)
            .then(if (look.frame == null) Modifier else Modifier.border(BloxBorders.frame, look.frame, shape))
            .padding(padding),
    )
}
