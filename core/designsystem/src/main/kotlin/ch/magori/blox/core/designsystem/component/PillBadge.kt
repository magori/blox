package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** Around the label of a badge, unless told otherwise: « ouvert » in Mes jardins. */
private val BadgePadding: PaddingValues = PaddingValues(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1)

/** The check of a badge [withCheck][PillBadge]. */
private val BadgeCheck: Dp = 12.dp

/** The colours of a badge. */
internal data class BadgeLook(val fill: Color, val ink: Color)

/** A badge in [palette]: `accent-ink` on `accent`, in every theme, Original included. */
internal fun badgeLook(palette: BloxPalette): BadgeLook = BadgeLook(fill = palette.accent, ink = palette.accentInk)

/**
 * A badge (EN JEU in Mes musiques, NOUVEAU RECORD or NOUVEAU MEILLEUR at the end of a game, « ouvert » in Mes jardins):
 * a short label, [text] as given, on one line unless [maxLines] says otherwise, as [badgeLook] colours it, in a
 * straight pill with no frame, in [style] (`small` unless told otherwise), with a check before it if [withCheck].
 * [padding] is the room around its content. Unlike a [Sticker], it neither leans nor is framed. What TalkBack reads of
 * it belongs to the screen, in [modifier].
 */
@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small),
    maxLines: Int = 1,
    withCheck: Boolean = false,
    padding: PaddingValues = BadgePadding,
) {
    val look = badgeLook(BloxTheme.palette)
    Row(
        modifier = modifier
            .background(look.fill, BloxTheme.shapes.pill)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
        if (withCheck) CheckMark(color = look.ink, modifier = Modifier.size(BadgeCheck))
        Text(text = text, color = look.ink, style = style, maxLines = maxLines)
    }
}
