package ch.magori.blox.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.designsystem.R

/**
 * *Supprimer « [name] » ?*, a [ConfirmWindow]: [note], if any, then the answers, [keep], which calls [onDismiss], and
 * *Supprimer*, in `danger`, which calls [onConfirm]. A deletion that could not be written, [failed], is told, the
 * window still open.
 */
@Composable
fun DeleteWindow(
    name: String,
    keep: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    failed: Boolean = false,
) {
    ConfirmWindow(
        title = stringResource(R.string.core_designsystem_window_delete_title, name),
        dismiss = keep,
        confirm = stringResource(R.string.core_designsystem_window_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        note = note,
        danger = true,
        failed = failed,
    )
}
