package ch.magori.blox.feature.musics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.cutName
import ch.magori.blox.core.designsystem.component.DeleteWindow
import ch.magori.blox.core.designsystem.component.RenameWindow
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.text.NameRefusal
import ch.magori.blox.core.designsystem.text.NamedThing
import ch.magori.blox.core.designsystem.text.nameRefusalText

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
        RenameWindow(
            current = dialog.name,
            name = name,
            onNameChanged = { name = cutName(it) },
            maxLength = SavedMusic.MAX_NAME_LENGTH,
            problem = problem?.let {
                nameRefusalText(NameRefusal.valueOf(it.name), NamedThing.MUSIC, SavedMusic.MAX_NAME_LENGTH)
            },
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
