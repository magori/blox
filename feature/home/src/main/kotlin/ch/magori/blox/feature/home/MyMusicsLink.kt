package ch.magori.blox.feature.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * « MES MUSIQUES » (V1, P4.21): in the header of the music section, a link to the list of the musics of the player, in
 * capitals and underlined, on one line. TalkBack reads it as written, not letter by letter.
 */
@Composable
internal fun MyMusicsLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.feature_home_my_musics)
    TextButton(
        onClick = onClick,
        shape = BloxTheme.shapes.button,
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = label },
    ) {
        SingleLineText(
            text = label.uppercase(),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
        )
    }
}
