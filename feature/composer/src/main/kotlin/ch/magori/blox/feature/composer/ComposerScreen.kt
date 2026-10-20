package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Category
import ch.magori.blox.core.music.CategoryChoice
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.core.music.Surprise
import ch.magori.blox.feature.composer.grid.GridLines
import ch.magori.blox.feature.composer.grid.NoteGrid
import ch.magori.blox.feature.composer.grid.gridRows
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.SaveProblem
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.ui.ComposerSizes
import ch.magori.blox.feature.composer.ui.ToolButton

/**
 * The music editor (V8, V11) of [viewModel]; [onBack] leaves it, and [onDeleted] once its music is deleted, with what
 * was deleted. While the music is listened to, the screen hands each frame to the view model, which moves the play head
 * on (ADR 0012), and its lifecycle events, which stop the music once the editor is no longer visible. Retour, the
 * button or the gesture of the system, asks the view model, which leaves at once or asks first.
 */
@Composable
internal fun ComposerScreen(
    viewModel: ComposerViewModel,
    onBack: () -> Unit,
    onDeleted: (DeletedMusic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val opened by viewModel.uiState.collectAsStateWithLifecycle()
    val listening = opened?.played != null
    LaunchedEffect(viewModel, listening) {
        if (listening) {
            while (true) {
                withFrameNanos(viewModel::onFrame)
            }
        }
    }
    // The view model decides what each event of the screen means for the music listened to (tested there); this
    // observer, without a JVM test, only hands the events over.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event -> viewModel.onLifecycleEvent(event) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        // Until the music is read, Retour goes to the navigation: there is nothing to lose yet.
        isBackEnabled = opened != null && opened?.exit == null,
        onBackCompleted = { if (opened != null) viewModel.onBackAsked() },
    )
    val exit = opened?.exit
    LaunchedEffect(exit) {
        when (exit) {
            ComposerExit.Leave -> onBack()
            is ComposerExit.Deleted -> onDeleted(exit.music)
            null -> return@LaunchedEffect
        }
        viewModel.onExitDone()
    }
    // A saved music opens once the library is read: until then, the page alone.
    val uiState = opened ?: return Box(modifier = modifier.fillMaxSize().background(BloxTheme.palette.page))
    val base = stringResource(uiState.editor.draft.category?.label ?: R.string.feature_composer_name_base)
    ComposerContent(
        uiState = uiState,
        onBack = viewModel::onBackAsked,
        onRename = { viewModel.onRenameAsked(base) },
        onDelete = viewModel::onDeleteAsked,
        onOtherTrackShown = viewModel::onOtherTrackShown,
        onFollowPlayback = viewModel::onFollowPlayback,
        onTrackChosen = viewModel::onTrackChosen,
        onModeChosen = viewModel::onModeChosen,
        onUndo = viewModel::onUndo,
        onCopyBar = viewModel::onCopyBar,
        onClearBar = viewModel::onClearBar,
        onCellTouched = viewModel::onCellTouched,
        onCellSlid = viewModel::onCellSlid,
        onBarChosen = viewModel::onBarChosen,
        onSlower = viewModel::onSlower,
        onFaster = viewModel::onFaster,
        onListen = viewModel::onListen,
        onCategoryChosen = viewModel::onCategoryChosen,
        onDice = viewModel::onDice,
        onSave = { viewModel.onSaveAsked(base) },
        onMessageShown = viewModel::onMessageShown,
        dialogActions = remember(viewModel, base) {
            DialogActions(
                onNameChanged = viewModel::onNameChanged,
                onFreeNameTaken = viewModel::onFreeNameTaken,
                onForGamesChanged = viewModel::onForGamesChanged,
                onSaveConfirmed = viewModel::onSaveConfirmed,
                onRenameConfirmed = viewModel::onRenameConfirmed,
                onDismissed = viewModel::onDialogDismissed,
                onLeaveWithoutSaving = viewModel::onLeaveWithoutSaving,
                onSaveBeforeLeaving = { viewModel.onSaveBeforeLeaving(base) },
                onDeleteConfirmed = viewModel::onDeleteConfirmed,
            )
        },
        modifier = modifier,
    )
}

