package ch.magori.blox.feature.game.sidepanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.feature.game.isBest
import ch.magori.blox.feature.game.ui.PanelSizes

/** The sizes of the best bar, in one place. */
private object BestBarSizes {
    /** Its height, as `docs/design-system/components/StatPanel` says. */
    val height = 4.dp

    /** Between its bottom and the frame of the panel: it lies in the padding under the score, which keeps its size. */
    val bottom = 2.dp
}

/**
 * How much of the best bar under the score is filled, from 0 to 1: [score] against [best] (the record of the Classic,
 * or the best of the day), full once the score reaches it. Null without a best above 0 (0, or not read yet): the bar
 * is then hidden.
 */
internal fun bestBarFill(
    score: Points,
    best: Int?,
): Float? = if (best.isBest()) minOf(score.value.toFloat() / best, 1f) else null

/**
 * The best bar drawn at the bottom of a panel, in its padding, as wide as its content: a track, filled from the start
 * of the line up to [fill], of 0 to 1, in the colours of [bestBarColors], in pills (square in Original). Drawn over
 * the panel, it changes neither its size nor the place of its value.
 */
@Composable
internal fun Modifier.bestBar(fill: Float): Modifier {
    val colors = bestBarColors(BloxTheme.palette)
    val shape = BloxTheme.shapes.pill
    return drawWithCache {
        val inset = (BloxBorders.frame + PanelSizes.padding).toPx()
        val height = BestBarSizes.height.toPx()
        val top = size.height - (BloxBorders.frame + BestBarSizes.bottom).toPx() - height
        val track = Size((size.width - 2 * inset).coerceAtLeast(0f), height)
        val filled = Size(track.width * fill, height)
        val trackOutline = shape.createOutline(track, layoutDirection, this)
        val filledOutline = shape.createOutline(filled, layoutDirection, this)
        val filledLeft = if (layoutDirection == LayoutDirection.Rtl) inset + track.width - filled.width else inset
        onDrawWithContent {
            drawContent()
            translate(inset, top) { drawOutline(trackOutline, colors.track) }
            if (filled.width > 0f) translate(filledLeft, top) { drawOutline(filledOutline, colors.fill) }
        }
    }
}

/** The colours of the best bar: its [track], and its [fill] up to the score. */
@Immutable
internal data class BestBarColors(val track: Color, val fill: Color)

/**
 * The colours of the best bar in [palette]: a track of `panel-inset`, filled in `accent-pressed`, which stands out
 * from it by 3:1 at least in every theme (WCAG 2.1, 1.4.11). The design system gives `panel-border` and `accent`: the
 * same green in Original, 1.6:1 in Couleurs by day, the progress would not show.
 */
internal fun bestBarColors(palette: BloxPalette): BestBarColors =
    BestBarColors(track = palette.panelInset, fill = palette.accentPressed)
