package ch.magori.blox.feature.composer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.theme.BloxAlphas
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The sizes shared by the parts of the editor, in one place. */
internal object ComposerSizes {
    /** Around the screen. */
    val padding = 12.dp

    /** Between two blocks of the screen, and between two buttons. */
    val gap = 4.dp
}

/**
 * A button of the editor, framed as a panel: [text] on it, [description] read by TalkBack in its place when the text
 * alone is too short to say what it does. Disabled when it has no effect: greyed out, and announced as such.
 */
@Composable
internal fun ToolButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    val palette = BloxTheme.palette
    ButtonBox(
        background = palette.panel,
        frame = palette.panelBorder,
        modifier = modifier
            .alpha(if (enabled) 1f else BloxAlphas.DISABLED)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .described(description),
    ) {
        ButtonLabel(text = text, color = palette.ink)
    }
}

/**
 * A choice of the editor among others (a tab, the mode of the touches, a bar), read by TalkBack with its [role]: a
 * selected one is filled in [selectedColor], `accent` unless told otherwise, its label in [selectedInk]; an unselected
 * one is framed, as on the home screen. In every theme the two differ by 3:1 at least in luminance (`ChoiceColorsTest`
 * of the home screen, `TrackColorsTest` for the tabs), so that hue is not their only cue.
 */
@Composable
internal fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    description: String? = null,
    selectedColor: Color = BloxTheme.palette.accent,
    selectedInk: Color = BloxTheme.palette.accentInk,
) {
    val palette = BloxTheme.palette
    val background = if (selected) selectedColor else palette.panel
    ButtonBox(
        background = background,
        frame = if (selected) background else palette.panelBorder,
        modifier = modifier
            .selectable(selected = selected, role = role, onClick = onClick)
            .described(description),
    ) {
        ButtonLabel(text = text, color = if (selected) selectedInk else palette.ink)
    }
}

@Composable
private fun ButtonBox(
    background: Color,
    frame: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = BloxTheme.shapes.button
    Box(
        modifier = modifier
            .heightIn(min = BloxSizes.minTouchTarget)
            .background(background, shape)
            .border(BloxBorders.frame, frame, shape)
            .padding(horizontal = BloxBorders.frame + 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** The label of a button, on one line, shrunk if needed. */
@Composable
private fun ButtonLabel(
    text: String,
    color: Color,
) {
    SingleLineText(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium.copy(lineHeight = TextUnit.Unspecified),
        textAlign = TextAlign.Center,
    )
}

/** TalkBack reads [description], when there is one, in place of the label of the button. */
private fun Modifier.described(description: String?): Modifier =
    if (description == null) this else semantics { contentDescription = description }
