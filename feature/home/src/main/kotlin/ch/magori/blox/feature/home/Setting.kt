package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToFit

/**
 * A setting, on or off as [checked]: a row as wide as the page, drawn as a panel, its [text] on the left and its
 * switch on the right, the whole row a switch for TalkBack (« Vibrations, interrupteur, désactivé ») and for the
 * finger. The thumb of the switch stands on the left when off, on the right when on: its state is not told by colour
 * only. In a large font, the label keeps its size and the row grows: it goes on two lines at a space, and the switch
 * goes under it, still on the right, when the label no longer fits beside it (« Vibrations » in Original, twice as
 * large, on 360 dp). It shrinks only as a last resort, so that no word is ever cut in two.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Setting(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = settingColors(BloxTheme.palette)
    val shape = BloxTheme.shapes.md
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BloxSizes.minTouchTarget)
            .panelFrame(colors.row, shape)
            // The ripple of a tap stays within the frame, rounded in Couleurs.
            .clip(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = SETTING_PADDING, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(SETTING_PADDING),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val style = MaterialTheme.typography.labelLarge
        // The label as wide as its words; the switch takes the room left on the line, on its right, or the next line.
        Text(
            text = text,
            color = colors.row.ink,
            style = style,
            maxLines = 2,
            autoSize = shrinkToFit(style.fontSize),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            // Drawn only: the row holds the action and the state, so TalkBack reads one switch, its label included.
            Switch(checked = checked, onCheckedChange = null, colors = colors.switch)
        }
    }
}

/** The background and the frame of a choice or of a setting, in [colors], of [shape]. */
internal fun Modifier.panelFrame(
    colors: ChoiceColors,
    shape: Shape,
): Modifier = background(colors.background, shape).border(BloxBorders.frame, colors.frame, shape)

/** The room inside a setting, on both sides, and between its label and its switch. */
private val SETTING_PADDING = 12.dp
