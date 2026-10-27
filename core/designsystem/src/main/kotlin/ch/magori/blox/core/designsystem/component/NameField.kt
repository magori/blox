package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxPalette
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * The field where a [name] is typed, under its label *Nom*: a box of `panel-inset`, 56 dp high at least, its corners
 * `radius-md`, the name on as many lines as it needs, never scrolled; a line break typed (*Entrée*) or pasted is a
 * space, and *Entrée* calls [onDone]. Under it, why the name cannot be taken, [problem], beside the alert, read by
 * TalkBack as it shows, and the count of its characters out of [maxLength] (« 9/40 », read « 9 caractères sur 40 »).
 * The box is framed in `ink` while it has the focus, in `danger` while the name is refused ([nameFieldFrame]).
 */
@Composable
fun NameField(
    name: String,
    onNameChanged: (String) -> Unit,
    maxLength: Int,
    problem: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val frame = nameFieldFrame(focused = focused, refused = problem != null, palette = palette)
    val shape = BloxTheme.shapes.md
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
        BasicTextField(
            value = name,
            onValueChange = { onNameChanged(it.replace(LINE_BREAK, " ")) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
            cursorBrush = SolidColor(palette.ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (problem != null) error(problem) },
            decorationBox = { innerTextField ->
                Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
                    Text(
                        text = stringResource(R.string.core_designsystem_window_name),
                        color = palette.label,
                        style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small),
                    )
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = FieldHeight)
                            .background(palette.panelInset, shape)
                            .then(if (frame == null) Modifier else Modifier.border(BloxBorders.frame, frame, shape))
                            .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space2),
                    ) {
                        innerTextField()
                    }
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2)) {
            Box(modifier = Modifier.weight(1f)) { problem?.let { NameProblem(it, palette.danger) } }
            NameCount(count = name.codePointCount(0, name.length), maxLength = maxLength, color = palette.label)
        }
    }
}

/** Why the name cannot be taken, [text] in [color] beside the alert, read by TalkBack as it shows. */
@Composable
private fun NameProblem(
    text: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        BloxIcon(BloxIcon.ALERT, tint = color, modifier = Modifier.padding(top = 2.dp))
        Text(text = text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/** « [count]/[maxLength] », in [color], read by TalkBack « 9 caractères sur 40 ». */
@Composable
private fun NameCount(
    count: Int,
    maxLength: Int,
    color: Color,
) {
    val read = pluralStringResource(R.plurals.core_designsystem_name_count, count, count, maxLength)
    Text(
        text = "$count/$maxLength",
        color = color,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics { contentDescription = read },
    )
}

/**
 * The frame of the box of a name field: in `danger` while the name is [refused], whether [focused] or not; in `ink`
 * while it has the focus; none otherwise, the box standing out by its `panel-inset`.
 */
internal fun nameFieldFrame(
    focused: Boolean,
    refused: Boolean,
    palette: BloxPalette,
): Color? = when {
    refused -> palette.danger
    focused -> palette.ink
    else -> null
}

/** The least height of the box of a name field. */
private val FieldHeight = 56.dp

/** A line break, from a keyboard or pasted. */
private val LINE_BREAK = Regex("\r\n|[\r\n]")