/**
 * The editor as [uiState] shows it, from top to bottom: the header, the tabs, the toolbar, the grid of one bar, the bar
 * buttons, *Au hasard* and its categories, the tempo, *Écouter* and *Enregistrer*; the message of the editor, if any, over
 * the bottom of the grid until [onMessageShown]; its window, if any, answered through [dialogActions]. No logic here:
 * every touch goes to its callback.
 */
@Composable
internal fun ComposerContent(
    uiState: ComposerUiState,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOtherTrackShown: (Boolean) -> Unit,
    onFollowPlayback: (Boolean) -> Unit,
    onTrackChosen: (Track) -> Unit,
    onModeChosen: (EditMode) -> Unit,
    onUndo: () -> Unit,
    onCopyBar: () -> Unit,
    onClearBar: () -> Unit,
    onCellTouched: (pitch: Pitch, eighth: Int) -> Unit,
    onCellSlid: (pitch: Pitch, fromEighth: Int, toEighth: Int) -> Unit,
    onBarChosen: (Int) -> Unit,
    onSlower: () -> Unit,
    onFaster: () -> Unit,
    onCategoryChosen: (CategoryChoice) -> Unit,
    onDice: () -> Unit,
    onListen: () -> Unit,
    onSave: () -> Unit,
    onMessageShown: () -> Unit,
    dialogActions: DialogActions,
    modifier: Modifier = Modifier,
) {
    val editor = uiState.editor
    val lines = remember(editor, uiState.showOtherTrack) { GridLines(gridRows(editor, uiState.showOtherTrack)) }
    val played = uiState.played
    Box(modifier = modifier.fillMaxSize().background(BloxTheme.palette.page).safeDrawingPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(ComposerSizes.padding),
            verticalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
        ) {
            ComposerHeader(
                name = uiState.name,
                category = editor.draft.category?.let { stringResource(it.label) },
                status = editor.status,
                canDelete = editor.saved != null,
                showOtherTrack = uiState.showOtherTrack,
                followPlayback = uiState.followPlayback,
                onBack = onBack,
                onRename = onRename,
                onDelete = onDelete,
                onOtherTrackShown = onOtherTrackShown,
                onFollowPlayback = onFollowPlayback,
            )
            TrackTabs(track = editor.track, onTrackChosen = onTrackChosen)
            ComposerToolbar(
                mode = editor.mode,
                canUndo = editor.canUndo,
                canCopyBar = editor.canCopyBar,
                canClearBar = editor.canClearBar,
                onModeChosen = onModeChosen,
                onUndo = onUndo,
                onCopyBar = onCopyBar,
                onClearBar = onClearBar,
            )
            // The message shows over the bottom of the grid, never over the bar buttons nor *Arrêter*.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                NoteGrid(
                    lines = lines,
                    track = editor.track,
                    empty = uiState.isTrackEmpty,
                    playedEighth = played?.takeIf { it.bar == editor.bar }?.eighth,
                    onTouch = onCellTouched,
                    onSlide = onCellSlid,
                    modifier = Modifier.fillMaxSize(),
                )
                ComposerMessageHost(
                    message = uiState.message,
                    onMessageShown = onMessageShown,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            BarButtons(bar = editor.bar, playedBar = played?.bar, onBarChosen = onBarChosen)
            DicePanel(choice = uiState.choice, onCategoryChosen = onCategoryChosen, onDice = onDice)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TempoPanel(
                    bpm = editor.draft.tempo.bpm,
                    canSlowDown = uiState.canSlowDown,
                    canSpeedUp = uiState.canSpeedUp,
                    onSlower = onSlower,
                    onFaster = onFaster,
                    modifier = Modifier.weight(1f),
                )
                ListenButton(listening = played != null, enabled = uiState.canListen, onClick = onListen)
            }
            // A line of its own: beside the tempo and *Écouter*, it leaves the tempo buttons no room on 360 dp.
            SaveButton(changed = editor.asksBeforeLeaving, onClick = onSave, modifier = Modifier.fillMaxWidth())
        }
    }
    ComposerDialogHost(dialog = uiState.dialog, name = uiState.name, actions = dialogActions)
}

