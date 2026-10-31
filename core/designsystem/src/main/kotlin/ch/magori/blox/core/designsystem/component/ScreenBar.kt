package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The bar of a screen reached from another (Paramètres, Mes musiques): the disc Retour of 48 dp, read
 * [backDescription], which calls [onBack], then [title] in `screen-title`, 12 dp after it (`space3`, as V21 and V32
 * draw it), a heading for TalkBack, on one line, shrunk if needed: the pixel font of Original would cut a word. It
 * shrinks no further than the floor of the texts (`shrinkFloor`): a title meant for it is short.
 */
@Composable
fun ScreenBar(
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        GlyphButton(
            description = backDescription,
            onClick = onBack,
            glyph = { Glyphs.back(this, it) },
        )
        SingleLineText(
            text = title,
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.titleLarge.merge(BloxTheme.typography.screenTitle),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}
