package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A window that asks before an action ([title], a question read as a heading by TalkBack): [note], if any, then the
 * answers, the safe one first, [dismiss], which calls [onDismiss], then [confirm], which calls [onConfirm], filled in
 * `accent`, or in `danger` when it cannot be undone ([danger]). An answer that could not be written, [failed], is told
 * above the note, read by TalkBack as it shows, the window still open to try again or give up.
 */
@Composable
fun ConfirmWindow(
    title: String,
    dismiss: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    danger: Boolean = false,
    failed: Boolean = false,
) {
    WindowPanel(title = title, modifier = modifier) {
        if (failed) WindowFailure()
        note?.let { WindowText(it) }
        WindowAnswers(
            dismiss = dismiss,
            confirm = confirm,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            danger = danger,
        )
    }
}
