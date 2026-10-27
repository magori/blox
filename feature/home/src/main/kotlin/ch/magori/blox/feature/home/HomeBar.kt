package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.ScoreChip
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.text.scoreText
import ch.magori.blox.core.designsystem.theme.BloxSpaces

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
 * The record (puce Record), capped as on the game screen ([ScoreChip]); TalkBack reads its real value, nothing more.
 */
@Composable
private fun RecordChip(record: Int) {
    ScoreChip(
        label = stringResource(R.string.feature_home_record),
        value = scoreText(record),
        description = stringResource(R.string.feature_home_record_description, record),
    )
}
