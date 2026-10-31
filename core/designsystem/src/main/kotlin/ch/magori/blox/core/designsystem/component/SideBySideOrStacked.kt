package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The items of [content] side by side, as wide and as high as each other, [gap] apart, while the label of each holds in
 * its share less [padding] on each side ([holdSideBySide]); one above the other otherwise, each as wide as the line,
 * [stackedGap] apart, rather than cut a word or shrink a font made larger. The label of an item is the room it needs on
 * a line, in pixels: [labelWidths], measured by the caller, or else the width the item asks (its `maxIntrinsicWidth`).
 * Given only the width it asks, as beside another part, it is as wide as the widest item, and so stacks them. The tiles
 * of the home, the foot of Mes musiques and *Copier* / *Effacer* of the editor are laid out so.
 */
@Composable
fun SideBySideOrStacked(
    gap: Dp,
    modifier: Modifier = Modifier,
    stackedGap: Dp = gap,
    labelWidths: List<Int>? = null,
    padding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val policy = remember(density, labelWidths, padding, gap, stackedGap) {
        with(density) {
            SideBySideOrStackedPolicy(labelWidths, padding.roundToPx(), gap.roundToPx(), stackedGap.roundToPx())
        }
    }
    Layout(content = content, modifier = modifier.fillMaxWidth(), measurePolicy = policy)
}

/**
 * How [SideBySideOrStacked] lays its items out, all in pixels: side by side in the shares of a row ([rowShares]), as
 * high as the highest asks, while each of [labels] (or the width each item asks, when null) holds in its share less
 * [padding] on each side, [gap] apart; else one above the other, [stackedGap] apart, each as wide as the line, or as
 * the widest item asks when the line is unbounded.
 */
private data class SideBySideOrStackedPolicy(
    private val labels: List<Int>?,
    private val padding: Int,
    private val gap: Int,
    private val stackedGap: Int,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        }
        val weights = measurables.map { 1f }
        val asked = labels ?: measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        return if (holdSideBySide(width, gap, padding, weights, asked)) {
            val shares = rowShares(width, gap, weights)
            val height = measurables.zip(shares).maxOf { (item, share) -> item.minIntrinsicHeight(share) }
            val placed = measurables.zip(shares).map { (item, share) -> item.measure(Constraints.fixed(share, height)) }
            layout(width, height) {
                var x = 0
                placed.forEach {
                    it.placeRelative(x, 0)
                    x += it.width + gap
                }
            }
        } else {
            val placed = measurables.map { it.measure(Constraints.fixedWidth(width)) }
            layout(width, placed.sumOf { it.height } + stackedGap * (placed.size - 1)) {
                var y = 0
                placed.forEach {
                    it.placeRelative(0, y)
                    y += it.height + stackedGap
                }
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOf { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.maxOf { it.minIntrinsicWidth(height) }
}
