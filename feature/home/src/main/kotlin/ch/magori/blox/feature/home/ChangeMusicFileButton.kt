package ch.magori.blox.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.ButtonSize

/**
 * Opens the picker to change the music file of « Mon fichier », in its row of the sheet Musique, beside its listen
 * button: a compact secondary button, short on screen (« Changer »), so that both leave room to the row; TalkBack says
 * « Changer de fichier ».
 */
@Composable
internal fun ChangeMusicFileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.feature_home_change_file_description)
    BubbleButton(
        family = ButtonFamily.SECONDARY,
        onClick = onClick,
        size = ButtonSize.COMPACT,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Text(text = stringResource(R.string.feature_home_change_file), maxLines = 1, softWrap = false)
    }
}
