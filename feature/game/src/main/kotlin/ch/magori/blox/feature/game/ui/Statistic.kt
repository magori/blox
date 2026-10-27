package ch.magori.blox.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * One statistic on a line, on the game over screen and on the card of the pause: its [label] and its [value];
 * TalkBack reads [description].
 */
@Composable
internal fun Statistic(
    label: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = BloxTheme.palette.label,
            fontSize = PanelSizes.label,
            modifier = Modifier.weight(1f, fill = false),
        )
        ValueText(text = value)
    }
}
