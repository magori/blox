package ch.magori.blox.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import ch.magori.blox.core.designsystem.R

/**
 * A window where a [name] of [maxLength] characters at most is typed ([NameField]), under [title]: why the name cannot
 * be taken, [problem], is told under the field, read by TalkBack as it shows, and [confirm] is disabled meanwhile;
 * *Entrée* confirms too, the name allowed. A button [freeName] (« Prendre « X 2 » ») may offer a name in its place;
 * then come [extra] parts, and the answers, *Annuler* and [confirm]. A name that could not be written, [failed], is
 * told, the window still open. While the name is being written, [busy], [confirm] is disabled and *Entrée* does
 * nothing.
 */
@Composable
fun NamingWindow(
    title: String,
    name: String,
    onNameChanged: (String) -> Unit,
    maxLength: Int,
    problem: String?,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    freeName: String? = null,
    onFreeNameTaken: () -> Unit = {},
    busy: Boolean = false,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    WindowPanel(title = title, modifier = modifier) {
        if (failed) WindowFailure()
        NameField(
            name = name,
            onNameChanged = onNameChanged,
            maxLength = maxLength,
            problem = problem,
            onDone = { if (problem == null && !busy) onConfirm() },
        )
        freeName?.let { WindowButton(text = it, onClick = onFreeNameTaken, modifier = Modifier.fillMaxWidth()) }
        extra()
        WindowAnswers(
            confirm = confirm,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmEnabled = problem == null && !busy,
        )
    }
}

/** « 40 caractères au plus. »: a name longer than [maxLength] characters, refused. */
@Composable
@ReadOnlyComposable
fun nameTooLongText(maxLength: Int): String =
    pluralStringResource(R.plurals.core_designsystem_window_name_too_long, maxLength, maxLength)
