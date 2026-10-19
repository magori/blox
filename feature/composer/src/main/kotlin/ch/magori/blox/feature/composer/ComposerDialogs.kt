package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.feature.composer.model.SaveProblem
import ch.magori.blox.feature.composer.ui.ComposerSizes
import ch.magori.blox.feature.composer.ui.ToolButton

/** What the windows of the editor do, each answer to its callback; no logic here. */
@Immutable
internal class DialogActions(
    val onNameChanged: (String) -> Unit,
    val onFreeNameTaken: () -> Unit,
    val onForGamesChanged: (Boolean) -> Unit,
    val onSaveConfirmed: () -> Unit,
    val onRenameConfirmed: () -> Unit,
    val onDismissed: () -> Unit,
    val onLeaveWithoutSaving: () -> Unit,
    val onSaveBeforeLeaving: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
) {
    companion object {
        /** Windows that do nothing: previews and screenshots. */
        val None = DialogActions({}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/** The window [dialog] over the editor, if any, for the music called [name]. */
@Composable
internal fun ComposerDialogHost(
    dialog: ComposerDialog?,
    name: String?,
    actions: DialogActions,
) {
    if (dialog == null) return
    // Wider than the default of the platform: a title in the pixel font, twice as large, keeps its words whole.
    Dialog(onDismissRequest = actions.onDismissed, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ComposerWindow(
            dialog = dialog,
            name = name,
            actions = actions,
            modifier = Modifier.padding(horizontal = WindowMargin),
        )
    }
}

/** The panel of the window [dialog]: apart from its [Dialog], so that the screenshots show it. */
@Composable
internal fun ComposerWindow(
    dialog: ComposerDialog,
    name: String?,
    actions: DialogActions,
    modifier: Modifier = Modifier,
) {
    when (dialog) {
        is ComposerDialog.Save -> NamingWindow(
            title = stringResource(R.string.feature_composer_save_title),
            confirm = stringResource(R.string.feature_composer_save),
            dialog = dialog,
            onConfirm = actions.onSaveConfirmed,
            actions = actions,
            modifier = modifier,
        ) {
            ForGamesBox(checked = dialog.forGames, onChecked = actions.onForGamesChanged)
        }

        is ComposerDialog.Rename -> NamingWindow(
            title = stringResource(R.string.feature_composer_rename_title),
            confirm = stringResource(R.string.feature_composer_rename),
            dialog = dialog,
            onConfirm = actions.onRenameConfirmed,
            actions = actions,
            modifier = modifier,
        )

        ComposerDialog.Leave -> WindowPanel(title = stringResource(R.string.feature_composer_leave_title), modifier) {
            WindowText(stringResource(R.string.feature_composer_leave_text))
            // One answer a line: « Quitter sans enregistrer » is long, and the font may be twice as large.
            ToolButton(
                wraps = true,
                text = stringResource(R.string.feature_composer_save),
                onClick = actions.onSaveBeforeLeaving,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ToolButton(
                wraps = true,
                text = stringResource(R.string.feature_composer_leave_without_saving),
                onClick = actions.onLeaveWithoutSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            ToolButton(
                wraps = true,
                text = stringResource(R.string.feature_composer_continue),
                onClick = actions.onDismissed,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is ComposerDialog.Delete -> WindowPanel(
            title = stringResource(R.string.feature_composer_delete_title, name.orEmpty()),
            modifier = modifier,
        ) {
            if (dialog.failed) WriteFailed()
            if (dialog.isGameMusic) WindowText(stringResource(R.string.feature_composer_delete_game_music))
            Answers(
                dismiss = stringResource(R.string.feature_composer_keep),
                confirm = stringResource(R.string.feature_composer_delete),
                onDismiss = actions.onDismissed,
                onConfirm = actions.onDeleteConfirmed,
                danger = true,
            )
        }
    }
}

/**
 * A window where a name is typed ([dialog]), then [extra] content, and its answers: [confirm], disabled while the name
 * has a problem, which the window says, offering a free name when the name is taken. *Entrée* confirms too.
 */
@Composable
private fun NamingWindow(
    title: String,
    confirm: String,
    dialog: ComposerDialog.Naming,
    onConfirm: () -> Unit,
    actions: DialogActions,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val palette = BloxTheme.palette
    WindowPanel(title = title, modifier = modifier) {
        if (dialog.failed) WriteFailed()
        OutlinedTextField(
            value = dialog.name,
            onValueChange = actions.onNameChanged,
            label = { Text(stringResource(R.string.feature_composer_name_label)) },
            singleLine = true,
            isError = dialog.problem != null,
            supportingText = dialog.problem?.let { problem ->
                {
                    Text(
                        text = stringResource(problem.text),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
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
        dialog.freeName?.let { free ->
            ToolButton(
                wraps = true,
                text = stringResource(R.string.feature_composer_take_free_name, free),
                onClick = actions.onFreeNameTaken,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        extra()
        Answers(
            dismiss = stringResource(R.string.feature_composer_cancel),
            confirm = confirm,
            onDismiss = actions.onDismissed,
            onConfirm = onConfirm,
            confirmEnabled = dialog.problem == null,
        )
    }
}

/** *Jouer cette musique pendant les parties*, [checked] or not, read by TalkBack as a check box. */
@Composable
private fun ForGamesBox(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val palette = BloxTheme.palette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BloxSizes.minTouchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChecked),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = palette.accent,
                checkmarkColor = palette.accentInk,
                uncheckedColor = palette.ink,
            ),
        )
        Text(
            text = stringResource(R.string.feature_composer_for_games),
            color = palette.ink,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The two answers of a window, side by side: [dismiss], then [confirm], filled in `accent`, or `danger` if it deletes. */
@Composable
private fun Answers(
    dismiss: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap)) {
        ToolButton(text = dismiss, onClick = onDismiss, wraps = true, modifier = Modifier.weight(1f))
        ToolButton(
            wraps = true,
            text = confirm,
            onClick = onConfirm,
            enabled = confirmEnabled,
            primary = true,
            danger = danger,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A window: a panel of the theme, its [title], then its [content], scrolling when the font is large. */
@Composable
private fun WindowPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = BloxTheme.palette
    val shape = BloxTheme.shapes.panel
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.panel, shape)
            .border(BloxBorders.frame, palette.panelBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(WindowPadding),
        verticalArrangement = Arrangement.spacedBy(WindowGap),
    ) {
        Text(
            text = title,
            color = palette.ink,
            // Cut at a syllable, never in the middle of one, when a word is wider than the window.
            style = BloxTheme.typography.title.merge(MaterialTheme.typography.titleSmall).copy(hyphens = Hyphens.Auto),
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** « L'enregistrement a échoué. », in `danger`, read by TalkBack as it shows: the window stays open to try again. */
@Composable
private fun WriteFailed() {
    Text(
        text = stringResource(R.string.feature_composer_window_failed),
        color = BloxTheme.palette.danger,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** A sentence of a window. */
@Composable
private fun WindowText(text: String) {
    Text(text = text, color = BloxTheme.palette.ink, style = MaterialTheme.typography.bodyMedium)
}

/** What the window says of a name that cannot be taken. */
@get:StringRes
private val SaveProblem.text: Int
    get() = when (this) {
        SaveProblem.EMPTY_GRID -> R.string.feature_composer_problem_empty
        SaveProblem.NAME_TAKEN -> R.string.feature_composer_problem_taken
        SaveProblem.BLANK_NAME -> R.string.feature_composer_problem_blank
    }

/** Between the window and the edges of the screen, inside the window, and between its parts. */
internal val WindowMargin = 16.dp
private val WindowPadding = 12.dp
private val WindowGap = 12.dp
