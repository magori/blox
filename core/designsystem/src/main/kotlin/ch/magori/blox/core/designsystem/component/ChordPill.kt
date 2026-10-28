package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The pill of a chord (the Locksmith, the musical garden): its [label], the name of its numeral (« vi », « ♭VII »), on
 * the page framed in `button-outline`, or filled in `accent` once [played]. At least 44 dp wide; the screen may give it
 * its own width in [modifier]. What TalkBack reads of the chords belongs to the screen, which reads the band whole.
 */
@Composable
fun ChordPill(
    label: String,
    played: Boolean,
    modifier: Modifier = Modifier,
) {
    val fill = if (played) BloxTheme.palette.accent else BloxTheme.palette.page
    val frame = if (played) BloxTheme.palette.accent else BloxTheme.palette.buttonOutline
    Box(
        modifier = modifier
            .widthIn(min = CHORD_PILL)
            .background(fill, BloxTheme.shapes.pill)
            .border(BloxBorders.frame, frame, BloxTheme.shapes.pill)
            .padding(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (played) BloxTheme.palette.accentInk else BloxTheme.palette.ink,
            style = BloxTheme.typography.buttonLabel,
        )
    }
}

/** The narrowest pill of a chord. */
private val CHORD_PILL = 44.dp
