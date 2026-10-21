package ch.magori.blox.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the icons of the home screen beside a label, decorative, read by the tests only. */
internal const val HOME_ICON_TAG = "home_icon"

/**
 * What the listen button of a music shows, [label] after its [icon], and what TalkBack reads, [description]: the
 * button alone says « Écouter », TalkBack says which music.
 */
internal data class ListenTexts(
    @param:StringRes val label: Int,
    @param:StringRes val description: Int,
    val icon: BloxIcon,
)

/** The texts of the listen button of a music, [previewed] or not: listen to it, or stop it. */
internal fun listenTexts(previewed: Boolean): ListenTexts = if (previewed) {
    ListenTexts(
        label = R.string.feature_home_stop,
        description = R.string.feature_home_stop_description,
        icon = BloxIcon.STOP,
    )
} else {
    ListenTexts(
        label = R.string.feature_home_listen,
        description = R.string.feature_home_listen_description,
        icon = BloxIcon.PLAY,
    )
}

/**
 * Listens to the music named [musicName] before playing, or stops it if [previewed]. The icon before the label is
 * decorative: TalkBack reads « Écouter Menuet », nothing more.
 */
@Composable
internal fun ListenButton(
    musicName: String,
    previewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = listenTexts(previewed)
    val description = stringResource(texts.description, musicName)
    val ink = BloxTheme.palette.label
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = description },
    ) {
        BloxIcon(texts.icon, Modifier.testTag(HOME_ICON_TAG), size = IconSizes.besideSmallLabel, tint = ink)
        Spacer(Modifier.width(IconSizes.gap))
        Text(text = stringResource(texts.label), color = ink, style = MaterialTheme.typography.labelMedium)
    }
}
