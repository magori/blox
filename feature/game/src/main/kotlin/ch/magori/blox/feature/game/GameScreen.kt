package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.feature.game.board.BoardCanvas
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.controls.DirectionPad
import ch.magori.blox.feature.game.controls.RotationPad
import ch.magori.blox.feature.game.controls.rememberControlFingers
import ch.magori.blox.feature.game.gameover.GameOverScreen
import ch.magori.blox.feature.game.pause.PauseScreen
import ch.magori.blox.feature.game.sidepanel.NextPiecePanel
import ch.magori.blox.feature.game.sidepanel.ScorePanels
import ch.magori.blox.feature.game.sidepanel.SidePanel
import ch.magori.blox.feature.game.topbar.GameTopBar
import ch.magori.blox.feature.game.ui.ScreenSizes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/** The 24 dp margins the controls are sized for (P2.5). */
private val ScreenMargin = 24.dp

/**
 * The game screen of [viewModel]. The screen owns the frame clock (ADR 0012): while it is started
 * and the game is not paused, it hands the view model every frame, after telling it that frames
 * start again.
 *
 * When the screen is no longer in front (`ON_PAUSE`), the game pauses and stays paused until the
 * player resumes it. A recreation of the activity (dark theme, language, window size) goes through
 * `ON_PAUSE` too, and so pauses the game: this is intended.
 *
 * The system back follows [backAction]: it pauses the game, resumes it, or, once the game is over,
 * leaves it as the navigation does.
 */
@Composable
internal fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The state itself, never its value: this root reads only what the system back needs, and so does
    // not recompose at each move of the piece.
    val uiStateHolder = viewModel.uiState.collectAsStateWithLifecycle()
    val uiState = remember(uiStateHolder) { { uiStateHolder.value } }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // No frames while paused: the loop starts again, from scratch, when the game resumes.
            viewModel.uiState
                .map { it.paused }
                .distinctUntilChanged()
                .collectLatest { paused ->
                    if (!paused) {
                        viewModel.onFramesStarted()
                        while (true) {
                            withFrameNanos(viewModel::onFrame)
                        }
                    }
                }
        }
    }
    // A game over or a game already paused ignores the pause.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }
    val backAction by rememberPart(uiState, ::backAction)
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = backAction != BackAction.LEAVE,
        onBackCompleted = {
            when (backAction) {
                BackAction.PAUSE -> viewModel.onPause()
                BackAction.RESUME -> viewModel.onResume()
                BackAction.LEAVE -> Unit
            }
        },
    )
    // Only while resumed: a second tap during the transition must not act on the next entry.
    GameScreen(
        uiState = uiState,
        onBack = dropUnlessResumed(block = onBack),
        onReplay = dropUnlessResumed(block = onReplay),
        onControl = viewModel::onControl,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        modifier = modifier,
    )
}

/**
 * The part of the state given by [uiState] that [part] picks, as a state that changes only when that
 * part changes: the composable that reads it recomposes for that part only, not at each move of the
 * piece. [part] is not a key: the first one given is kept, so it must capture nothing, as a property
 * of [GameUiState] or a function of it alone.
 */
@Composable
private fun <T> rememberPart(
    uiState: () -> GameUiState,
    part: (GameUiState) -> T,
): State<T> = remember(uiState) { derivedStateOf { part(uiState()) } }

/**
 * The game screen for the state given by [uiState]: the top bar ([onPause], the record), the board, the
 * side column and the controls, whose changes go to [onControl]. Paused, the pause screen in their
 * place, the board hidden: Reprendre is [onResume], Menu is [onBack]. Once the game is over, the game
 * over screen, which has priority (ADR 0011): Menu is [onBack], Rejouer is [onReplay]. No logic here.
 *
 * Each part of the screen reads only the part of the state it shows: a move of the piece draws the
 * board again, and recomposes nothing.
 */
@Composable
internal fun GameScreen(
    uiState: () -> GameUiState,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameOver = rememberPart(uiState) { it.gameOver }.value
    val paused by rememberPart(uiState) { it.paused }
    when {
        gameOver != null -> GameOverScreen(state = gameOver, onReplay = onReplay, onMenu = onBack, modifier = modifier)
        paused -> PauseScreen(onResume = onResume, onMenu = onBack, modifier = modifier)
        else -> Game(uiState = uiState, onControl = onControl, onPause = onPause, modifier = modifier)
    }
}

