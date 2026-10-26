package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A row of the settings page that leads to an [action] (Record du Classique and Effacer), laid out as a
 * [SettingSwitchRow]: its [icon] in a slot of 40 dp, its [label], which goes to the line between its words, then the
 * [action] at the end of the line. The action goes under the label, on the right, only when the longest word of the
 * label no longer holds beside it (a large font). TalkBack leaves the label out and reads the action alone, which must
 * therefore name itself whole (« Effacer le record du Classique »): the row is read once.
 */
@Composable
fun SettingActionRow(
    icon: BloxIcon,
    label: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    val palette = BloxTheme.palette
    SettingRowLayout(
        modifier = modifier.settingRow().padding(BloxSpaces.space2),
        labels = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(icon, tint = palette.label)
                Text(
                    text = label,
                    color = palette.ink,
                    style = settingLabelStyle(),
                    modifier = Modifier
                        .padding(start = BloxSpaces.space3)
                        .weight(1f)
                        .semantics { hideFromAccessibility() },
                )
            }
        },
        track = action,
    )
}
