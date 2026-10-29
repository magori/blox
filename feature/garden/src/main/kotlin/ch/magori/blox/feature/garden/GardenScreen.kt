package ch.magori.blox.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.dropUnlessResumed
import ch.magori.blox.core.board.garden.GardenCell
import ch.magori.blox.core.board.garden.GardenCellCanvas
import ch.magori.blox.core.board.garden.GardenCellEffects
import ch.magori.blox.core.board.garden.GardenPlayHead
import ch.magori.blox.core.board.garden.isInGardenBand
import ch.magori.blox.core.board.piece.pieceCells
import ch.magori.blox.core.board.tile.PieceTile
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.designsystem.component.BubbleButton
import ch.magori.blox.core.designsystem.component.ButtonFamily
import ch.magori.blox.core.designsystem.component.ChoiceItem
import ch.magori.blox.core.designsystem.component.ChoiceSegments
import ch.magori.blox.core.designsystem.component.ChordPill
import ch.magori.blox.core.designsystem.component.ClosingSheet
import ch.magori.blox.core.designsystem.component.GlyphButton
import ch.magori.blox.core.designsystem.component.Glyphs
import ch.magori.blox.core.designsystem.component.ListenButton
import ch.magori.blox.core.designsystem.component.MessageAction
import ch.magori.blox.core.designsystem.component.PrimaryButton
import ch.magori.blox.core.designsystem.component.SecondaryButton
import ch.magori.blox.core.designsystem.component.SnackMessage
import ch.magori.blox.core.designsystem.frame.LeavingContent
import ch.magori.blox.core.designsystem.icon.BloxIcon
import ch.magori.blox.core.designsystem.theme.BloxBorders
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxSpaces
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.ColorfulTheme
import ch.magori.blox.core.designsystem.theme.shrinkToWholeWords
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood

/**
 * The tag of Changer de jeu, the same in every language, read by the journey of `:benchmark`, which copies it as it is:
 * it does not see this module.
 */
internal const val GARDEN_CHANGE_GAME_TAG = "garden_change_game"

/** The tag of the dot of « Mes jardins », decorative, read by the tests only. */
internal const val GARDEN_MODIFIED_DOT_TAG = "garden_modified_dot"

/**
 * What the player does on the musical garden, handed to the view model and to `app` by the entry. Tout effacer, Au
 * hasard, and in « Mes jardins » opening or deleting a kept garden, hand over how long « Annuler » is offered, which the screen reads from the accessibility of the phone; keeping a
 * garden in « Mes jardins », how long « « X » gardé. » shows, and the name of its mood as the screen shows it. While
 * the garden is listened to, the screen hands each frame it draws over to [onFrame], at its time in nanoseconds (ADR
 * 0012).
 */
@Immutable
internal class GardenActions(
    val onChangeGame: () -> Unit,
    val onListen: () -> Unit = {},
    val onFrame: (frameNanos: Long) -> Unit = {},
    val onRoll: (undoMillis: Long) -> Unit = {},
    val onMood: (GardenMood) -> Unit = {},
    val onClearAll: (undoMillis: Long) -> Unit = {},
    val onTap: (Position) -> Unit = {},
    val onChoose: (PieceType) -> Unit = {},
    val onRemove: () -> Unit = {},
    val onTurn: (Rotation) -> Unit = {},
    val onPlace: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onOpenMyGardens: () -> Unit = {},
    val onCloseMyGardens: () -> Unit = {},
    val onKeep: (moodName: String) -> Unit = {},
    val onSaveInPlace: (messageMillis: Long) -> Unit = {},
    val onNameChanged: (String) -> Unit = {},
    val onCancelNaming: () -> Unit = {},
    val onConfirmKeeping: (messageMillis: Long) -> Unit = {},
    val onOpenKept: (GardenId, undoMillis: Long) -> Unit = { _, _ -> },
    val onRename: (GardenId) -> Unit = {},
    val onConfirmRenaming: () -> Unit = {},
    val onDelete: (GardenId) -> Unit = {},
    val onCancelDeleting: () -> Unit = {},
    val onConfirmDeleting: (undoMillis: Long) -> Unit = {},
) {
    companion object {
        /** Actions that do nothing, for the previews. */
        val None = GardenActions(onChangeGame = {})
    }
}

