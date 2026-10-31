package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Two parts on one line, [gap] apart, the first at the start, the second at the end: the fixed one, first when
 * [fixedFirst], as wide as it asks, and up to the rest of the line to the other, while that rest is [restMin] wide at
 * least and holds what it shows at its size, its longest word whole ([fitsBesideFixed]); otherwise one above the other,
 * in their order, [stackedGap] apart, each up to the width of the line, at its end, rather than cut a word of a font
 * made larger. A part that fills its room says so itself (`fillMaxWidth`). Side by side, each stands in the height of
 * the line as [verticalAlignment] says, and the whole in the least height it is given. The rows of the settings (the
 * track or the action beside the labels) and the commands of the editor (*Annuler* beside the tools, *Copier* and
 * *Effacer* beside the bars, *Écouter* beside the tempo) are laid out so.
 */
@Composable
fun FixedBesideRest(
    fixedFirst: Boolean,
    gap: Dp,
    modifier: Modifier = Modifier,
    stackedGap: Dp = gap,
    restMin: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        require(measurables.size == 2) { "Two parts stand beside each other, not ${measurables.size}." }
        val (first, second) = measurables
        val fixed = if (fixedFirst) first else second
        val rest = if (fixedFirst) second else first
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val fixedWidth = fixed.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(width)
        val restWidth = width - fixedWidth - gapPx
        val restNeeds = maxOf(restMin.roundToPx(), rest.minIntrinsicWidth(Constraints.Infinity))
        if (fitsBesideFixed(width, gapPx, fixedWidth, restNeeds)) {
            val fixedPlaced = fixed.measure(Constraints(maxWidth = fixedWidth))
            val restPlaced = rest.measure(Constraints(maxWidth = restWidth))
            val (start, end) = if (fixedFirst) fixedPlaced to restPlaced else restPlaced to fixedPlaced
            val line = maxOf(start.height, end.height)
            val height = maxOf(line, constraints.minHeight)
            layout(width, height) {
                val top = verticalAlignment.align(line, height)
                start.placeRelative(0, top + verticalAlignment.align(start.height, line))
                end.placeRelative(width - end.width, top + verticalAlignment.align(end.height, line))
            }
        } else {
            val stackedGapPx = stackedGap.roundToPx()
            val placed = listOf(first, second).map { it.measure(Constraints(maxWidth = width)) }
            val stack = placed.sumOf { it.height } + stackedGapPx
            val height = maxOf(stack, constraints.minHeight)
            layout(width, height) {
                var y = verticalAlignment.align(stack, height)
                placed.forEach {
                    it.placeRelative(width - it.width, y)
                    y += it.height + stackedGapPx
                }
            }
        }
    }
}

/**
 * Whether a part that [restNeeds] pixels stands beside a fixed one [fixedWidth] wide (as wide as the line at most), on a
 * line [width] wide, [gap] between them.
 */
internal fun fitsBesideFixed(
    width: Int,
    gap: Int,
    fixedWidth: Int,
    restNeeds: Int,
): Boolean = width - fixedWidth.coerceAtMost(width) - gap >= restNeeds
