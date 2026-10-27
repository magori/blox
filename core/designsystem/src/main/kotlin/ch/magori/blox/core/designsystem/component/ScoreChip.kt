package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A score in the bar of a home (puce Record, puce « MEILLEUR DU JOUR »): a star, decorative, then the [label] in
 * capitals above the [value] as shown (capped, or « — »), shrunk rather than cut in a large font; a pill of 48 dp high
 * at least, as the discs beside it. TalkBack reads [description] alone, the real value, not the text shown.
 */
@Composable
fun ScoreChip(
    label: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .heightIn(min = BloxSizes.minTouchTarget)
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(horizontal = BloxSpaces.space4),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BloxIcon(BloxIcon.STAR, tint = palette.label)
        Column {
            SingleLineText(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
                color = palette.label,
            )
            SingleLineText(
                text = value,
                style = MaterialTheme.typography.titleMedium.merge(BloxTheme.typography.record),
                color = palette.ink,
            )
        }
    }
}
