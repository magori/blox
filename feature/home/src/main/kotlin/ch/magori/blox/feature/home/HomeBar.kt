package ch.magori.blox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The bar of the home: the disc « Changer de jeu » at the start, which goes back to the choice of the game through
 * [onChangeGame], the chip of the [record] in the middle, and the gear at the end, which opens the settings through
 * [onSettings]. Both discs are 48 dp, so that the chip stays in the middle.
 */
@Composable
internal fun HomeBar(
    record: Int,
    onChangeGame: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.CARTRIDGE,
            description = stringResource(R.string.feature_home_change_game),
            onClick = onChangeGame,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            RecordChip(record)
        }
        GlyphButton(
            icon = BloxIcon.GEAR,
            description = stringResource(R.string.feature_home_settings),
            onClick = onSettings,
        )
    }
}

/**
 * The record (puce Record): a star, decorative, then « RECORD » above its value, capped as on the game screen and
 * shrunk rather than cut in a large font; a pill of 48 dp at least. TalkBack reads its real value, nothing more.
 */
@Composable
private fun RecordChip(record: Int) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    val description = stringResource(R.string.feature_home_record_description, record)
    Row(
        modifier = Modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space1)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BloxIcon(BloxIcon.STAR, tint = palette.label)
        Column {
            SingleLineText(
                text = stringResource(R.string.feature_home_record).uppercase(),
                style = MaterialTheme.typography.labelSmall.merge(BloxTheme.typography.statLabel),
                color = palette.label,
            )
            SingleLineText(
                text = scoreText(record),
                style = MaterialTheme.typography.titleMedium.merge(BloxTheme.typography.record),
                color = palette.ink,
            )
        }
    }
}
