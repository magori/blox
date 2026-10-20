package ch.magori.blox.feature.musics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.cutName
import ch.magori.blox.core.designsystem.component.DeleteWindow
import ch.magori.blox.core.designsystem.component.NamingWindow
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.component.nameTooLongText

/**
 * *Renommer*: a field on the name of the music, cut at [SavedMusic.MAX_NAME_LENGTH] characters (`cutName`); a name
 * refused ([problemOf]) is told under it, and *Renommer* is disabled. *Annuler*, or Back, renames nothing. A rename
 * that could not be written is told, the dialog still open.
 */
@Composable
internal fun RenameDialog(
    dialog: MyMusicsDialog.Rename,
    problemOf: (String) -> NameProblem?,
    onRenamed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(dialog.name) }
    val problem = problemOf(name)
    WindowDialog(onDismissRequest = onDismiss) {
        NamingWindow(
            title = stringResource(R.string.feature_musics_rename_title, dialog.name),
            name = name,
            onNameChanged = { name = cutName(it) },
            problem = problem?.let { nameProblemMessage(it) },
            confirm = stringResource(R.string.feature_musics_rename_confirm),
            onConfirm = { onRenamed(name) },
            onDismiss = onDismiss,
            failed = dialog.failed,
        )
    }
}

/**
 * *Supprimer « X » ?*: the deletion may be undone just after; a music played during the games is told to give way to
 * the music of the home screen. *Supprimer*, in `danger`, or *Garder la musique*. A deletion that could not be
 * written is told, the dialog still open.
 */
@Composable
internal fun DeleteDialog(
    dialog: MyMusicsDialog.ConfirmDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(onDismissRequest = onDismiss) {
        DeleteWindow(
            name = dialog.name,
            keep = stringResource(R.string.feature_musics_delete_keep),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            note = stringResource(
                if (dialog.inGame) R.string.feature_musics_delete_note_in_game else R.string.feature_musics_delete_note,
            ),
            failed = dialog.failed,
        )
    }
}

/** What the dialog says of a name refused. */
@Composable
@ReadOnlyComposable
private fun nameProblemMessage(problem: NameProblem): String = when (problem) {
    NameProblem.BLANK -> stringResource(R.string.feature_musics_name_blank)
    NameProblem.TAKEN -> stringResource(R.string.feature_musics_name_taken)
    NameProblem.TOO_LONG -> nameTooLongText(SavedMusic.MAX_NAME_LENGTH)
}
