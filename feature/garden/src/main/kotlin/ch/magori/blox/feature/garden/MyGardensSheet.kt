package ch.magori.blox.feature.garden

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.garden.GardenThumbnail
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.designsystem.component.BloxSheet
import ch.magori.blox.core.designsystem.component.DeleteWindow
import ch.magori.blox.core.designsystem.component.MessageAction
import ch.magori.blox.core.designsystem.component.NamingWindow
import ch.magori.blox.core.designsystem.component.PillBadge
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.RenameWindow
import ch.magori.blox.core.designsystem.component.RowMenu
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.component.SheetLeaving
import ch.magori.blox.core.designsystem.component.SnackMessage
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.component.messageTimeoutMillis
import ch.magori.blox.core.designsystem.frame.LeavingContent
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.text.NameRefusal
import ch.magori.blox.core.designsystem.text.NamedThing
import ch.magori.blox.core.designsystem.text.nameRefusalText
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import kotlin.math.roundToInt
import ch.magori.blox.core.designsystem.R as DesignSystemR

/**
 * The frame a sheet is drawn in, given its title, what stands at its bottom, always in sight, if anything, and its
 * content: a sheet, or a sheet settled in the previews.
 */
internal typealias GardensSheetFrame = @Composable (
    title: String,
    bottom: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) -> Unit

/**
 * The sheet « Mes jardins » over the garden (docs/jardin-musical.md, Mes jardins), while [GardenUiState.sheet] is open,
 * and over it the window of the name while one is typed, or the one of a deletion to confirm; the system back and its
 * scrim close it. Closed, it goes down as it is [leaving], as it stood, without its windows.
 */
@Composable
internal fun MyGardensSheet(
    state: GardenUiState,
    actions: GardenActions,
    leaving: SheetLeaving? = null,
) {
    val messageMillis = messageTimeoutMillis(hasAction = false)
    val undoMillis = undoTimeoutMillis()
    MyGardensSheetContent(state, actions, messageMillis, undoMillis) { title, bottom, content ->
        BloxSheet(
            title = title,
            onDismissRequest = actions.onCloseMyGardens,
            leaving = leaving,
            actions = bottom,
            content = content,
        )
    }
    val sheet = state.sheet?.takeIf { leaving == null } ?: return
    sheet.naming?.let { naming ->
        WindowDialog(onDismissRequest = actions.onCancelNaming) {
            GardenNamingWindow(naming, copy = state.linked != null, actions, messageMillis, busy = sheet.writing)
        }
    }
    sheet.deleting?.let { deleting ->
        WindowDialog(onDismissRequest = actions.onCancelDeleting) {
            GardenDeleteWindow(deleting, actions, undoMillis, busy = sheet.writing)
        }
    }
}

/**
 * What the sheet « Mes jardins » holds, drawn in [frame]: at the top, « Garder ce jardin » for a garden that comes from
 * no kept garden, else « Enregistrer « X » » and « Garder une copie », greyed while they cannot be done or while a
 * change is written, with why (« Rien n'a changé… » for a garden as it was kept); « L'opération a échoué. » when a
 * change could not be written; then, once the gardens are read, their count out of 30, and their list from the most
 * recent, or what to do when there is none. While they are read, the buttons alone, greyed. A message of the screen
 * shows for [messageMillis] once a garden is kept; « Annuler » is offered for [undoMillis] once one is opened or
 * deleted. While the deletion of a kept garden may be undone, its message stands at the bottom of the sheet.
 */
