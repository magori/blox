package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * Opens the picker to change the music file of « Mon fichier », beside its listen button: short on screen
 * (« Changer »), so that both fit under the choice; TalkBack says « Changer de fichier ».
 */
@Composable
internal fun ChangeMusicFileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.feature_home_change_file_description)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(R.string.feature_home_change_file),
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
