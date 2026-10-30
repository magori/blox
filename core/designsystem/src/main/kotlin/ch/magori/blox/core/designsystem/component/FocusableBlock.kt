package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A block of text that acts on nothing, which the keyboard may still focus: where the focus goes when what held it
 * leaves (a list left empty, « Aucun jardin gardé. »). Read in one by TalkBack, its [content] one under the other as
 * [verticalArrangement] says; focused, it bears the ring of the buttons, in `buttons.focus` (WCAG 2.4.7), drawn for a
 * light ground, of a page or of a sheet, as theirs. [modifier] applies to the block, and may hold its `focusRequester`.
 */
@Composable
fun FocusableBlock(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val ring = buttonFocusRing(focused, BloxTheme.buttons)
    Column(
        modifier = modifier
            .focusRing(ring, BloxTheme.shapes.md)
            .semantics(mergeDescendants = true) {}
            .focusable(interactionSource = interactionSource),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