/** The game under way: the top bar, the board, the side column and the controls. */
@Composable
private fun Game(
    uiState: () -> GameUiState,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val record by rememberPart(uiState) { it.record }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .padding(ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(ScreenSizes.gap),
    ) {
        GameTopBar(record = record, onPause = onPause)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val density = LocalDensity.current.density
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val layout = remember(width, height, density) { gameLayout(width, height, density) }
            Play(uiState = uiState, layout = layout, onControl = onControl, modifier = Modifier.fillMaxSize())
        }
    }
}

/** What the side column shows: it changes when a piece locks, not when it moves. */
@Immutable
private data class SideColumn(
    val score: Points,
    val level: Level,
    val lines: Lines,
    val next: PieceType,
    val rotationSystem: RotationSystem,
)

private fun GameUiState.sideColumn(): SideColumn = SideColumn(score, level, lines, next, rotationSystem)

/**
 * The board, the side column and the controls, each where [layout] puts it. The score column scrolls
 * when its panels do not fit, as with an enlarged font, rather than pass under the screen or over the
 * controls. The board reads the board and the piece only while drawing.
 */
@Composable
private fun Play(
    uiState: () -> GameUiState,
    layout: GameLayout,
    onControl: (ControlChange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fingers = rememberControlFingers(onControl)
    val scoreScroll = rememberScrollState()
    val side by rememberPart(uiState, GameUiState::sideColumn)
    PlacedParts(modifier = modifier) {
        BoardCanvas(
            board = { uiState().board },
            piece = { uiState().piece },
            modifier = Modifier.placedAt(layout.board),
        )
        val nextPiece = layout.nextPiece
        if (nextPiece == null) {
            SidePanel(
                score = side.score,
                level = side.level,
                lines = side.lines,
                next = side.next,
                rotationSystem = side.rotationSystem,
                cellSize = layout.cellSize,
                modifier = Modifier.placedAt(layout.sidePanel).verticalScroll(scoreScroll),
            )
        } else {
            ScorePanels(
                score = side.score,
                level = side.level,
                lines = side.lines,
                modifier = Modifier.placedAt(layout.sidePanel).verticalScroll(scoreScroll),
            )
            NextPiecePanel(
                next = side.next,
                rotationSystem = side.rotationSystem,
                cellSize = layout.cellSize,
                modifier = Modifier.placedAt(nextPiece).wrapContentHeight(Alignment.Top),
            )
        }
        DirectionPad(fingers = fingers, modifier = Modifier.placedAt(layout.directions))
        RotationPad(fingers = fingers, modifier = Modifier.placedAt(layout.rotations))
    }
}

/**
 * Each part of [content] exactly in the rectangle its [placedAt] gives, in pixels of this space: the
 * part takes the size of the rectangle, and is placed at its position.
 */
@Composable
private fun PlacedParts(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val parts = measurables.map { measurable ->
            val rect = measurable.layoutId as Rect
            val left = rect.left.roundToInt()
            val top = rect.top.roundToInt()
            val width = (rect.right.roundToInt() - left).coerceAtLeast(0)
            val height = (rect.bottom.roundToInt() - top).coerceAtLeast(0)
            measurable.measure(Constraints.fixed(width, height)) to IntOffset(left, top)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            parts.forEach { (placeable, position) -> placeable.place(position) }
        }
    }
}

/** Where [PlacedParts] puts this part: [rect], in pixels of its space. */
private fun Modifier.placedAt(rect: Rect): Modifier = layoutId(rect)

/** A game under way, for the previews: a stack, a T falling, an I next, a record. */
private val previewState = GameUiState(
    board = Board.parse(
        """
        J.........
        JJJ..OO..L
        SSTTTOO.LL
        IIII.ZZSSL
        """,
    ),
    piece = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(column = 4, row = 6)),
    next = PieceType.I,
    level = Level(5),
    lines = Lines(52),
    score = Points(12_340),
    rotationSystem = ClassicRotationSystem,
    record = 98_760,
)

@Composable
private fun GameScreenPreview(
    kind: BloxThemeKind,
    uiState: GameUiState,
) {
    BloxTheme(kind = kind) {
        GameScreen(uiState = { uiState }, onBack = {}, onReplay = {}, onControl = {}, onPause = {}, onResume = {})
    }
}

// `internal` rather than `private`: the screenshot tests (src/screenshotTest) render these previews.

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenOriginalPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenColorfulPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenPausedPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState.copy(paused = true))
}

@Preview(widthDp = 360, heightDp = 760, fontScale = 2f)
@Composable
internal fun GameScreenLargeFontPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState)
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun GameScreenLandscapePreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState)
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenTabletPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState)
}

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun GameScreenTabletPortraitPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState)
}

@Preview(widthDp = 220, heightDp = 640)
@Composable
private fun GameScreenNarrowPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState)
}
