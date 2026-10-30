package ch.magori.blox.feature.musics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.data.cutName
import ch.magori.blox.core.designsystem.component.DeleteWindow
import ch.magori.blox.core.designsystem.component.RenameWindow
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.component.WindowOverPage
import ch.magori.blox.core.designsystem.text.NameRefusal
import ch.magori.blox.core.designsystem.text.NamedThing
import ch.magori.blox.core.designsystem.text.nameRefusalText
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.designsystem.theme.UI_MODE_NIGHT
import ch.magori.blox.core.music.ScoreId

/**
 * *Renommer*, in a window over the list ([MusicRenameWindow]): the name typed is kept here, out of the window, cut at
 * [SavedMusic.MAX_NAME_LENGTH] characters (`cutName`).
 */
@Composable
internal fun RenameDialog(
    dialog: MyMusicsDialog.Rename,
    problemOf: (String) -> NameProblem?,
    onRenamed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(dialog.name) }
    WindowDialog(onDismissRequest = onDismiss) {
        MusicRenameWindow(dialog, name, { name = cutName(it) }, problemOf, onRenamed, onDismiss)
    }
}

/**
 * *Renommer*: a field on the [name] typed; a name refused ([problemOf]) is told under it, and *Renommer* is disabled.
 * *Annuler*, or Back, renames nothing. A rename that could not be written is told, the window still open.
 */
@Composable
private fun MusicRenameWindow(
    dialog: MyMusicsDialog.Rename,
    name: String,
    onNameChanged: (String) -> Unit,
    problemOf: (String) -> NameProblem?,
    onRenamed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val problem = problemOf(name)
    RenameWindow(
        current = dialog.name,
        name = name,
        onNameChanged = onNameChanged,
        maxLength = SavedMusic.MAX_NAME_LENGTH,
        problem = problem?.let {
            nameRefusalText(NameRefusal.valueOf(it.name), NamedThing.MUSIC, SavedMusic.MAX_NAME_LENGTH)
        },
        onConfirm = { onRenamed(name) },
        onDismiss = onDismiss,
        failed = dialog.failed,
    )
}

/** *Supprimer « X » ?*, in a window over the list ([MusicDeleteWindow]). */
@Composable
internal fun DeleteDialog(
    dialog: MyMusicsDialog.ConfirmDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(onDismissRequest = onDismiss) { MusicDeleteWindow(dialog, onConfirm, onDismiss) }
}

/**
 * *Supprimer « X » ?*: the deletion may be undone just after; a music played during the games is told to give way to
 * the music of the home screen. *Garder la musique*, the safe answer, first, then *Supprimer*, filled in `danger`. A
 * deletion that could not be written is told, the window still open.
 */
@Composable
private fun MusicDeleteWindow(
    dialog: MyMusicsDialog.ConfirmDelete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
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

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

/** « Supprimer « Thème du soir » ? », the music played during the games, over the page in Couleurs. */
@Composable
private fun DeletingPreview() = BloxTheme(kind = BloxThemeKind.COLORFUL) {
    WindowOverPage {
        MusicDeleteWindow(
            dialog = MyMusicsDialog.ConfirmDelete(ScoreId.Personal("1"), "Thème du soir", inGame = true),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsDeletingPreview() = DeletingPreview()

@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
internal fun MyMusicsDeletingNightPreview() = DeletingPreview()

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsDeletingLargeFontPreview() = DeletingPreview()

/** « Renommer « Ma marche » », over the page in Couleurs. */
@Composable
private fun RenamingPreview() = BloxTheme(kind = BloxThemeKind.COLORFUL) {
    WindowOverPage {
        val dialog = MyMusicsDialog.Rename(ScoreId.Personal("3"), "Ma marche")
        MusicRenameWindow(
            dialog = dialog,
            name = dialog.name,
            onNameChanged = {},
            problemOf = { null },
            onRenamed = {},
            onDismiss = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun MyMusicsRenamingPreview() = RenamingPreview()

@Preview(widthDp = 360, heightDp = 760, uiMode = UI_MODE_NIGHT)
@Composable
internal fun MyMusicsRenamingNightPreview() = RenamingPreview()

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun MyMusicsRenamingLargeFontPreview() = RenamingPreview()
