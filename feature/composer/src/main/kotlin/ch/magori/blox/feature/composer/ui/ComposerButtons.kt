package ch.magori.blox.feature.composer.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.FramedBox
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes shared by the parts of the editor, in one place. */
internal object ComposerSizes {
    /** Around the screen. */
    val padding = 12.dp

    /** Between two buttons. */
    val gap = 4.dp

    /** Between two blocks of the screen (V33). */
    val blockGap = 6.dp

    /**
     * The least height of the grid, its line of beats and its hint included: three lines of notes in sight at least on
     * a small phone with a large font, where the page scrolls to keep it.
     */
    val gridMin = 200.dp
}

/**
 * A choice among others of the save window (a style), read by TalkBack with its [role]: a selected one is filled in
 * `accent`, its label in `accent-ink`; an unselected one is framed. A choice as wide as its label, rather than given its
 * width, leaves [labelPadding] on each side of it. Left until the save window becomes a sheet (P5.104).
 */
@Composable
internal fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    labelPadding: Dp = 0.dp,
) {
    val palette = BloxTheme.palette
    val background = if (selected) palette.accent else palette.panel
    val ink = if (selected) palette.accentInk else palette.ink
    FramedBox(
        background = background,
        frame = if (selected) background else palette.panelBorder,
        labelPadding = labelPadding,
        modifier = modifier.selectable(selected = selected, role = role, onClick = onClick),
    ) {
        SingleLineText(
            text = text,
            color = ink,
            style = MaterialTheme.typography.labelMedium.copy(lineHeight = TextUnit.Unspecified),
            textAlign = TextAlign.Center,
        )
    }
}
