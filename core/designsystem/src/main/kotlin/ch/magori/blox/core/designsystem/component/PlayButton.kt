package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The tag of the triangle of a [PlayButton], decorative, read by the tests only. */
const val PLAY_BUTTON_ICON_TAG = "play_button_icon"

/**
 * Jouer (Principal XL), as wide as its column: its [label] above the [reminder] of the game it opens (« Classique ·
 * Niveau 05 · Korobeïniki »), cut by an ellipsis when too long, and a disc in `accent-ink` bearing the triangle in
 * `accent`, decorative. TalkBack reads [description], the label and the whole reminder in words. Shared by the home of
 * the Classic and the entry of the Graine du jour; a halo, if any, is drawn behind it by the screen.
 */
@Composable
fun PlayButton(
    label: String,
    reminder: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BubbleButton(
        family = ButtonFamily.PRIMARY_XL,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SingleLineText(text = label, style = LocalTextStyle.current)
            Text(
                text = reminder,
                style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small)
                    .copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlayDisc()
    }
}

/** The disc of Jouer, in `accent-ink`, and its triangle in `accent`; square in Original, as every pill. */
@Composable
private fun PlayDisc() {
    val palette = BloxTheme.palette
    Box(
        modifier = Modifier.size(PLAY_DISC).background(palette.accentInk, BloxTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        BloxIcon(BloxIcon.PLAY, Modifier.testTag(PLAY_BUTTON_ICON_TAG), tint = palette.accent)
    }
}

/** The disc of Jouer (docs/design-system/README.md, Boutons). */
private val PLAY_DISC = 52.dp
