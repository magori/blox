package ch.magori.blox.feature.home

import androidx.annotation.StringRes
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
 * What the listen button of a music shows, [label], and what TalkBack reads, [description]: the button
 * alone says « Écouter », TalkBack says which music.
 */
internal data class ListenTexts(@param:StringRes val label: Int, @param:StringRes val description: Int)

/** The texts of the listen button of a music, [previewed] or not: listen to it, or stop it. */
internal fun listenTexts(previewed: Boolean): ListenTexts = if (previewed) {
    ListenTexts(label = R.string.feature_home_stop, description = R.string.feature_home_stop_description)
} else {
    ListenTexts(label = R.string.feature_home_listen, description = R.string.feature_home_listen_description)
}

/** Listens to the music named [musicName] before playing, or stops it if [previewed]. */
@Composable
internal fun ListenButton(
    musicName: String,
    previewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = listenTexts(previewed)
    val description = stringResource(texts.description, musicName)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(texts.label),
            color = BloxTheme.palette.label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
