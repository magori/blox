package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords

/**
 * A volume of the settings page (docs/design-system/README.md, Paramètres), laid out as a [SettingSwitchRow]: its
 * [icon] in a slot of 40 dp, its [label], then under it a [VolumeSlider] at [percent], which hands the volume asked
 * for to [onPercentChange]. TalkBack reads the slider once, named by [label], which it does not read again. [enabled]
 * false (the volume of the effects while they are off), the label and the slider are greyed and the slider cannot be
 * moved. In a large font, the label shrinks only so as not to cut a word, as the label of a switch.
 *
 * @throws IllegalArgumentException if [percent] is not a multiple of 10 from 0 to 100.
 */
@Composable
fun SettingVolumeRow(
    icon: BloxIcon,
    label: String,
    percent: Int,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = BloxTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BloxSizes.settingRow)
            .padding(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(BloxSizes.settingIcon), contentAlignment = Alignment.Center) {
            BloxIcon(icon, size = IconSizes.alone, tint = palette.label)
        }
        Column(
            modifier = Modifier.padding(start = BloxSpaces.space3).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val style = settingLabelStyle()
            Text(
                text = label,
                color = if (enabled) palette.ink else palette.label,
                style = style,
                maxLines = 2,
                autoSize = shrinkToWholeWords(style.fontSize),
                // TalkBack reads it as the name of the slider.
                modifier = Modifier.semantics { hideFromAccessibility() },
            )
            VolumeSlider(percent = percent, onPercentChange = onPercentChange, description = label, enabled = enabled)
        }
    }
}
