package ch.magori.blox.feature.musics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxTheme

/**
 * *Renommer*: a field on the name of the music, of [SavedMusic.MAX_NAME_LENGTH] characters at the most; a name refused
 * ([problemOf]) is told under it, and *Renommer* is disabled. *Annuler*, or Back, renames nothing. A rename that could
 * not be written is told, the dialog still open.
 */
@Composable
internal fun RenameDialog(
    dialog: MyMusicsDialog.Rename,
    problemOf: (String) -> NameProblem?,
    onRenamed: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = BloxTheme.palette
    val field = rememberTextFieldState(initialText = dialog.name)
    val problem = problemOf(field.text.toString())
    DialogPanel(onDismiss = onDismiss) {
        DialogTitle(stringResource(R.string.feature_musics_rename_title, dialog.name))
        if (dialog.failed) WriteFailed()
        OutlinedTextField(
            state = field,
            label = { Text(stringResource(R.string.feature_musics_rename_field)) },
            inputTransformation = InputTransformation.maxLength(SavedMusic.MAX_NAME_LENGTH),
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = problem != null,
            supportingText = problem?.let { { Text(nameProblemMessage(it)) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            onKeyboardAction = { if (problem == null) onRenamed(field.text.toString()) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.ink,
                unfocusedBorderColor = palette.panelBorder,
                focusedLabelColor = palette.ink,
                unfocusedLabelColor = palette.label,
                cursorColor = palette.ink,
                errorBorderColor = palette.danger,
                errorLabelColor = palette.danger,
                errorSupportingTextColor = palette.danger,
                errorCursorColor = palette.danger,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_rename_confirm),
            background = palette.accent,
            ink = palette.accentInk,
            frame = palette.accent,
            onClick = { onRenamed(field.text.toString()) },
            enabled = problem == null,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_rename_cancel),
            background = palette.panel,
            ink = palette.ink,
            frame = palette.panelBorder,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
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
    val palette = BloxTheme.palette
    DialogPanel(onDismiss = onDismiss) {
        DialogTitle(stringResource(R.string.feature_musics_delete_title, dialog.name))
        if (dialog.failed) WriteFailed()
        Text(
            text = stringResource(
                if (dialog.inGame) R.string.feature_musics_delete_note_in_game else R.string.feature_musics_delete_note,
            ),
            color = palette.label,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_delete_confirm),
            background = palette.danger,
            ink = palette.dangerInk,
            frame = palette.danger,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButtonBox(
            text = stringResource(R.string.feature_musics_delete_keep),
            background = palette.panel,
            ink = palette.ink,
            frame = palette.panelBorder,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A dialog of the list, framed as a panel; Back or a touch outside it dismisses it. */
@Composable
private fun DialogPanel(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.panel, shape)
                .border(BloxBorders.frame, palette.panelBorder, shape)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(
        text = text,
        color = BloxTheme.palette.ink,
        style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleMedium),
        modifier = Modifier.semantics { heading() },
    )
}

/** What the dialog says of a name refused. */
@Composable
private fun nameProblemMessage(problem: NameProblem): String = when (problem) {
    NameProblem.BLANK -> stringResource(R.string.feature_musics_name_blank)

    NameProblem.TAKEN -> stringResource(R.string.feature_musics_name_taken)

    NameProblem.TOO_LONG -> pluralStringResource(
        R.plurals.feature_musics_name_too_long,
        SavedMusic.MAX_NAME_LENGTH,
        SavedMusic.MAX_NAME_LENGTH,
    )
}

/** « L'enregistrement a échoué. », in `danger`, read by TalkBack as soon as it shows: the dialog stays open. */
@Composable
private fun WriteFailed() {
    Text(
        text = stringResource(R.string.feature_musics_write_failed),
        color = BloxTheme.palette.danger,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
