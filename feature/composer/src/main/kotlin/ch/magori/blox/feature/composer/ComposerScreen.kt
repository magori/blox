package ch.magori.blox.feature.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.music.Note
import ch.magori.blox.core.music.Pitch
import ch.magori.blox.feature.composer.grid.GridLines
import ch.magori.blox.feature.composer.grid.NoteGrid
import ch.magori.blox.feature.composer.grid.gridRows
import ch.magori.blox.feature.composer.model.EditMode
import ch.magori.blox.feature.composer.model.EditorState
import ch.magori.blox.feature.composer.model.ScoreDraft
import ch.magori.blox.feature.composer.model.Track
import ch.magori.blox.feature.composer.model.TrackGrid
import ch.magori.blox.feature.composer.ui.ComposerSizes

/** The music editor (V8, V11) of [viewModel]; [onBack] leaves it. */
@Composable
internal fun ComposerScreen(
    viewModel: ComposerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ComposerContent(
        uiState = uiState,
        onBack = onBack,
        onOtherTrackShown = viewModel::onOtherTrackShown,
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
        modifier = modifier,
    )
}

/**
 * The editor as [uiState] shows it, from top to bottom: the header, the tabs, the toolbar, the grid of one bar, the bar
 * buttons and the tempo. No logic here: every touch goes to its callback. *Écouter*, *Enregistrer*, the dice and the
 * categories come with the next slices of the editor.
 */
@Composable
internal fun ComposerContent(
    uiState: ComposerUiState,
    onBack: () -> Unit,
    onOtherTrackShown: (Boolean) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val editor = uiState.editor
    val lines = remember(editor, uiState.showOtherTrack) { GridLines(gridRows(editor, uiState.showOtherTrack)) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .padding(ComposerSizes.padding),
        verticalArrangement = Arrangement.spacedBy(ComposerSizes.gap),
    ) {
        ComposerHeader(
            status = editor.status,
            showOtherTrack = uiState.showOtherTrack,
            onBack = onBack,
            onOtherTrackShown = onOtherTrackShown,
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
        NoteGrid(
            lines = lines,
            track = editor.track,
            empty = uiState.isTrackEmpty,
            onTouch = onCellTouched,
            onSlide = onCellSlid,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        BarButtons(bar = editor.bar, onBarChosen = onBarChosen)
        TempoPanel(
            bpm = editor.draft.tempo.bpm,
            canSlowDown = uiState.canSlowDown,
            canSpeedUp = uiState.canSpeedUp,
            onSlower = onSlower,
            onFaster = onFaster,
        )
    }
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
) {
    BloxTheme(kind = kind) {
        ComposerContent(
            uiState = ComposerUiState(editor, showOtherTrack = true),
            onBack = {},
            onOtherTrackShown = {},
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
        )
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerOriginalPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, previewEditor())
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerColorfulPreview() {
    ComposerPreview(BloxThemeKind.COLORFUL, previewEditor())
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun ComposerEmptyPreview() {
    ComposerPreview(BloxThemeKind.ORIGINAL, EditorState.new())
}
