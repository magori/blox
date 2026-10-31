package ch.magori.blox.feature.game.feedback

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.boardPoint
import ch.magori.blox.core.designsystem.component.STICKER_TILT
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.feature.game.R

/** Around the text of the sticker « +points ». */
private val StickerPaddingX = 8.dp
private val StickerPaddingY = 4.dp

/**
 * Over the board, the sticker « +points » that [sticker] gives on each frame: `accent-ink` on `accent`, in the font
 * of the stickers (`points-pop`), in a pill that leans, centred across the board, on the row it says. It is read only
 * while drawing (ADR 0024): it rises and fades without recomposing anything. TalkBack reads nothing of it: the score
 * says the points. Four lines cleared at once make it the sticker « 4 LIGNES », over the points, in `four-lines-pop`.
 */
@Composable
internal fun PointsSticker(
    sticker: () -> StickerLook?,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val style = BloxTheme.typography.pointsPop
    val fourLinesStyle = BloxTheme.typography.fourLinesPop.copy(textAlign = TextAlign.Center)
    val fourLines = stringResource(R.string.feature_game_four_lines_sticker).uppercase()
    val palette = BloxTheme.palette
    Spacer(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                val padding = Offset(StickerPaddingX.toPx(), StickerPaddingY.toPx())
                onDrawBehind {
                    val look = sticker() ?: return@onDrawBehind
                    // Measured again only when the points change: the measurer keeps its last texts.
                    val text = measurer.measure(
                        if (look.fourLines) "$fourLines\n+${look.points}" else "+${look.points}",
                        if (look.fourLines) fourLinesStyle else style,
                        density = this,
                        layoutDirection = layoutDirection,
                    )
                    val pill = Size(text.size.width + 2 * padding.x, text.size.height + 2 * padding.y)
                    val centre = boardPoint(size.width, size.height, ClassicBoard.columns / 2f, look.row - look.rise)
                    val topLeft = centre - Offset(pill.width / 2, pill.height / 2)
                    rotate(STICKER_TILT, pivot = centre) {
                        drawRoundRect(
                            palette.accent,
                            topLeft = topLeft,
                            size = pill,
                            cornerRadius = CornerRadius(pill.height / 2),
                            alpha = look.alpha,
                        )
                        drawText(text, color = palette.accentInk, topLeft = topLeft + padding, alpha = look.alpha)
                    }
                }
            },
    )
}
