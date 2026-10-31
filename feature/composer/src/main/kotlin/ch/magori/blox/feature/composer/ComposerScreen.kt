package ch.magori.blox.feature.composer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ch.magori.blox.core.data.DeletedMusic
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.data.keyName
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.ButtonSize
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.component.SettledSheet
import ch.magori.blox.core.designsystem.component.SingleLineText
import ch.magori.blox.core.designsystem.component.SnackMessage
import ch.magori.blox.core.designsystem.component.WindowOverPage
import ch.magori.blox.core.designsystem.component.messageTimeoutMillis
import ch.magori.blox.core.designsystem.lifecycle.ScreenHiddenEffect
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
import kotlinx.coroutines.delay

/**
 * The music editor (V8, V33) of [viewModel]; [onBack] leaves it, and [onDeleted] once its music is deleted, with what
 * was deleted. While the music is listened to, the screen hands each frame to the view model, which moves the play head
 * on (ADR 0012); the music stops once the editor is no longer visible. Retour, the button or the gesture of the system,
 * asks the view model, which leaves at once or asks first.
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
    ScreenHiddenEffect(onHidden = viewModel::onHidden)
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
    val uiState = opened
    if (uiState == null) {
        Box(modifier = modifier.fillMaxSize().background(BloxTheme.palette.page))
        return
    }
    ComposerContent(
        uiState = uiState,
        onBack = viewModel::onBackAsked,
        onRename = viewModel::onRenameAsked,
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
        onSave = viewModel::onSaveAsked,
        onMessageShown = viewModel::onMessageShown,
        dialogActions = remember(viewModel) {
            DialogActions(
                onNameChanged = viewModel::onNameChanged,
                onFreeNameTaken = viewModel::onFreeNameTaken,
                onForGamesChanged = viewModel::onForGamesChanged,
                onStyleChosen = viewModel::onStyleChosen,
                onSaveConfirmed = viewModel::onSaveConfirmed,
                onRenameConfirmed = viewModel::onRenameConfirmed,
                onDismissed = viewModel::onDialogDismissed,
                onLeaveWithoutSaving = viewModel::onLeaveWithoutSaving,
                onSaveBeforeLeaving = viewModel::onSaveBeforeLeaving,
                onDeleteConfirmed = viewModel::onDeleteConfirmed,
            )
        },
        modifier = modifier,
    )
}

/**
 * The editor as [uiState] shows it (V33), from top to bottom ([GridBetween]): the header, the tracks, the modes and
 * *Annuler*, the grid of one bar, the bars with *Copier* and *Effacer*, the tempo with *Écouter*, and *Enregistrer*;
 * the message of the editor, if any, over the bottom of the grid until
 * [onMessageShown]; its window, if any, answered through [dialogActions]; the sheet « Au hasard » once the dice of the
 * header is touched, open until a music is rolled ([onDice]) or it is closed. Whether the sheet is open is the screen's
 * (ADR 0029): it has no rule. No other logic here: every touch goes to its callback.
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
    var diceOpen by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(BloxTheme.palette.page).safeDrawingPadding()) {
        GridBetween(
            modifier = Modifier.fillMaxSize(),
            above = {
                ComposerHeader(
                    name = uiState.name,
                    style = stringResource(editor.draft.category?.label ?: R.string.feature_composer_style_free),
                    key = editor.draft.scale.keyName(),
                    status = editor.status,
                    canDelete = editor.saved != null,
                    showOtherTrack = uiState.showOtherTrack,
                    followPlayback = uiState.followPlayback,
                    windowOpen = uiState.dialog != null,
                    onBack = onBack,
                    onRename = onRename,
                    onDice = { diceOpen = true },
                    onDelete = onDelete,
                    onOtherTrackShown = onOtherTrackShown,
                    onFollowPlayback = onFollowPlayback,
                )
                TrackTabs(track = editor.track, onTrackChosen = onTrackChosen)
                ModesAndUndo(mode = editor.mode, canUndo = editor.canUndo, onModeChosen = onModeChosen, onUndo = onUndo)
            },
            grid = {
                // The message shows over the bottom of the grid, never over the bar buttons nor *Arrêter*.
                Box {
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
            },
            below = {
                BarsAndTools(
                    bar = editor.bar,
                    playedBar = played?.bar,
                    canCopyBar = editor.canCopyBar,
                    canClearBar = editor.canClearBar,
                    onBarChosen = onBarChosen,
                    onCopyBar = onCopyBar,
                    onClearBar = onClearBar,
                )
                // Écouter at the end of the line of the tempo, or under it when the tempo needs the whole line.
                FixedBesideRest(fixedFirst = false, restMin = 0.dp) {
                    TempoPanel(
                        bpm = editor.draft.tempo.bpm,
                        canSlowDown = uiState.canSlowDown,
                        canSpeedUp = uiState.canSpeedUp,
                        onSlower = onSlower,
                        onFaster = onFaster,
                    )
                    ListenButton(
                        playing = played != null,
                        musicName = uiState.name,
                        onClick = onListen,
                        enabled = uiState.canListen,
                        size = ListenSize,
                    )
                }
                // A line of its own: beside the tempo and *Écouter*, it leaves the tempo buttons no room on 360 dp.
                SaveButton(changed = editor.asksBeforeLeaving, onClick = onSave, modifier = Modifier.fillMaxWidth())
            },
        )
    }
    ComposerDialogHost(dialog = uiState.dialog, name = uiState.name, actions = dialogActions)
    if (diceOpen) {
        DiceSheet(
            choice = uiState.choice,
            onCategoryChosen = onCategoryChosen,
            onRoll = {
                diceOpen = false
                onDice()
            },
            onDismissRequest = { diceOpen = false },
        )
    }
}

/**
 * *Enregistrer*, the one main button of the editor, always in `accent`, compact; preceded by a dot, and read
 * « Enregistrer les changements », while the music has [changed] (changes that leaving would lose).
 */