/**
 * The musical garden (V40), drawn in the Colorful theme only, day or night as the phone, the effects of the application
 * kept (ADR 0026, ADR 0027). From the top, scrolling on a small screen or in a large font: the bar (Changer de jeu, the
 * title, Au hasard, Écouter), the moods, the information row (the scale and the tempo, Tout effacer), the chords over
 * the grid of 16 × 14, the message, the tray of the 7 pieces and Retirer. Anchored at the bottom, within reach of the
 * thumb: turn left, Poser, turn right. « Jardin effacé · Annuler » (or « « X » ouvert », « « X » supprimé. ») shows over
 * the bottom of the page while it is offered, unless the sheet « Mes jardins » is open over it. Until the garden kept is read ([uiState] null), the bar alone, Au hasard and Écouter greyed. While the garden
 * is listened to, the column played is framed on the grid and the pill of its chord filled, and the frames of the
 * screen are handed over to the view model (ADR 0012). No logic: what each action means belongs to the view model and
 * to `app`. Changer de jeu counts only while the garden is resumed.
 * The last change of the garden plays its feedback on the grid ([GardenFeedback]), and the notes of the column played
 * bounce ([playHeadBounce]), from their tick 0 on the frames of the screen, or frozen at [effectTick] for the previews;
 * the feedback of the change shown first, as on coming back to the screen, is not played again.
 */
@Composable
internal fun GardenScreen(
    uiState: GardenUiState?,
    actions: GardenActions,
    modifier: Modifier = Modifier,
    effectTick: Int? = null,
) {
    val changeGame = dropUnlessResumed { actions.onChangeGame() }
    val undoMillis = undoTimeoutMillis()
    val listening = uiState?.playedColumn != null
    // The clock of the play head belongs to the screen (ADR 0012): it hands over the frames only while listening.
    val onFrame by rememberUpdatedState(actions.onFrame)
    LaunchedEffect(listening) {
        if (listening) {
            while (true) {
                withFrameNanos { onFrame(it) }
            }
        }
    }
    // The height of « Jardin effacé · Annuler », its margins included, which the page leaves free under the tray.
    var undoBar by remember { mutableIntStateOf(0) }
    ColorfulTheme {
        val motion = rememberGardenMotion(uiState, effectTick)
        val keptTick = rememberKeptMessageTick(uiState?.keeps)
        val effects = BloxTheme.effects
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BloxTheme.palette.page)
                .safeDrawingPadding(),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space2),
                    verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GardenBar(
                        onChangeGame = changeGame,
                        onRoll = { actions.onRoll(undoMillis) },
                        onMyGardens = actions.onOpenMyGardens,
                        onListen = actions.onListen,
                        ready = uiState != null,
                        listening = listening,
                        modified = uiState?.modified == true,
                        keeps = uiState?.keeps ?: 0,
                    )
                    if (uiState != null) {
                        Moods(uiState.garden.mood, actions.onMood)
                        InformationRow(
                            mood = uiState.garden.mood,
                            hasPieces = uiState.garden.pieces.isNotEmpty(),
                            onClearAll = { actions.onClearAll(undoMillis) },
                        )
                        ChordsAndGrid(uiState, actions.onTap, motion)
                        Message(uiState)
                        Tray(uiState, actions)
                        // Room for the message while it shows: it never covers Retirer, scrolled to the end.
                        if (uiState.undo != null || uiState.keptName != null) {
                            Spacer(Modifier.height(with(LocalDensity.current) { undoBar.toDp() }))
                        }
                    }
                }
                val messageModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { undoBar = it.height }
                    .padding(horizontal = BloxSpaces.space4, vertical = BloxSpaces.space2)
                // Under the sheet « Mes jardins », none: the sheet tells its own (« « X » supprimé. »).
                when {
                    uiState == null || uiState.sheet != null -> {}

                    uiState.undo != null -> SnackMessage(
                        text = undoText(uiState.undo, uiState.undoFailed),
                        modifier = messageModifier,
                        action = MessageAction(stringResource(R.string.feature_garden_undo), actions.onUndo),
                    )

                    uiState.keptName != null -> SnackMessage(
                        text = stringResource(R.string.feature_garden_kept, uiState.keptName),
                        modifier = messageModifier.graphicsLayer {
                            val entrance = keptMessageEntrance(keptTick(), effects)
                            alpha = entrance.alpha
                            translationY = entrance.rise * BloxSizes.messageRise.toPx()
                        },
                    )
                }
            }
            if (uiState != null) {
                Controls(uiState, actions)
                ClosingSheet(uiState.takeIf { it.sheet != null }) { open, leaving ->
                    MyGardensSheet(open, actions, leaving)
                }
            }
        }
    }
}

