package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import ch.magori.blox.core.designsystem.R
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * A window where a [name] is typed, on as many lines as it needs, under [title]: why the name cannot be taken,
 * [problem], is told under the field, read by TalkBack as it shows, and [confirm] is disabled meanwhile; *Entrée*
 * confirms too, the name allowed, and a line break pasted is a space. A button [freeName] (« Prendre « X 2 » ») may
 * offer a name in its place; then come [extra] parts, and the answers, *Annuler* and [confirm]. A name that could not be written, [failed], is told, the window still open.
 */
@Composable
fun NamingWindow(
    title: String,
    name: String,
    onNameChanged: (String) -> Unit,
    problem: String?,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    freeName: String? = null,
    onFreeNameTaken: () -> Unit = {},
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val palette = BloxTheme.palette
    WindowPanel(title = title, modifier = modifier) {
        if (failed) WindowFailure()
        OutlinedTextField(
            value = name,
            // A name holds on one line: a line break typed (Entrée on a keyboard) is a space.
            onValueChange = { onNameChanged(it.replace(LINE_BREAK, " ")) },
            label = { Text(stringResource(R.string.core_designsystem_window_name)) },
            // On as many lines as it needs, never scrolled: a name of 40 characters stays whole in every theme, with the
            // font twice as large too (four lines in Original).
            singleLine = false,
            isError = problem != null,
            supportingText = problem?.let {
                { Text(text = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (problem == null) onConfirm() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.ink,
                unfocusedTextColor = palette.ink,
                focusedBorderColor = palette.ink,
                unfocusedBorderColor = palette.panelBorder,
                focusedLabelColor = palette.ink,
                unfocusedLabelColor = palette.label,
                cursorColor = palette.ink,
                errorBorderColor = palette.ink,
                errorLabelColor = palette.ink,
                errorSupportingTextColor = palette.ink,
                errorTextColor = palette.ink,
                errorCursorColor = palette.ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        freeName?.let { WindowButton(text = it, onClick = onFreeNameTaken, modifier = Modifier.fillMaxWidth()) }
        extra()
        WindowAnswers(confirm = confirm, onConfirm = onConfirm, onDismiss = onDismiss, confirmEnabled = problem == null)
    }
}

/** « 40 caractères au plus. »: a name longer than [maxLength] characters, refused. */
@Composable
@ReadOnlyComposable
fun nameTooLongText(maxLength: Int): String =
    pluralStringResource(R.plurals.core_designsystem_window_name_too_long, maxLength, maxLength)

/** A line break, from a keyboard or pasted. */
private val LINE_BREAK = Regex("\r\n|[\r\n]")