@Composable
private fun SaveButton(
    changed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.feature_composer_save_changed_description)
    BubbleButton(
        family = ButtonFamily.PRIMARY,
        onClick = onClick,
        modifier = if (changed) modifier.semantics { contentDescription = description } else modifier,
        size = ButtonSize.COMPACT,
    ) {
        if (changed) {
            val ink = LocalContentColor.current
            Spacer(
                modifier = Modifier
                    .testTag(SAVE_DOT_TAG)
                    .size(SaveDotSize)
                    .drawBehind { drawCircle(ink) },
            )
        }
        // On one line, shrunk if need be: in Original twice as large, « Enregistrer » beside its dot would be cut.
        SingleLineText(
            text = stringResource(R.string.feature_composer_save),
            style = LocalTextStyle.current,
            color = LocalContentColor.current,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** The dot before *Enregistrer* while the music has changed. */
private val SaveDotSize = 8.dp

/** The tag of the dot before *Enregistrer*, read by the tests. */
internal const val SAVE_DOT_TAG = "composer_save_dot"

/** Écouter, larger in the editor than in a list: the main way to hear what is written. */
private val ListenSize = 56.dp

/** The name of a category. */
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

/**
 * Shows [message], if any, as a [SnackMessage], for as long as a message stays ([messageTimeoutMillis]), then calls
 * [onMessageShown]; a message the editor takes away before (its undo gone) goes at once.
 */
@Composable
private fun ComposerMessageHost(
    message: ComposerMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (message == null) return
    val timeout = messageTimeoutMillis(hasAction = false)
    val shown by rememberUpdatedState(onMessageShown)
    LaunchedEffect(message) {
        delay(timeout)
        shown()
    }
    SnackMessage(text = stringResource(message.text), modifier = modifier)
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
    over: @Composable () -> Unit = {},
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
        over()
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

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun ComposerColorfulLargeFontPreview() {
    ComposerPreview(BloxThemeKind.COLORFUL, previewEditor(), played = Cell(bar = 0, eighth = 2))
}

/**
 * Original, figé, stays usable in a font twice as large: on a phone, the page scrolls rather than squash its parts; a
 * page as high as all of them shows each, down to the tempo, Écouter and Enregistrer.
 */
@Preview(widthDp = 360, heightDp = 1300, fontScale = 2f)
@Composable
internal fun ComposerOriginalLargeFontPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, previewEditor(), played = Cell(bar = 0, eighth = 2))
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerEmptyPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, EditorState.new())
}

/** The sheet « Au hasard » open over the editor, Joyeux chosen: the screenshots render it without its window. */
@Composable
private fun DiceSheetPreview() {
    ComposerPreview(BloxThemeKind.COLORFUL, previewEditor(), choice = Category.JOYFUL) {
        DiceSheetContent(choice = Category.JOYFUL, onCategoryChosen = {}, onRoll = {}) { title, actions, content ->
            SettledSheet(title = title, actions = actions, content = content)
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun DiceSheetColorfulPreview() = DiceSheetPreview()

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun DiceSheetColorfulLargeFontPreview() = DiceSheetPreview()

/** The message that the dice can be undone, at the bottom of the page, as it shows over the bottom of the grid. */
@Composable
private fun MessagePreview(kind: BloxThemeKind) {
    BloxTheme(kind = kind) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize().background(BloxTheme.palette.page).padding(ComposerSizes.padding),
        ) {
            SnackMessage(stringResource(ComposerMessage.DICE_ROLLED.text))
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
        WindowOverPage { ComposerWindow(dialog = dialog, name = "Joyeux 1", actions = DialogActions.None) }
    }
}

/** The save window of a music in Joyeux whose name another music has: the problem, the free name offered, the style. */
private val TakenName = ComposerDialog.Save(
    name = "Matin de fête en sol",
    forGames = true,
    style = Category.JOYFUL,
    problem = SaveProblem.Name(NameProblem.TAKEN),
    freeName = "Matin de fête en sol 2",
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

@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
internal fun SaveWindowOriginalLargeFontPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, TakenName)
}

@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
internal fun SaveWindowColorfulLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, TakenName)
}

/** The save window of a music in Épique without problem, its name of 40 characters, the longest one. */
private val LongestName = ComposerDialog.Save(
    name = "Marche des géants en fa dièse mixolydien",
    forGames = false,
    style = Category.EPIC,
)

@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
internal fun SaveWindowLongestNameOriginalLargeFontPreview() {
    WindowPreview(BloxThemeKind.ORIGINAL, LongestName)
}

@Preview(widthDp = 360, heightDp = 1400, fontScale = 2f)
@Composable
internal fun SaveWindowLongestNameColorfulLargeFontPreview() {
    WindowPreview(BloxThemeKind.COLORFUL, LongestName)
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