/** What the message that offers « Annuler » says of [undo]; that the operation failed, once Annuler [failed]. */
@Composable
@ReadOnlyComposable
internal fun undoText(
    undo: GardenUndo,
    failed: Boolean,
): String = when {
    failed -> stringResource(R.string.feature_garden_failed)
    else -> undoText(undo)
}

/** What the message that offers « Annuler » says of [undo]. */
@Composable
@ReadOnlyComposable
private fun undoText(undo: GardenUndo): String = when (undo) {
    is GardenUndo.Cleared -> stringResource(R.string.feature_garden_cleared)
    is GardenUndo.Opened -> stringResource(R.string.feature_garden_opened, undo.name)
    is GardenUndo.Deleted -> stringResource(R.string.feature_garden_deleted, undo.deleted.garden.name)
}

/**
 * How long « Annuler » is offered on this phone: [UNDO_MILLIS], or as long as Android recommends when the player asked
 * it for more time to act (WCAG 2.1, 2.2.1).
 */
@Composable
@ReadOnlyComposable
internal fun undoTimeoutMillis(): Long = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
    originalTimeoutMillis = UNDO_MILLIS,
    containsIcons = false,
    containsText = true,
    containsControls = true,
) ?: UNDO_MILLIS

/**
 * The bar: the disc « Changer de jeu » at the start, then the title, in capitals, read as a heading in words, then the
 * die « Au hasard », the disc « Mes jardins » and the disc Écouter, greyed until the garden is [ready]. « Mes jardins »
 * bears a dot in `ink` while the garden [modified] since it was kept, read « jardin modifié ». Its dot goes out after the
 * sheet once a garden is kept, the [keeps] counted. Écouter shows a triangle,
 * read « Écouter le jardin »; while [listening], a square, read « Arrêter le jardin », keeping its pressed fill: the
 * listen button of the musics (`ListenButton`).
 */
@Composable
private fun GardenBar(
    onChangeGame: () -> Unit,
    onRoll: () -> Unit,
    onMyGardens: () -> Unit,
    onListen: () -> Unit,
    ready: Boolean,
    listening: Boolean,
    modified: Boolean,
    keeps: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.CARTRIDGE,
            description = stringResource(R.string.feature_garden_change_game),
            onClick = onChangeGame,
            modifier = Modifier.testTag(GARDEN_CHANGE_GAME_TAG),
        )
        val title = stringResource(R.string.feature_garden_title)
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .weight(1f)
                // Its text kept, TalkBack reads the title in words in its place, as a heading.
                .semantics {
                    contentDescription = title
                    heading()
                },
            color = BloxTheme.palette.ink,
            style = BloxTheme.typography.gameTitle,
            // With a large font on a narrow screen, it shrinks rather than cut a word.
            autoSize = shrinkToWholeWords(BloxTheme.typography.gameTitle.fontSize),
        )
        GlyphButton(
            icon = BloxIcon.DICE,
            description = stringResource(R.string.feature_garden_random),
            onClick = onRoll,
            enabled = ready,
        )
        MyGardensDisc(onClick = onMyGardens, enabled = ready, modified = modified, keeps = keeps)
        ListenButton(
            playing = listening,
            musicName = stringResource(R.string.feature_garden_music_name),
            onClick = onListen,
            enabled = ready,
        )
    }
}

/**
 * The disc « Mes jardins », which opens its sheet; while the garden [modified] since it was kept, a dot in `ink` of
 * 8 dp at its top end, a shape rather than a colour, read as its state, « jardin modifié ». It fades out as it goes,
 * after the sheet has gone down when a garden was just kept (one more of the gardens kept, [keeps]), at once otherwise.
 */