@Composable
internal fun MyGardensSheetContent(
    state: GardenUiState,
    actions: GardenActions,
    messageMillis: Long,
    undoMillis: Long,
    frame: GardensSheetFrame,
) {
    val deleted = state.undo as? GardenUndo.Deleted
    val bottom: (@Composable ColumnScope.() -> Unit)? = deleted?.let {
        {
            SnackMessage(
                text = undoText(it, state.undoFailed),
                action = MessageAction(stringResource(R.string.feature_garden_undo), actions.onUndo),
            )
        }
    }
    frame(stringResource(R.string.feature_garden_my_gardens), bottom) {
        Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3)) {
            KeepButtons(state, actions, messageMillis)
            if (state.sheet?.failed == true) SheetText(stringResource(R.string.feature_garden_failed), failure = true)
            state.keepRefusal?.let { refusal -> refusalText(refusal)?.let { SheetText(it) } }
            if (state.unchanged) SheetText(stringResource(R.string.feature_garden_unchanged))
            val gardens = state.myGardens
            if (gardens is MyGardens.Listed) {
                KeptGardens(
                    rows = state.sheetRows,
                    open = state.linked,
                    actions = actions,
                    undoMillis = undoMillis,
                    windowOpen = state.sheet?.naming != null || state.sheet?.deleting != null,
                )
            }
        }
    }
}

/**
 * « Garder ce jardin », or, for a garden that comes from a kept garden, « Enregistrer « X » » then « Garder une copie »;
 * each as wide as the sheet.
 */
@Composable
private fun KeepButtons(
    state: GardenUiState,
    actions: GardenActions,
    messageMillis: Long,
) {
    val moodName = stringResource(state.garden.mood.label)
    val linked = state.linked
    val canKeep = state.keepRefusal == null
    val writing = state.sheet?.writing == true
    if (linked == null) {
        PrimaryButton(
            text = stringResource(R.string.feature_garden_keep),
            icon = BloxIcon.PLUS,
            onClick = { actions.onKeep(moodName) },
            enabled = canKeep && !writing,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        PrimaryButton(
            text = stringResource(R.string.feature_garden_save_in_place, linked.name),
            icon = BloxIcon.CHECK,
            onClick = { actions.onSaveInPlace(messageMillis) },
            enabled = state.canSaveInPlace && !writing,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.feature_garden_keep_copy),
            icon = BloxIcon.COPY,
            onClick = { actions.onKeep(moodName) },
            enabled = canKeep && !writing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Why a garden cannot be kept, as the sheet writes it; nothing while the gardens are read. */
@Composable
@ReadOnlyComposable
private fun refusalText(refusal: KeepRefusal): String? = when (refusal) {
    KeepRefusal.LOADING -> null

    KeepRefusal.UNREADABLE -> stringResource(R.string.feature_garden_unreadable)

    KeepRefusal.EMPTY_GARDEN -> stringResource(R.string.feature_garden_empty_garden)

    KeepRefusal.FULL -> pluralStringResource(
        R.plurals.feature_garden_full,
        GardenLibrary.MAX_GARDENS,
        GardenLibrary.MAX_GARDENS,
    )
}

/** A sentence of the sheet, in `label`; a [failure] in `danger`, read by TalkBack as it shows. */
@Composable
private fun SheetText(
    text: String,
    failure: Boolean = false,
) {
    Text(
        text = text,
        color = if (failure) BloxTheme.palette.danger else BloxTheme.palette.label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = if (failure) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
    )
}

/**
 * The count of the kept gardens out of 30, then their [rows], the [open] one marked; or, without any, « Aucun jardin
 * gardé. » and what to do. A row touched opens its garden, « Annuler » offered for [undoMillis]. Once the window a row
 * opened closes ([windowOpen]), the focus goes back to its menu « … ». The row of a garden deleted fades out, then
 * folds up ([deletedRow]), unread by TalkBack; in reduced effects, it is gone at once.
 */
@Composable
private fun ColumnScope.KeptGardens(
    rows: List<SheetRow>,
    open: KeptGarden?,
    actions: GardenActions,
    undoMillis: Long,
    windowOpen: Boolean,
) {
    val listed = rows.count { it.listed }
    Text(
        text = pluralStringResource(
            R.plurals.feature_garden_count,
            listed,
            listed,
            GardenLibrary.MAX_GARDENS,
        ),
        color = BloxTheme.palette.label,
        style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small),
    )
    // One column, the texts of an empty list then the rows: the last garden deleted leaves where it stood.
    Column {
        if (listed == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
                Text(
                    text = stringResource(R.string.feature_garden_none_kept),
                    color = BloxTheme.palette.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                SheetText(stringResource(R.string.feature_garden_none_kept_hint))
            }
        }
        GardenRows(rows, open, actions, undoMillis, windowOpen)
    }
}

/** The [rows] of [KeptGardens], each as [KeptGardenRow] draws it, or leaving the list. */
@Composable
private fun GardenRows(
    rows: List<SheetRow>,
    open: KeptGarden?,
    actions: GardenActions,
    undoMillis: Long,
    windowOpen: Boolean,
) {
    val length = deletedRowTicks(BloxTheme.effects)
    Column {
        rows.forEach { row ->
            key(row.garden.id) {
                LeavingContent(open = row.garden.takeIf { row.listed }, length = length) { kept, leaving ->
                    KeptGardenRow(
                        kept = kept,
                        open = kept.id == open?.id,
                        onOpen = { actions.onOpenKept(kept.id, undoMillis) },
                        onRename = { actions.onRename(kept.id) },
                        onDelete = { actions.onDelete(kept.id) },
                        windowOpen = windowOpen,
                        modifier = if (leaving == null) Modifier else Modifier.leaving(leaving),
                    )
                }
            }
        }
    }
}

/**
 * A row leaving the list, [ticks] after its deletion, as [deletedRow] says, read in its drawing and its layout alone:
 * unread by TalkBack, as it is no longer listed.
 */
private fun Modifier.leaving(ticks: () -> Int): Modifier = this
    .clearAndSetSemantics {}
    .graphicsLayer {
        alpha = deletedRow(ticks()).alpha
        clip = true
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height * deletedRow(ticks()).height).roundToInt()) {
            placeable.placeRelative(0, 0)
        }
    }

