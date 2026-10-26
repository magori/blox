package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A row of the settings page that leads to an [action] (Record du Classique and Effacer), laid out as a
 * [SettingSwitchRow]: its [icon] in a slot of 40 dp, its [label], then the [action] at the end of the line; when both
 * do not hold on one line (a large font), the action goes under them. TalkBack reads the label, then the action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingActionRow(
    icon: BloxIcon,
    label: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    val palette = BloxTheme.palette
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BloxSizes.settingRow)
            .padding(BloxSpaces.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(BloxSizes.settingIcon), contentAlignment = Alignment.Center) {
                BloxIcon(icon, size = IconSizes.alone, tint = palette.label)
            }
            // No autoSize: it would take the whole line, and push the action under the label in any font.
            Text(
                text = label,
                color = palette.ink,
                style = settingLabelStyle(),
                modifier = Modifier.padding(start = BloxSpaces.space3),
            )
        }
        action()
    }
}