/**
 * *Enregistrer*: filled in `accent` and preceded by a dot while the music has [changed] (changes that leaving would
 * lose), framed otherwise.
 */
@Composable
private fun SaveButton(
    changed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolButton(
        text = stringResource(if (changed) R.string.feature_composer_save_changed else R.string.feature_composer_save),
        description = if (changed) stringResource(R.string.feature_composer_save_changed_description) else null,
        onClick = onClick,
        primary = changed,
        modifier = modifier,
    )
}

/** The name of a category, and the base of the name proposed for a music of it (« Joyeux 1 »). */
@get:StringRes
internal val Category.label: Int
    get() = when (this) {
        Category.JOYFUL -> R.string.feature_composer_category_joyful
        Category.CALM -> R.string.feature_composer_category_calm
        Category.EPIC -> R.string.feature_composer_category_epic
        Category.MELANCHOLIC -> R.string.feature_composer_category_melancholic
        Category.MYSTERIOUS -> R.string.feature_composer_category_mysterious
        Category.RETRO -> R.string.feature_composer_category_retro
        Category.DANCE -> R.string.feature_composer_category_dance
    }

/** Shows [message], if any, for a while, in the ink of the page, then calls [onMessageShown]. */
@Composable
private fun ComposerMessageHost(
    message: ComposerMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = message?.let { stringResource(it.text) }
    // A preview, without time going by, shows the message as it stands.
    val host = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (text != null) {
            host.showSnackbar(text)
            onMessageShown()
        }
    }
    SnackbarHost(hostState = host, modifier = modifier) { data -> MessageBar(data.visuals.message) }
}

/** A message of the editor, [text] in the colour of the page on the ink. */
@Composable
private fun MessageBar(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = BloxTheme.palette
    Snackbar(
        containerColor = palette.ink,
        contentColor = palette.page,
        shape = BloxTheme.shapes.panel,
        modifier = modifier,
    ) {
        Text(text)
    }
}

/** The text of a message of the editor. */
@get:StringRes
private val ComposerMessage.text: Int
    get() = when (this) {
        ComposerMessage.FOLLOW_STOPPED -> R.string.feature_composer_follow_stopped
        ComposerMessage.WRITE_FAILED -> R.string.feature_composer_write_failed
        ComposerMessage.DICE_ROLLED -> R.string.feature_composer_dice_rolled
        ComposerMessage.BAR_COPIED -> R.string.feature_composer_bar_copied
        ComposerMessage.BAR_CLEARED -> R.string.feature_composer_bar_cleared
    }

/**
 * A music to show the editor with: on the melody, a long note, two sixteenth notes in a cell, a note out of the scale
 * and the top C; on the bass, notes shown in watermark.
 */
private fun previewEditor(): EditorState {
    val draft = ScoreDraft.new()
    fun note(
        name: String?,
        beats: Double,
    ) = Note(name?.let(Pitch::parse), beats)
    val melody = listOf(
        note("E5", 1.0),
        note(null, 0.5),
        note("G5", 0.25),
        note("G5", 0.25),
        note("F#5", 0.5),
        note("C6", 0.5),
        note("A4", 1.0),
    )
    val bass = listOf(note("C3", 2.0), note("G2", 2.0))
    return EditorState(
        draft.copy(
            melody = TrackGrid.playing(Track.MELODY, draft.scale, melody),
            bass = TrackGrid.playing(Track.BASS, draft.scale, bass),
        ),
    )
}