/**
 * The row of a [kept] garden, 72 dp high at least: its thumbnail, framed in `panel-border` as the grid is, its name on
 * 2 lines at most, its mood and its count of pieces, then « ouvert » when it is the garden [open]; touched, it opens
 * the garden ([onOpen]). Then its menu « … »: Renommer ([onRename]), Supprimer ([onDelete]). Read in one by TalkBack,
 * « Pluie douce, Calme, 12 pièces, ouvert », with the action « Ouvrir », Renommer and Supprimer as actions of its own;
 * the thumbnail is decorative. Its menu takes the focus back once the window it opened closes ([windowOpen]). [modifier]
 * applies to the row.
 */
@Composable
private fun KeptGardenRow(
    kept: KeptGarden,
    open: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    windowOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
        KeptGardenSummary(kept, open, onOpen, onRename, onDelete, Modifier.weight(1f))
        RowMenu(name = kept.name, windowOpen = windowOpen) {
            RenameItem(onRename = onRename)
            DeleteItem(onDelete = onDelete)
        }
    }
}

/** What a row of [KeptGardenRow] shows of its [kept] garden, and what touching it does. */
@Composable
private fun KeptGardenSummary(
    kept: KeptGarden,
    open: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mood = stringResource(kept.garden.mood.label)
    val count = kept.garden.pieces.size
    val pieces = pluralStringResource(R.plurals.feature_garden_piece_count, count, count)
    val spoken = stringResource(
        if (open) R.string.feature_garden_row_open_spoken else R.string.feature_garden_row_spoken,
        kept.name,
        mood,
        pieces,
    )
    val palette = BloxTheme.palette
    val openLabel = stringResource(R.string.feature_garden_row_open)
    val renameLabel = stringResource(DesignSystemR.string.core_designsystem_rename)
    val deleteLabel = stringResource(DesignSystemR.string.core_designsystem_window_delete)
    Row(
        modifier = modifier
            .heightIn(min = ROW_HEIGHT)
            .clip(BloxTheme.shapes.md)
            .clickable(onClickLabel = openLabel, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                onClick(label = openLabel) {
                    onOpen()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(renameLabel) {
                        onRename()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space3),
    ) {
        // Its free cells are those of the grid: by day, `empty-cell` barely stands out from `panel`, and `board` not
        // at all; the frame of the grid, `panel-border`, gives the thumbnail its edges.
        GardenThumbnail(
            garden = kept.garden,
            modifier = Modifier
                .border(THUMBNAIL_FRAME, palette.panelBorder)
                .padding(THUMBNAIL_FRAME),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = kept.name,
                color = palette.ink,
                style = MaterialTheme.typography.labelLarge.merge(BloxTheme.typography.buttonLabel),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.feature_garden_row_info, mood, pieces),
                color = palette.label,
                style = MaterialTheme.typography.bodyMedium.merge(BloxTheme.typography.small),
            )
        }
        if (open) OpenBadge()
    }
}

