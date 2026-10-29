package ch.magori.blox.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.garden.GardenThumbnail
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.designsystem.component.BloxSheet
import ch.magori.blox.core.designsystem.component.CheckMark
import ch.magori.blox.core.designsystem.component.NamingWindow
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.component.WindowDialog
import ch.magori.blox.core.designsystem.component.messageTimeoutMillis
import ch.magori.blox.core.designsystem.component.nameTooLongText
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme

/** The frame a sheet is drawn in, given its title and its content: a sheet, or a sheet settled in the previews. */
internal typealias GardensSheetFrame = @Composable (title: String, content: @Composable ColumnScope.() -> Unit) -> Unit

/**
 * The sheet « Mes jardins » over the garden (docs/jardin-musical.md, Mes jardins), while [GardenUiState.sheet] is open,
 * and the window of the name over it while one is typed; the system back and its scrim close it.
 */
@Composable
internal fun MyGardensSheet(
    state: GardenUiState,
    actions: GardenActions,
) {
    val messageMillis = messageTimeoutMillis(hasAction = false)
    MyGardensSheetContent(state, actions, messageMillis) { title, content ->
        BloxSheet(title = title, onDismissRequest = actions.onCloseMyGardens, content = content)
    }
    state.sheet?.naming?.let { naming ->
        WindowDialog(onDismissRequest = actions.onCancelNaming) {
            GardenNamingWindow(
                naming,
                copy = state.linked != null,
                actions,
                messageMillis,
                busy = state.sheet.writing,
            )
        }
    }
}

/**
 * What the sheet « Mes jardins » holds, drawn in [frame]: at the top, « Garder ce jardin » for a garden that comes from
 * no kept garden, else « Enregistrer « X » » and « Garder une copie », greyed while they cannot be done or while a change is written, with why (« Rien n'a changé… » for a garden as it was kept);
 * « L'opération a échoué. » when a change could not be written; then, once the gardens are read, their count out of
 * 30, and their list from the most recent, or what to do when there is none. While they are read, the buttons alone,
 * greyed. A message of the screen shows for [messageMillis] once a garden is kept.
 */
@Composable
internal fun MyGardensSheetContent(
    state: GardenUiState,
    actions: GardenActions,
    messageMillis: Long,
    frame: GardensSheetFrame,
) {
    frame(stringResource(R.string.feature_garden_my_gardens)) {
        Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space3)) {
            KeepButtons(state, actions, messageMillis)
            if (state.sheet?.failed == true) SheetText(stringResource(R.string.feature_garden_failed), failure = true)
            state.keepRefusal?.let { refusal -> refusalText(refusal)?.let { SheetText(it) } }
            if (state.unchanged) SheetText(stringResource(R.string.feature_garden_unchanged))
            val gardens = state.myGardens
            if (gardens is MyGardens.Listed) KeptGardens(state.keptGardens, open = state.linked)
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
 * The count of the kept [gardens] out of 30, then their rows, the [open] one marked; or, without any, « Aucun jardin
 * gardé. » and what to do.
 */
@Composable
private fun KeptGardens(
    gardens: List<KeptGarden>,
    open: KeptGarden?,
) {
    Text(
        text = pluralStringResource(
            R.plurals.feature_garden_count,
            gardens.size,
            gardens.size,
            GardenLibrary.MAX_GARDENS,
        ),
        color = BloxTheme.palette.label,
        style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small),
    )
    if (gardens.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1)) {
            Text(
                text = stringResource(R.string.feature_garden_none_kept),
                color = BloxTheme.palette.ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            SheetText(stringResource(R.string.feature_garden_none_kept_hint))
        }
    } else {
        Column {
            gardens.forEach { KeptGardenRow(it, open = it.id == open?.id) }
        }
    }
}

/**
 * The row of a [kept] garden, 72 dp high at least: its thumbnail, framed in `panel-border` as the grid is, its name on
 * 2 lines at most, its mood and its count of pieces, then « ouvert » when it is the garden [open]. Read in one by
 * TalkBack, « Pluie douce, Calme, 12 pièces, ouvert »; the thumbnail is decorative.
 */
@Composable
private fun KeptGardenRow(
    kept: KeptGarden,
    open: Boolean,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_HEIGHT)
            .clearAndSetSemantics { contentDescription = spoken },
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
    val palette = BloxTheme.palette
    Row(
        modifier = Modifier
            .background(palette.accent, BloxTheme.shapes.pill)
            .padding(horizontal = BloxSpaces.space2, vertical = BloxSpaces.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
    ) {
        CheckMark(color = palette.accentInk, modifier = Modifier.size(CHECK))
        Text(
            text = stringResource(R.string.feature_garden_open),
            color = palette.accentInk,
            style = MaterialTheme.typography.labelMedium.merge(BloxTheme.typography.small),
        )
    }
}

/**
 * The window of the name of a garden to keep, « Garder ce jardin », or « Garder une copie » for a [copy]: its [naming],
 * why the name is refused under the field, « Annuler » then « Garder », disabled while the garden is being written,
 * [busy].
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
    NamingWindow(
        title = stringResource(if (copy) R.string.feature_garden_keep_copy else R.string.feature_garden_keep),
        name = naming.name,
        onNameChanged = actions.onNameChanged,
        maxLength = KeptGarden.MAX_NAME_LENGTH,
        problem = naming.problem?.let { problemText(it) },
        confirm = stringResource(R.string.feature_garden_keep_confirm),
        onConfirm = { actions.onConfirmKeeping(messageMillis) },
        onDismiss = actions.onCancelNaming,
        failed = naming.failed,
        busy = busy,
        modifier = modifier,
    )
}

/** Why a garden cannot take a name, under the field. */
@Composable
@ReadOnlyComposable
private fun problemText(problem: NameProblem): String = when (problem) {
    NameProblem.BLANK -> stringResource(R.string.feature_garden_name_blank)

    NameProblem.TAKEN -> stringResource(R.string.feature_garden_name_taken)

    // Never shown: the view model cuts the name typed at the longest a name may be.
    NameProblem.TOO_LONG -> nameTooLongText(KeptGarden.MAX_NAME_LENGTH)
}

/** A row of the list, at least (docs/jardin-musical.md, Mes jardins). */
private val ROW_HEIGHT = 72.dp

/** The frame of a thumbnail, and the room it takes on each side. */
private val THUMBNAIL_FRAME = 1.dp

/** The check of « ouvert ». */
private val CHECK = 12.dp
