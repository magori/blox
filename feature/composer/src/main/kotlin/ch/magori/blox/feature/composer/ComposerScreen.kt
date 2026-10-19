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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.grid.GridLines
import ch.magori.blox.feature.composer.grid.NoteGrid
import ch.magori.blox.feature.composer.grid.gridRows
import ch.magori.blox.feature.composer.model.Cell
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.ui.ComposerSizes

/**
 * The music editor (V8, V11) of [viewModel]; [onBack] leaves it. While the music is listened to, the screen hands each
 * frame to the view model, which moves the play head on (ADR 0012), and its lifecycle events, which stop the music once
 * the editor is no longer visible.
 */
@Composable
internal fun ComposerScreen(
    viewModel: ComposerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listening = uiState.played != null
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
    ComposerContent(
        uiState = uiState,
        onBack = onBack,
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
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}

/**
 * The editor as [uiState] shows it, from top to bottom: the header, the tabs, the toolbar, the grid of one bar, the bar
 * buttons, the tempo and *Écouter*; the message of the editor, if any, over the bottom of the grid until
 * [onMessageShown]. No logic here: every touch goes to its callback. *Enregistrer*, the dice and the categories come
 * with the next slices of the editor.
 */
@Composable
internal fun ComposerContent(
    uiState: ComposerUiState,
    onBack: () -> Unit,
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
    onListen: () -> Unit,
    onMessageShown: () -> Unit,
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
                status = editor.status,
                showOtherTrack = uiState.showOtherTrack,
                followPlayback = uiState.followPlayback,
                onBack = onBack,
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
        }
    }
}

/** Shows [message], if any, for a while, in the ink of the page, then calls [onMessageShown]. */
@Composable
private fun ComposerMessageHost(
    message: ComposerMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember { SnackbarHostState() }
    val text = message?.let { stringResource(it.text) }
    LaunchedEffect(message) {
        if (text != null) {
            host.showSnackbar(text)
            onMessageShown()
        }
    }
    val palette = BloxTheme.palette
    SnackbarHost(hostState = host, modifier = modifier) { data ->
        Snackbar(containerColor = palette.ink, contentColor = palette.page, shape = BloxTheme.shapes.panel) {
            Text(data.visuals.message)
        }
    }
}

/** The text of a message of the editor. */
@get:StringRes
private val ComposerMessage.text: Int
    get() = when (this) {
        ComposerMessage.FOLLOW_STOPPED -> R.string.feature_composer_follow_stopped
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
) {
    BloxTheme(kind = kind) {
        ComposerContent(
            uiState = ComposerUiState(editor, showOtherTrack = true, played = played),
            onBack = {},
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
            onListen = {},
            onMessageShown = {},
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
