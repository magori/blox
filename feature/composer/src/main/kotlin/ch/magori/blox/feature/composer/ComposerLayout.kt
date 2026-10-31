package ch.magori.blox.feature.composer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import ch.magori.blox.feature.composer.ui.ComposerSizes

/**
 * The parts of the editor from top to bottom, [ComposerSizes.blockGap] apart, within [ComposerSizes.padding]: those
 * [above] the grid, each as high as it asks, the [grid], given the room they leave, and those [below] it. The grid keeps
 * [ComposerSizes.gridMin] at least: on a small phone with a large font, where the parts leave it less, the whole page
 * scrolls rather than squash a part or hide the grid.
 */
@Composable
internal fun GridBetween(
    above: @Composable () -> Unit,
    grid: @Composable () -> Unit,
    below: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val page = constraints.maxHeight
        Layout(
            contents = listOf(above, grid, below),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(ComposerSizes.padding),
        ) { (aboveParts, gridPart, belowParts), constraints ->
            val width = constraints.maxWidth
            val gap = ComposerSizes.blockGap.roundToPx()
            val padding = ComposerSizes.padding.roundToPx() * 2
            val asked = Constraints(maxWidth = width)
            val tops = aboveParts.map { it.measure(asked) }
            val bottoms = belowParts.map { it.measure(asked) }
            val parts = tops + bottoms
            val taken = parts.sumOf { it.height } + gap * parts.size
            val inside = if (page == Constraints.Infinity) null else page - padding
            val grid = gridHeight(inside, taken, ComposerSizes.gridMin.roundToPx())
            val shown = gridPart.single().measure(Constraints.fixed(width, grid))
            layout(width, taken + grid) {
                var y = 0
                (tops + shown + bottoms).forEach { part: Placeable ->
                    part.placeRelative(0, y)
                    y += part.height + gap
                }
            }
        }
    }
}
