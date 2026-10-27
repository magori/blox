package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A row of settings (the home, docs/design-system/README.md): [control] on the line of its [label], at its end, when
 * both fit; else the label goes above it, and the control stands under it, centred, as wide as it asks up to the
 * whole line (a group of choices takes it all), as in a large font. [action], if any,
 * stands beside the label. TalkBack reads the label as a heading, then the control.
 */
@Composable
fun LabelledControl(
    label: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    control: @Composable () -> Unit,
) {
    Layout(
        contents = listOf({ ControlLabel(label, action) }, control),
        modifier = modifier,
    ) { (labels, controls), constraints ->
        val labelPart = labels.single()
        val controlPart = controls.single()
        val gap = BloxSpaces.space3.roundToPx()
        val labelWidth = labelPart.maxIntrinsicWidth(Constraints.Infinity)
        val controlWidth = controlPart.maxIntrinsicWidth(Constraints.Infinity)
        val wanted = labelWidth + gap + controlWidth
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else wanted
        if (wanted <= width) {
            // Beside: the label at the start of the line, the control at its end, both centred on the line.
            val placedControl = controlPart.measure(Constraints.fixedWidth(controlWidth))
            val placedLabel = labelPart.measure(Constraints(maxWidth = width - controlWidth - gap))
            val height = maxOf(placedControl.height, placedLabel.height)
            layout(width, height) {
                placedLabel.placeRelative(0, (height - placedLabel.height) / 2)
                placedControl.placeRelative(width - placedControl.width, (height - placedControl.height) / 2)
            }
        } else {
            // Above: the label on its own line, the control under it, centred, as wide as it wants up to the line.
            val placedLabel = labelPart.measure(Constraints(maxWidth = width))
            val placedControl = controlPart.measure(Constraints(maxWidth = width))
            val spacing = BloxSpaces.space1.roundToPx()
            layout(width, placedLabel.height + spacing + placedControl.height) {
                placedLabel.placeRelative(0, 0)
                placedControl.placeRelative((width - placedControl.width) / 2, placedLabel.height + spacing)
            }
        }
    }
}

/** The label of a row, a heading, and its [action] beside it, or under it when short of room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlLabel(
    label: String,
    action: (@Composable () -> Unit)?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.section),
            modifier = Modifier.semantics { heading() },
        )
        action?.invoke()
    }
}