@Composable
private fun ComposerPreview(
    kind: BloxThemeKind,
    editor: EditorState,
    played: Cell? = null,
    choice: CategoryChoice = Surprise,
) {
    BloxTheme(kind = kind) {
        ComposerContent(
            uiState = ComposerUiState(
                editor,
                showOtherTrack = true,
                played = played,
                choice = choice,
            ),
            onBack = {},
            onRename = {},
            onDelete = {},
            onOtherTrackShown = {},
            onFollowPlayback = {},
            onTrackChosen = {},
            onModeChosen = {},
            onUndo = {},
            onCopyBar = {},
            onClearBar = {},
            onCellTouched = { _, _ -> },
            onCellSlid = { _, _, _ -> },
            onBarChosen = {},
            onSlower = {},
            onFaster = {},
            onCategoryChosen = {},
            onDice = {},
            onListen = {},
            onSave = {},
            onMessageShown = {},
            dialogActions = DialogActions.None,
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews. The two themes
// show the music listened to, its third cell played.

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerOriginalPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, previewEditor(), played = Cell(bar = 0, eighth = 2))
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerColorfulPreview() {
    ComposerPreview(BloxThemeKind.COLORFUL, previewEditor(), played = Cell(bar = 0, eighth = 2))
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerEmptyPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, EditorState.new())
}

/**
 * The editor once the dice rolled in Joyeux over a music, which *Annuler* brings back; its message shows apart
 * ([MessagePreview]), as the host of the messages shows it only once time goes by.
 */
@Composable
private fun DicePreview(kind: BloxThemeKind) {
    val rolled = previewEditor()
    ComposerPreview(
        kind,
        rolled.copy(draft = rolled.draft.copy(category = Category.JOYFUL), undoable = ScoreDraft.new()),
        choice = Category.JOYFUL,
    )
}

/** The message that the dice can be undone, at the bottom of the page, as it shows over the bottom of the grid. */
@Composable
private fun MessagePreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(ComposerSizes.padding),
        ) {
            MessageBar(stringResource(ComposerMessage.DICE_ROLLED.text))
        }
    }
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
internal fun MessageOriginalPreview() {
    MessagePreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 240)
@Composable
internal fun MessageColorfulPreview() {
    MessagePreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
internal fun MessageOriginalLargeFontPreview() {
    MessagePreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 400, fontScale = 2f)
@Composable
internal fun MessageColorfulLargeFontPreview() {
    MessagePreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerDiceOriginalPreview() {
    DicePreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerDiceColorfulPreview() {
    DicePreview(BloxThemeKind.COLORFUL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun ComposerDiceOriginalLargeFontPreview() {
    DicePreview(BloxThemeKind.ORIGINAL)
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun ComposerDiceColorfulLargeFontPreview() {
    DicePreview(BloxThemeKind.COLORFUL)
}

/**
 * The window [dialog] over a dimmed page, as it shows over the editor: the screenshots render its panel without the
 * window of the system.
 */
@Composable
private fun WindowPreview(
    kind: BloxThemeKind,
    dialog: ComposerDialog,
) {
    BloxTheme(kind = kind) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(horizontal = WindowMargin),
        ) {
            ComposerWindow(dialog = dialog, name = "Joyeux 1", actions = DialogActions.None)
        }
    }
}

/** The save window of a music whose name another music has: the problem, and the free name offered. */
private val TakenName = ComposerDialog.Save(
    name = "Joyeux 1",
    forGames = true,
    problem = SaveProblem.NAME_TAKEN,
    freeName = "Joyeux 2",
)

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun SaveWindowOriginalPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, TakenName)
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun SaveWindowColorfulPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, TakenName)
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun SaveWindowOriginalLargeFontPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, TakenName)
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun SaveWindowColorfulLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, TakenName)
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun LeaveWindowOriginalPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, ComposerDialog.Leave)
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
internal fun LeaveWindowColorfulPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, ComposerDialog.Leave)
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun LeaveWindowOriginalLargeFontPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, ComposerDialog.Leave)
}

@Preview(widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
internal fun LeaveWindowColorfulLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, ComposerDialog.Leave)
}
