package ch.magori.blox.feature.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.SavedMusic
import ch.magori.blox.core.designsystem.component.DeleteWindow
import ch.magori.blox.core.designsystem.component.NamingWindow
import ch.magori.blox.core.designsystem.component.WindowButton
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.component.WindowPanel
import ch.magori.blox.core.designsystem.component.WindowText
import ch.magori.blox.core.designsystem.component.nameTooLongText
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.music.Category
import ch.magori.blox.feature.composer.model.SaveProblem
import ch.magori.blox.feature.composer.ui.ChoiceButton
import ch.magori.blox.feature.composer.ui.ComposerSizes

/** What the windows of the editor do, each answer to its callback; no logic here. */
@Immutable
internal class DialogActions(
    val onNameChanged: (String) -> Unit,
    val onFreeNameTaken: () -> Unit,
    val onForGamesChanged: (Boolean) -> Unit,
    val onStyleChosen: (Category?) -> Unit,
    val onSaveConfirmed: () -> Unit,
    val onRenameConfirmed: () -> Unit,
    val onDismissed: () -> Unit,
    val onLeaveWithoutSaving: () -> Unit,
    val onSaveBeforeLeaving: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
) {
    companion object {
        /** Windows that do nothing: previews and screenshots. */
        val None = DialogActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
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
    WindowDialog(onDismissRequest = actions.onDismissed) {
        ComposerWindow(dialog = dialog, name = name, actions = actions)
    }
}

/** The panel of the window [dialog]: apart from its dialog, so that the screenshots show it. */
@Composable
internal fun ComposerWindow(
    dialog: ComposerDialog,
    name: String?,
    actions: DialogActions,
    modifier: Modifier = Modifier,
) {
    when (dialog) {
        is ComposerDialog.Save -> ComposerNamingWindow(
            title = stringResource(R.string.feature_composer_save_title),
            confirm = stringResource(R.string.feature_composer_save),
            dialog = dialog,
            onConfirm = actions.onSaveConfirmed,
            actions = actions,
            modifier = modifier,
        ) {
            StyleChoice(style = dialog.style, onStyleChosen = actions.onStyleChosen)
            ForGamesBox(checked = dialog.forGames, onChecked = actions.onForGamesChanged)
        }

        is ComposerDialog.Rename -> ComposerNamingWindow(
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
            WindowButton(
                text = stringResource(R.string.feature_composer_save),
                onClick = actions.onSaveBeforeLeaving,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )
            WindowButton(
                text = stringResource(R.string.feature_composer_leave_without_saving),
                onClick = actions.onLeaveWithoutSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            WindowButton(
                text = stringResource(R.string.feature_composer_continue),
                onClick = actions.onDismissed,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is ComposerDialog.Delete -> DeleteWindow(
            name = name.orEmpty(),
            keep = stringResource(R.string.feature_composer_keep),
            onConfirm = actions.onDeleteConfirmed,
            onDismiss = actions.onDismissed,
            note = if (dialog.isGameMusic) stringResource(R.string.feature_composer_delete_game_music) else null,
            failed = dialog.failed,
            modifier = modifier,
        )
    }
}

/**
 * A window where a name is typed ([dialog]), then [extra] content, and its answers: [confirm], disabled while the name
 * has a problem, which the window says, offering a free name when the name is taken.
 */
@Composable
private fun ComposerNamingWindow(
    title: String,
    confirm: String,
    dialog: ComposerDialog.Naming,
    onConfirm: () -> Unit,
    actions: DialogActions,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    NamingWindow(
        title = title,
        name = dialog.name,
        onNameChanged = actions.onNameChanged,
        problem = dialog.problem?.let { problemText(it) },
        confirm = confirm,
        onConfirm = onConfirm,
        onDismiss = actions.onDismissed,
        failed = dialog.failed,
        freeName = dialog.freeName?.let { stringResource(R.string.feature_composer_take_free_name, it) },
        onFreeNameTaken = actions.onFreeNameTaken,
        modifier = modifier,
        extra = extra,
    )
}

/**
 * *Style*, in the save window: the seven categories and Libre (null), read by TalkBack as radio buttons, the one chosen,
 * [style], filled. They go to the line when they do not fit, as with a large font, each 48 dp high at least.
 */
@Composable
private fun StyleChoice(
    style: Category?,
    onStyleChosen: (Category?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(StyleTitleGap)) {
        Text(
            text = stringResource(R.string.feature_composer_style),
            color = BloxTheme.palette.ink,
            style = MaterialTheme.typography.labelSmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
            verticalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
            modifier = Modifier.selectableGroup(),
        ) {
            Styles.forEach { choice ->
                ChoiceButton(
                    text = stringResource(choice?.label ?: R.string.feature_composer_style_free),
                    selected = choice == style,
                    onClick = { onStyleChosen(choice) },
                    role = Role.RadioButton,
                    labelPadding = StyleLabelPadding,
                )
            }
        }
    }
}

/** The styles of a saved music, in the order of the specification: the seven categories, then Libre. */
private val Styles: List<Category?> = Category.entries + null

/** Between the title *Style* and its choices. */
private val StyleTitleGap = 6.dp

/** The room on each side of the label of a style. */
private val StyleLabelPadding = 6.dp

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

/** What the window says of a music that cannot be saved, or of a name that cannot be taken. */
@Composable
@ReadOnlyComposable
private fun problemText(problem: SaveProblem): String = when (problem) {
    SaveProblem.EmptyGrid -> stringResource(R.string.feature_composer_problem_empty)

    is SaveProblem.Name -> when (problem.reason) {
        NameProblem.BLANK -> stringResource(R.string.feature_composer_problem_blank)

        NameProblem.TAKEN -> stringResource(R.string.feature_composer_problem_taken)

        // Never shown: the editor cuts the name typed at the longest a saved name may be.
        NameProblem.TOO_LONG -> nameTooLongText(SavedMusic.MAX_NAME_LENGTH)
    }
}
