package ch.magori.blox.feature.game.feedback

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import ch.magori.blox.core.board.boardPoint
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * Over the board, its [DANGER_ROWS] top rows, covered by `danger-soft` as much as [tint] says, read once per frame and
 * only while drawing (ADR 0024), so that they breathe without recomposing anything. TalkBack reads nothing of it: the
 * board says the height of the stack.
 */
@Composable
internal fun DangerRows(
    tint: () -> Float,
    modifier: Modifier = Modifier,
) {
    val color = BloxTheme.palette.dangerSoft
    Spacer(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                // The cells laid out as the board lays them out, once for the size.
                val origin = boardPoint(size.width, size.height, column = 0f, row = 0f)
                val corner =
                    boardPoint(size.width, size.height, ClassicBoard.columns.toFloat(), DANGER_ROWS.toFloat()) - origin
                val rows = Size(corner.x, corner.y)
                onDrawBehind {
                    val alpha = tint()
                    if (alpha > 0f) drawRect(color, origin, rows, alpha = alpha)
                }
            },
    )
}
