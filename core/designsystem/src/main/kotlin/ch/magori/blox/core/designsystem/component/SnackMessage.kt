package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.designsystem.theme.shrinkingLineHeight

/** The action a message offers (« Annuler »): its [label], and what touching it does, [onClick]. */
@Immutable
data class MessageAction(val label: String, val onClick: () -> Unit)

/**
 * A message that goes away (« « X » est supprimée. », « Dé tiré. »): [text] in the colour of the page on the ink,
 * `radius-snack`; its [action] if it offers one, in text, and its cross if it can be closed, [onClose], in bare discs.
 * TalkBack reads it as it shows, after what it is reading (a polite live region). Its action and its cross stand beside
 * its text when they hold there, else under it, at the end: its text is never cut, a word too long for its line
 * shrinking rather than be cut. Where it stands and how long it stays are the screen's.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SnackMessage(
    text: String,
    modifier: Modifier = Modifier,
    action: MessageAction? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = messageColors(BloxTheme.palette)
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.fill, BloxTheme.shapes.snack)
            .padding(start = BloxSpaces.space4, end = BloxSpaces.space1)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val style = MaterialTheme.typography.bodyMedium.shrinkingLineHeight()
        Text(
            text = text,
            color = colors.text,
            style = style,
            autoSize = shrinkToWholeWords(style.fontSize),
            modifier = Modifier.padding(vertical = BloxSpaces.space3).padding(end = BloxSpaces.space2),
        )
        if (action != null || onClose != null) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (action != null) {
                    BareButtonOnInk(
                        family = ButtonFamily.SECONDARY,
                        onClick = action.onClick,
                        size = ButtonSize.COMPACT,
                    ) {
                        Text(text = action.label)
                    }
                }
                if (onClose != null) {
                    GlyphButtonOnInk(
                        description = stringResource(R.string.core_designsystem_message_close),
                        onClick = onClose,
                    ) { ink -> Glyphs.close(this, ink) }
                }
            }
        }
    }
}

/** The colours of a message: its [fill], and its [text], which its action and its cross take at rest. */
@Immutable
internal data class MessageColors(val fill: Color, val text: Color)

/** The colours of a message in [palette]: the colour of the page on the ink, the reverse of the screen. */
internal fun messageColors(palette: BloxPalette) = MessageColors(fill = palette.ink, text = palette.page)
