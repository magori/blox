package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A button of the list, its [text] on one line, shrunk if needed (never cut in the middle of a word), in [ink] on
 * [background], framed in [frame]. Disabled, it is greyed out and announced as such.
 */
@Composable
internal fun TextButtonBox(
    text: String,
    background: Color,
    ink: Color,
    frame: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = BloxTheme.shapes.button
    Box(
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .widthIn(min = BloxSizes.minTouchTarget)
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .clip(shape)
            .background(background, shape)
            .border(BloxBorders.frame, frame, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            // A narrow margin: « Au hasard », in the pixel font twice as large, fits half the width only so.
            .padding(horizontal = BloxBorders.frame + 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(
            text = text,
            color = ink,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
