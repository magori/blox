package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * « MES MUSIQUES » (V1, P4.21): in the header of the music section, a link to the list of the musics of the player, in
 * capitals and underlined, on one line, after a list, decorative. TalkBack reads it as written, not letter by letter.
 */
@Composable
internal fun MyMusicsLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.feature_home_my_musics)
    val ink = BloxTheme.palette.ink
    TextButton(
        onClick = onClick,
        shape = BloxTheme.shapes.button,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = label },
    ) {
        BloxIcon(BloxIcon.LIST, Modifier.testTag(HOME_ICON_TAG), size = IconSizes.besideSmallLabel, tint = ink)
        Spacer(Modifier.width(IconSizes.gap))
        SingleLineText(
            text = label.uppercase(),
            color = ink,
            style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
        )
    }
}
