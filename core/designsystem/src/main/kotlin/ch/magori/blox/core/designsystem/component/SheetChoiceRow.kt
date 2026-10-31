package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords

/**
 * A choice of a sheet, one of a group ([SheetChoices]), [selected] or not: a round
 * framed in `button-outline`, filled in `accent` and bearing its check ([CheckMark]) once chosen, then its [label] and,
 * under it, its [subLine], which says what it is. The whole row, [BloxSizes.settingRow] high at least, is the choice,
 * for the finger and for TalkBack, which reads it once as a radio button: « Surprise, Un peu de tout, case d'option,
 * sélectionnée ». Touched, it calls [onClick]: the caller holds the choice. Pressed, it fills with `button-pressed`;
 * focused by the keyboard, it bears the ring of the buttons. With [oneLineSubLine], the sub-line, a name rather than
 * words (a file: « orage-d-ete.ogg »), holds one line at the size of the font, cut in its middle when too long, so that
 * its start and its end stay: TalkBack reads it whole. Its least width (intrinsic) is then its whole width.
 */
@Composable
fun SheetChoiceRow(
    label: String,
    subLine: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    oneLineSubLine: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val palette = BloxTheme.palette
    val bubble = BloxTheme.buttons.secondary
    val shape = BloxTheme.shapes.md
    val ring = buttonFocusRing(focused, BloxTheme.buttons)
    Row(
        modifier = modifier
            .focusRing(ring, shape)
            .fillMaxWidth()
            .heightIn(min = BloxSizes.settingRow)
            .background(if (pressed) bubble.pressedFill else Color.Transparent, shape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        ChoiceRound(selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            val ink = if (pressed) bubble.pressedInk else palette.ink
            val labelStyle = settingLabelStyle()
            Text(text = label, color = ink, style = labelStyle, autoSize = shrinkToWholeWords(labelStyle.fontSize))
            val subLineStyle = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small)
            val subLineInk = if (pressed) bubble.pressedInk else palette.label
            if (oneLineSubLine) {
                Text(
                    text = subLine,
                    color = subLineInk,
                    style = subLineStyle,
                    // Its least width is its whole width, never that of a piece between hyphens: a row which places its
                    // actions by the least width of its choice leaves the name room when it can; it is cut when not.
                    modifier = Modifier.width(IntrinsicSize.Max),
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
            } else {
                Text(
                    text = subLine,
                    color = subLineInk,
                    style = subLineStyle,
                    autoSize = shrinkToWholeWords(subLineStyle.fontSize),
                )
            }
        }
    }
}

/**
 * The choices of a sheet, its [content] of [SheetChoiceRow], one above the other: one group, which TalkBack reads as
 * such, each choice as one of them (« 2 sur 8 »).
 */
@Composable
fun SheetChoices(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.selectableGroup(), content = content)
}

/** The round of a choice: empty, framed in `button-outline`; chosen, filled in `accent`, its check in `accent-ink`. */
@Composable
private fun ChoiceRound(selected: Boolean) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.pill
    val fill = if (selected) palette.accent else Color.Transparent
    Box(
        modifier = Modifier
            .size(ROUND)
            .background(fill, shape)
            .border(BloxBorders.frame, if (selected) palette.accent else palette.buttonOutline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckMark(palette.accentInk, Modifier.testTag(SHEET_CHOICE_CHECK_TAG).size(CHECK))
    }
}

/** The tag of the check of a chosen row, read by the tests. */
internal const val SHEET_CHOICE_CHECK_TAG = "sheet_choice_check"

/** The round of a choice, and the check it bears once chosen. */
private val ROUND = 24.dp
private val CHECK = 14.dp