@Composable
private fun MyGardensDisc(
    onClick: () -> Unit,
    enabled: Boolean,
    modified: Boolean,
    keeps: Int,
) {
    val state = stringResource(R.string.feature_garden_modified)
    Box {
        GlyphButton(
            icon = BloxIcon.LIST,
            description = stringResource(R.string.feature_garden_my_gardens),
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.semantics { if (modified) stateDescription = state },
        )
        val effects = BloxTheme.effects
        // Shown with the count of the gardens kept: once a garden is kept, it goes out after the sheet ([keptDotLight]).
        LeavingContent(open = keeps.takeIf { modified }, length = keptDotTicks(effects)) { keepsShown, leaving ->
            val afterSheet = keeps != keepsShown
            val ink = BloxTheme.palette.ink
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(if (leaving == null) Modifier.testTag(GARDEN_MODIFIED_DOT_TAG) else Modifier)
                    .size(MODIFIED_DOT)
                    .drawBehind {
                        drawCircle(ink, alpha = leaving?.let { keptDotLight(it(), effects, afterSheet) } ?: 1f)
                    },
            )
        }
    }
}

/** The 4 moods, one group of choice read « Ambiance »; the one of the garden, [mood], retained. */
@Composable
private fun Moods(
    mood: GardenMood,
    onMood: (GardenMood) -> Unit,
) {
    val choices = GardenMood.entries.map { ChoiceItem(label = stringResource(it.label)) }
    ChoiceSegments(
        name = stringResource(R.string.feature_garden_moods),
        choices = choices,
        selected = mood.ordinal,
        onChosen = { onMood(GardenMood.entries[it]) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The scale of [mood] and its tempo, « Do pentatonique · 84 bpm », read with its tempo in words; then Tout effacer,
 * greyed unless the garden [hasPieces]. Side by side while the information holds on 2 lines without cutting a word
 * ([holdsBeside]); otherwise, as in a large font, Tout effacer goes under it, at the end.
 */
@Composable
private fun InformationRow(
    mood: GardenMood,
    hasPieces: Boolean,
    onClearAll: () -> Unit,
) {
    val scale = stringResource(scaleNameOf(mood))
    val tempo = pluralStringResource(R.plurals.feature_garden_tempo_spoken, mood.tempo, mood.tempo)
    val spoken = stringResource(R.string.feature_garden_mood_info_spoken, scale, tempo)
    val gap = BloxSpaces.space2
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Text(
                text = stringResource(R.string.feature_garden_mood_info, scale, mood.tempo),
                modifier = Modifier
                    .padding(start = BloxSpaces.space1)
                    .semantics { contentDescription = spoken },
                color = BloxTheme.palette.label,
                style = BloxTheme.typography.small,
            )
            SecondaryButton(
                text = stringResource(R.string.feature_garden_clear_all),
                icon = BloxIcon.ERASE,
                onClick = onClearAll,
                enabled = hasPieces,
            )
        },
    ) { (text, button), constraints ->
        val width = constraints.maxWidth
        val space = gap.roundToPx()
        val clear = button.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val room = (width - clear.width - space).coerceAtLeast(0)
        val beside = holdsBeside(
            room = room,
            longestWord = text.minIntrinsicWidth(Constraints.Infinity),
            height = text.maxIntrinsicHeight(room),
            lineHeight = text.maxIntrinsicHeight(Constraints.Infinity),
        )
        if (beside) {
            val info = text.measure(Constraints(maxWidth = room))
            val height = maxOf(info.height, clear.height)
            layout(width, height) {
                info.placeRelative(0, (height - info.height) / 2)
                clear.placeRelative(width - clear.width, (height - clear.height) / 2)
            }
        } else {
            val info = text.measure(Constraints(maxWidth = width))
            layout(width, info.height + space + clear.height) {
                info.placeRelative(0, 0)
                clear.placeRelative(width - clear.width, info.height + space)
            }
        }
    }
}

/**
 * The band of the 4 chords over the grid, each pill centred over its group of 4 eighths, read in one; then the grid,
 * its cells as wide as the width allows, 22 dp at most ([gridCellFor]).
 */
@Composable
private fun ChordsAndGrid(
    state: GardenUiState,
    onTap: (Position) -> Unit,
    motion: GardenMotion,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cell = gridCellFor(maxWidth)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BloxSpaces.space1),
        ) {
            ChordBand(state.garden.mood, playedChordOf(state), cell)
            Grid(state, cell, onTap, motion)
        }
    }
}