/** « ouvert », a pill in `accent` beside its check: the garden under way comes from this one. */
@Composable
private fun OpenBadge() {
    PillBadge(text = stringResource(R.string.feature_garden_open), withCheck = true)
}

/**
 * The window of the name of a garden to keep, « Garder ce jardin », or « Garder une copie » for a [copy], with « Garder »;
 * or of a kept garden to rename, « Renommer « X » », with « Renommer ». Its [naming], why the name is refused under
 * the field, « Annuler » then the answer, disabled while the gardens are being written, [busy].
 */
@Composable
internal fun GardenNamingWindow(
    naming: GardenNaming,
    copy: Boolean,
    actions: GardenActions,
    messageMillis: Long,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    val renaming = naming.renaming
    val problem = naming.problem?.let {
        nameRefusalText(NameRefusal.valueOf(it.name), NamedThing.GARDEN, KeptGarden.MAX_NAME_LENGTH)
    }
    if (renaming != null) {
        RenameWindow(
            current = renaming.name,
            name = naming.name,
            onNameChanged = actions.onNameChanged,
            maxLength = KeptGarden.MAX_NAME_LENGTH,
            problem = problem,
            onConfirm = actions.onConfirmRenaming,
            onDismiss = actions.onCancelNaming,
            modifier = modifier,
            failed = naming.failed,
            busy = busy,
        )
    } else {
        NamingWindow(
            title = stringResource(if (copy) R.string.feature_garden_keep_copy else R.string.feature_garden_keep),
            name = naming.name,
            onNameChanged = actions.onNameChanged,
            maxLength = KeptGarden.MAX_NAME_LENGTH,
            problem = problem,
            confirm = stringResource(R.string.feature_garden_keep_confirm),
            onConfirm = { actions.onConfirmKeeping(messageMillis) },
            onDismiss = actions.onCancelNaming,
            failed = naming.failed,
            busy = busy,
            modifier = modifier,
        )
    }
}

/**
 * « Supprimer « X » ? », the deletion of the kept garden [deleting] to confirm: it may be undone just after.
 * « Garder le jardin », then « Supprimer », which offers « Annuler » for [undoMillis]. A deletion that could not be
 * written is told, the window still open. While it is written, [busy], both answers are greyed.
 */
@Composable
internal fun GardenDeleteWindow(
    deleting: GardenDeleting,
    actions: GardenActions,
    undoMillis: Long,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    DeleteWindow(
        name = deleting.garden.name,
        keep = stringResource(R.string.feature_garden_delete_keep),
        onConfirm = { actions.onConfirmDeleting(undoMillis) },
        onDismiss = actions.onCancelDeleting,
        modifier = modifier,
        note = stringResource(R.string.feature_garden_delete_note),
        failed = deleting.failed,
        waiting = busy,
    )
}

/** A row of the list, at least (docs/jardin-musical.md, Mes jardins). */
private val ROW_HEIGHT = 72.dp

/** The frame of a thumbnail, and the room it takes on each side. */
private val THUMBNAIL_FRAME = 1.dp
