package ch.magori.blox.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.icon.IconSizes
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToFewestLines

/**
 * « COMPOSER UNE MUSIQUE » (V1, P4.21): under the choice of music, framed in ink, a note, decorative, before its label
 * in capitals, on one line, or on two in a large font. TalkBack reads it as written, not letter by letter.
 */
@Composable
internal fun ComposeMusicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = BloxTheme.palette.ink
    val label = stringResource(R.string.feature_home_compose)
    OutlinedButton(
        onClick = onClick,
        shape = BloxTheme.shapes.md,
        border = BorderStroke(BloxBorders.frame, ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .semantics { contentDescription = label },
    ) {
        BloxIcon(BloxIcon.NOTE, Modifier.testTag(HOME_ICON_TAG), tint = ink)
        // On one line, shrinking if needed, or on two in a large font, never cut: at 200 %, in the pixel font, one line
        // would hold the 20 letters only below the size of the text with the normal font; on two, « COMPOSER UNE » and
        // « MUSIQUE » stay larger. Two lines hold no word cut in the middle: « COMPOSER » alone would leave three.
        val style = MaterialTheme.typography.labelLarge
        Text(
            text = label.uppercase(),
            style = style,
            textAlign = TextAlign.Center,
            maxLines = 2,
            autoSize = shrinkToFewestLines(style.fontSize),
            modifier = Modifier.padding(start = IconSizes.gap),
        )
    }
}