/**
 * One pill per chord of [mood], each over its group of 4 cells of [cell]; the one [playedChord], if the garden is
 * listened to, filled.
 */
@Composable
private fun ChordBand(
    mood: GardenMood,
    playedChord: Int?,
    cell: Dp,
) {
    val reading = moodChordsText(mood)
    val group = cell * GardenMood.EIGHTHS_PER_CHORD
    Row(
        modifier = Modifier
            .padding(horizontal = GRID_FRAME)
            .clearAndSetSemantics { contentDescription = reading },
    ) {
        mood.progression.numerals.forEachIndexed { chord, numeral ->
            Box(modifier = Modifier.width(group), contentAlignment = Alignment.Center) {
                ChordPill(
                    label = numeral.name,
                    played = chord == playedChord,
                    modifier = Modifier.width(min(group - PILL_INSET, CHORD_PILL)),
                )
            }
        }
    }
}

/**
 * The grid of 16 × 14 cells of [cell] in its frame on the board, a group of 4 eighths out of two on a band; a
 * collection of 14 rows and 16 columns for TalkBack, each cell read « Croche 5, mi : libre » and touched by [onTap].
 * Its notes play their effects as [motion] says, read in the drawing alone.
 */
@Composable
private fun Grid(
    state: GardenUiState,
    cell: Dp,
    onTap: (Position) -> Unit,
    motion: GardenMotion,
) {
    val cells = remember(state) { gardenCells(state) }
    val effects = BloxTheme.effects
    val shape = RoundedCornerShape(GRID_CORNER)
    Column(
        modifier = Modifier
            .background(BloxTheme.palette.boardBackground, shape)
            .border(BloxBorders.frame, BloxTheme.palette.panelBorder, shape)
            .padding(GRID_FRAME)
            .semantics { collectionInfo = CollectionInfo(rowCount = Garden.ROWS, columnCount = Garden.COLUMNS) },
    ) {
        for (row in 0 until Garden.ROWS) {
            Row {
                for (column in 0 until Garden.COLUMNS) {
                    val position = Position(column, row)
                    val shown = cells[position] ?: GardenCell.Free
                    val playHead = playHeadAt(position, state)
                    val noteEffects = if (shown is GardenCell.Note) {
                        remember(motion, effects, position, playHead) {
                            NoteEffects(motion, effects, position, played = playHead != null)
                        }
                    } else {
                        null
                    }
                    GridCell(
                        cell = shown,
                        position = position,
                        size = cell,
                        mood = state.garden.mood,
                        playHead = playHead,
                        effects = noteEffects,
                        onTap = onTap,
                    )
                }
            }
        }
    }
}

/**
 * A cell of the grid, [cell] at [position], touched by [onTap]; in a band one group of 4 eighths out of two; framed
 * as its part of the [playHead], in the column played; a note playing its [effects].
 */
@Composable
private fun GridCell(
    cell: GardenCell,
    position: Position,
    size: Dp,
    mood: GardenMood,
    playHead: GardenPlayHead?,
    effects: GardenCellEffects?,
    onTap: (Position) -> Unit,
) {
    val description = cellText(cell, position, mood)
    val action = stringResource(cellActionOf(cell))
    GardenCellCanvas(
        cell = cell,
        size = size,
        inBand = isInGardenBand(position.column),
        playHead = playHead,
        effects = effects,
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = action) { onTap(position) }
            .semantics {
                contentDescription = description
                collectionItemInfo = CollectionItemInfo(
                    rowIndex = position.row,
                    rowSpan = 1,
                    columnIndex = position.column,
                    columnSpan = 1,
                )
            },
    )
}

