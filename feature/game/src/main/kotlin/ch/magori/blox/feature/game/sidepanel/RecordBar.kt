package ch.magori.blox.feature.game.sidepanel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.ui.PanelSizes

/** The sizes of the record bar, in one place. */
private object RecordBarSizes {
    /** Its height, as `docs/design-system/components/StatPanel` says. */
    val height = 4.dp

    /** Between its bottom and the frame of the panel: it lies in the padding under the score, which keeps its size. */
    val bottom = 2.dp
}

/**
 * How much of the record bar under the score is filled, from 0 to 1: [score] against [record], full once the score
 * reaches it. Null without a record above 0 (0, or not read yet): the bar is then hidden.
 */
internal fun recordBarFill(
    score: Points,
    record: Int?,
): Float? = if (record == null || record <= 0) null else minOf(score.value.toFloat() / record, 1f)

/**
 * The record bar drawn at the bottom of a panel, in its padding, as wide as its content: a track of `panel-border`,
 * filled in `accent` from the start of the line up to [fill], of 0 to 1, in pills (square in Original). Drawn over the
 * panel, it changes neither its size nor the place of its value.
 */
@Composable
internal fun Modifier.recordBar(fill: Float): Modifier {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    return drawWithCache {
        val inset = (BloxBorders.frame + PanelSizes.padding).toPx()
        val height = RecordBarSizes.height.toPx()
        val top = size.height - (BloxBorders.frame + RecordBarSizes.bottom).toPx() - height
        val track = Size((size.width - 2 * inset).coerceAtLeast(0f), height)
        val filled = Size(track.width * fill, height)
        val trackOutline = shape.createOutline(track, layoutDirection, this)
        val filledOutline = shape.createOutline(filled, layoutDirection, this)
        val filledLeft = if (layoutDirection == LayoutDirection.Rtl) inset + track.width - filled.width else inset
        onDrawWithContent {
            drawContent()
            translate(inset, top) { drawOutline(trackOutline, palette.panelBorder) }
            if (filled.width > 0f) translate(filledLeft, top) { drawOutline(filledOutline, palette.accent) }
        }
    }
}
