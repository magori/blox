package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Items side by side, as wide and as high as each other, [gap] apart, while the label of each, [labelWidths] (in
 * pixels, the room it needs on a line, measured by the caller), holds in its share less [padding] on each side
 * ([holdSideBySide]); one above the other otherwise, each as wide as the line, [stackedGap] apart, rather than cut a word
 * or shrink a font made larger. [item] draws the item of [index] with the modifier that sizes it, [sizing].
 */
@Composable
fun SideBySideOrStacked(
    labelWidths: List<Int>,
    padding: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
    stackedGap: Dp = gap,
    item: @Composable (index: Int, sizing: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sideBySide = with(density) {
            holdSideBySide(
                width = constraints.maxWidth,
                gap = gap.roundToPx(),
                padding = padding.roundToPx(),
                weights = labelWidths.map { 1f },
                labels = labelWidths,
            )
        }
        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                labelWidths.indices.forEach { item(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(stackedGap)) {
                labelWidths.indices.forEach { item(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}