/** What can be done, or why the preview cannot be placed, on 2 lines in ink; TalkBack reads it as it changes. */
@Composable
private fun Message(state: GardenUiState) {
    Text(
        text = stringResource(messageOf(state)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = BloxTheme.palette.ink,
        style = BloxTheme.typography.small,
        textAlign = TextAlign.Center,
        minLines = 2,
    )
}

/**
 * The tray: 2 rows of 4 tiles of 84 × 52 dp at most, 6 dp apart, the 7 pieces then Retirer. The pieces are one group of
 * choice for TalkBack, « Pièces », the chosen one drawn in its orientation; Retirer stands apart from it, greyed while
 * there is no preview.
 */
@Composable
private fun Tray(
    state: GardenUiState,
    actions: GardenActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val tile = min(TILE_WIDTH, (maxWidth - TILE_GAP * (TILES_PER_ROW - 1)) / TILES_PER_ROW)
        val name = stringResource(R.string.feature_garden_pieces)
        Box {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .semantics { contentDescription = name },
                verticalArrangement = Arrangement.spacedBy(BloxSpaces.space2),
            ) {
                TRAY.chunked(TILES_PER_ROW).forEach { types ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                        types.forEach { type -> Tile(state, type, tile, actions.onChoose) }
                        // The place of Retirer, at the end of the last row, outside the group.
                        if (types.size < TILES_PER_ROW) Spacer(Modifier.size(tile, TILE_HEIGHT))
                    }
                }
            }
            RemoveButton(
                onClick = actions.onRemove,
                enabled = state.preview != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(tile, TILE_HEIGHT),
            )
        }
    }
}

/** The tile of the piece of [type], chosen or not, [width] wide. */
@Composable
private fun Tile(
    state: GardenUiState,
    type: PieceType,
    width: Dp,
    onChoose: (PieceType) -> Unit,
) {
    val chosen = type == state.chosen
    val orientation = if (chosen) state.orientation else ClassicRotationSystem.spawnOrientation(type)
    PieceTile(
        piece = pieceCells(type, orientation),
        description = stringResource(R.string.feature_garden_tile, type.name),
        onClick = { onChoose(type) },
        modifier = Modifier.size(width, TILE_HEIGHT),
        chosen = chosen,
    )
}

/** Retirer: a cross then its label, a tile of the tray that takes the preview away. */
@Composable
private fun RemoveButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    BubbleButton(
        family = ButtonFamily.ICON,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = BloxTheme.shapes.md,
    ) {
        val ink = LocalContentColor.current
        Spacer(modifier = Modifier.size(CROSS).drawBehind { Glyphs.close(this, ink) })
        Text(
            text = stringResource(R.string.feature_garden_remove),
            maxLines = 1,
            autoSize = shrinkToWholeWords(LocalTextStyle.current.fontSize),
        )
    }
}

/** Turn left, Poser, turn right: anchored at the bottom of the screen, within reach of the thumb. */
@Composable
private fun Controls(
    state: GardenUiState,
    actions: GardenActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(BloxSpaces.space3),
        horizontalArrangement = Arrangement.spacedBy(BloxSpaces.space4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            icon = BloxIcon.ROTATE_LEFT,
            description = stringResource(R.string.feature_garden_turn_left),
            onClick = { actions.onTurn(Rotation.COUNTERCLOCKWISE) },
        )
        PrimaryButton(
            text = stringResource(R.string.feature_garden_place),
            icon = BloxIcon.PLACE,
            onClick = actions.onPlace,
            enabled = state.preview != null,
        )
        GlyphButton(
            icon = BloxIcon.ROTATE_RIGHT,
            description = stringResource(R.string.feature_garden_turn_right),
            onClick = { actions.onTurn(Rotation.CLOCKWISE) },
        )
    }
}

/** The pieces of the tray, in the order of the maquette. */
private val TRAY = listOf(PieceType.I, PieceType.O, PieceType.T, PieceType.S, PieceType.Z, PieceType.J, PieceType.L)

private const val TILES_PER_ROW = 4

/** A tile of the tray, at most, as on a phone of 390 dp (V40). */
private val TILE_WIDTH = 84.dp
private val TILE_HEIGHT = 52.dp
private val TILE_GAP = 6.dp

/** The cross of Retirer. */
private val CROSS = 16.dp

/** A pill of a chord, at most, and the room left on each side of its group of 4 cells. */
private val CHORD_PILL = 76.dp
private val PILL_INSET = 12.dp

/** The dot of « Mes jardins » while the garden changed since it was kept. */
private val MODIFIED_DOT = 8.dp

/** The corners of the frame of the grid. */
private val GRID_CORNER = 10.dp
