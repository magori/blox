package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
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
import ch.magori.blox.feature.game.board.BoardCanvas
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.controls.DirectionPad
import ch.magori.blox.feature.game.controls.HeldControls
import ch.magori.blox.feature.game.controls.Menu
import ch.magori.blox.feature.game.controls.RotationPad
import ch.magori.blox.feature.game.controls.keySourceOf
import ch.magori.blox.feature.game.controls.menuKey
import ch.magori.blox.feature.game.controls.rememberHeldControls
import ch.magori.blox.feature.game.gameover.GameOverScreen
import ch.magori.blox.feature.game.pause.PauseScreen
import ch.magori.blox.feature.game.sidepanel.NextPieceCells
import ch.magori.blox.feature.game.sidepanel.NextPiecePanel
import ch.magori.blox.feature.game.sidepanel.ScoreGrid
import ch.magori.blox.feature.game.sidepanel.ScorePanels
import ch.magori.blox.feature.game.sidepanel.SidePanel
import ch.magori.blox.feature.game.sidepanel.nextPieceCells
import ch.magori.blox.feature.game.topbar.GameTopBar
import ch.magori.blox.feature.game.ui.ScreenSizes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * The game screen of [viewModel]. The screen owns the frame clock (ADR 0012): while it is started
 * and the game runs ([GameUiState.running]: neither paused nor over), it hands the view model every frame,
 * after telling it that frames start again. While it shows, paused or over included, it asks the display for 60 Hz
 * ([GameDisplayModeEffect]), the rate of the simulation.
 *
 * When the screen is no longer in front (`ON_PAUSE`), the game pauses and stays paused until the
 * player resumes it. A recreation of the activity (dark theme, language, window size) goes through
 * `ON_PAUSE` too, and so pauses the game: this is intended.
 *
 * The system back follows [backAction]: it pauses the game, resumes it, or, once the game is over or while the
 * window is too small to play, leaves it as the navigation does.
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
            // Frames only while the game runs: none while paused, none once it is over. The loop starts again,
            // from scratch, when the game resumes.
            viewModel.uiState
                .map { it.running }
                .distinctUntilChanged()
                .collectLatest { running ->
                    if (running) {
                        viewModel.onFramesStarted()
                        while (true) {
                            withFrameNanos(viewModel::onFrame)
                        }
                    }
                }
        }
    }
    // The simulation runs at 60 Hz: so does the display, while the game screen shows, paused or over included.
    GameDisplayModeEffect()
    // A game over or a game already paused ignores the pause.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }
    var tooSmallToPlay by remember { mutableStateOf(false) }
    val backAction by rememberPart(uiState) { backAction(it, tooSmallToPlay) }
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
        onTooSmallToPlayChange = { tooSmallToPlay = it },
    )
}

/**
 * The part of the state given by [uiState] that [part] picks, as a state that changes only when that
 * part changes: the composable that reads it recomposes for that part only, not at each move of the
 * piece. [part] is not a key: the first one given is kept, so it must capture no plain value, only
 * states, read each time it runs: a property of [GameUiState], a function of it alone or of states.
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
 * In a window too small to play ([tooSmallToPlay], measured inside the system bars whatever shows), the message
 * « Agrandissez la fenêtre » in place of the board and of the pause screen, under the top bar, its pause button
 * disabled. The game is paused by [onPause] once the composition that shows the message is applied, before the next
 * frame and its ticks; the pause screen comes back only with a window large enough, where Reprendre resumes the
 * game. Whether the window is too small goes to [onTooSmallToPlayChange] at the same time.
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
    onTooSmallToPlayChange: (Boolean) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        val bars = WindowInsets.safeDrawing
        val width = constraints.maxWidth - bars.getLeft(density, direction) - bars.getRight(density, direction)
        val height = constraints.maxHeight - bars.getTop(density) - bars.getBottom(density)
        val tooSmall = remember(width, height, density.density) {
            tooSmallToPlay(width.toFloat(), height.toFloat(), density.density)
        }
        val running by rememberPart(uiState) { it.running }
        SideEffect {
            onTooSmallToPlayChange(tooSmall)
            if (tooSmall && running) onPause()
        }
        Screens(
            uiState = uiState,
            tooSmallToPlay = tooSmall,
            onBack = onBack,
            onReplay = onReplay,
            onControl = onControl,
            onPause = onPause,
            onResume = onResume,
        )
    }
}

/** The screen shown, among the game, its pause and its game over, as [GameScreen] says. */
@Composable
private fun Screens(
    uiState: () -> GameUiState,
    tooSmallToPlay: Boolean,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val gameOver = rememberPart(uiState) { it.gameOver }.value
    val paused by rememberPart(uiState) { it.paused }
    // Each of the three screens takes the focus as it shows, so that the keys of a keyboard or a pad reach it.
    val focus = remember { FocusRequester() }
    val menu = when {
        gameOver != null -> Menu.GAME_OVER
        paused -> Menu.PAUSE
        else -> null
    }
    LaunchedEffect(menu) { focus.requestFocus() }
    val focusManager = LocalFocusManager.current
    // Whether the menu itself holds the focus, none of its buttons: an arrow then moves it into the buttons.
    var menuFocused by remember { mutableStateOf(false) }
    val menuModifier = { shown: Menu ->
        Modifier
            .onFocusChanged { menuFocused = it.isFocused }
            .takingKeys(focus) {
                val repeatCount = it.nativeKeyEvent.repeatCount
                menuKey(shown, it.key, it.type, repeatCount, menuFocused, onResume) {
                    focusManager.moveFocus(FocusDirection.Enter)
                }
            }
    }
    when {
        gameOver != null -> GameOverScreen(
            state = gameOver,
            onReplay = onReplay,
            onMenu = onBack,
            modifier = menuModifier(Menu.GAME_OVER),
        )

        paused && !tooSmallToPlay -> PauseScreen(
            onResume = onResume,
            onMenu = onBack,
            modifier = menuModifier(Menu.PAUSE),
        )

        else -> Game(
            uiState = uiState,
            tooSmallToPlay = tooSmallToPlay,
            onControl = onControl,
            onPause = onPause,
            focus = focus,
        )
    }
}

/**
 * This part takes the keys that reach it once [focus] gives it the focus: [onKey] says whether it kept one
 * from the system.
 */
private fun Modifier.takingKeys(
    focus: FocusRequester,
    onKey: (KeyEvent) -> Boolean,
): Modifier = onKeyEvent(onKey).focusRequester(focus).focusTarget()

/**
 * The game under way: the top bar, the board, the side column and the controls. Given the focus by
 * [focus], it plays the keys of a keyboard or a pad as the controls ([HeldControls.key]), their pause key
 * being [onPause]. The other keys are left to the system: Tab moves the focus, for instance.
 *
 * In a window [tooSmallToPlay], the message « Agrandissez la fenêtre » in place of the board, the side column and
 * the controls, the pause button disabled: the game is already paused. The keys still reach the controls, which the
 * paused game ignores, rather than the system, where B and Escape would leave the game.
 */
@Composable
private fun Game(
    uiState: () -> GameUiState,
    tooSmallToPlay: Boolean,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val record by rememberPart(uiState) { it.record }
    val controls = rememberHeldControls(onControl)
    Column(
        modifier = modifier
            .takingKeys(focus) {
                val source = keySourceOf(it.nativeKeyEvent.source)
                controls.key(it.key, it.type, it.nativeKeyEvent.repeatCount, source, onPause)
            }
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .padding(ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(ScreenSizes.gap),
    ) {
        GameTopBar(record = record, onPause = onPause, pauseEnabled = !tooSmallToPlay)
        if (tooSmallToPlay) {
            TooSmallMessage(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val density = LocalDensity.current.density
                val fontScale = LocalDensity.current.fontScale
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()
                val layout =
                    remember(width, height, density, fontScale) { gameLayout(width, height, density, fontScale) }
                Play(uiState = uiState, layout = layout, controls = controls, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** What the side column shows: it changes when a piece locks, not when it moves. */
@Immutable
private data class SideColumn(val score: Points, val level: Level, val lines: Lines, val next: NextPieceCells)

private fun GameUiState.sideColumn(): SideColumn = SideColumn(score, level, lines, next)

/**
 * The board, the side column and the touch controls of [controls], each where [layout] puts it. The score column
 * scrolls when its panels do not fit, as with an enlarged font, rather than pass under the screen or over
 * the controls. The board reads the board and the piece only while drawing.
 */
@Composable
private fun Play(
    uiState: () -> GameUiState,
    layout: GameLayout,
    controls: HeldControls,
    modifier: Modifier = Modifier,
) {
    val scoreScroll = rememberScrollState()
    val side by rememberPart(uiState, GameUiState::sideColumn)
    PlacedParts(modifier = modifier) {
        BoardCanvas(
            board = { uiState().board },
            piece = { uiState().piece },
            modifier = Modifier.placedAt(layout.board, ReadingOrder.BOARD),
        )
        val nextPiece = layout.nextPiece
        val scores = Modifier.placedAt(layout.sidePanel, ReadingOrder.SCORES).verticalScroll(scoreScroll)
        when {
            layout.scoresAbove -> ScoreGrid(
                score = side.score,
                level = side.level,
                lines = side.lines,
                next = side.next,
                cellSize = layout.cellSize,
                modifier = scores,
            )

            nextPiece == null -> SidePanel(
                score = side.score,
                level = side.level,
                lines = side.lines,
                next = side.next,
                cellSize = layout.cellSize,
                modifier = scores,
            )

            else -> {
                ScorePanels(score = side.score, level = side.level, lines = side.lines, modifier = scores)
                NextPiecePanel(
                    next = side.next,
                    cellSize = layout.cellSize,
                    modifier = Modifier.placedAt(nextPiece, ReadingOrder.NEXT).wrapContentHeight(Alignment.Top),
                )
            }
        }
        val directionSpacing = with(LocalDensity.current) { layout.directionSpacing.toDp() }
        DirectionPad(
            controls = controls,
            modifier = Modifier.placedAt(layout.directions, ReadingOrder.DIRECTIONS),
            spacing = directionSpacing,
        )
        RotationPad(controls = controls, modifier = Modifier.placedAt(layout.rotations, ReadingOrder.ROTATIONS))
    }
}

/**
 * Each part of [content] exactly in the rectangle its [placedAt] gives, in pixels of this space: the
 * part takes the size of the rectangle, and is placed at its position. TalkBack reads the parts in
 * their [ReadingOrder].
 */
@Composable
private fun PlacedParts(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier.semantics { isTraversalGroup = true }) { measurables, constraints ->
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

/**
 * The order in which TalkBack reads the parts of the game, whatever their place: in landscape the arrows
 * are on the left, beside the score, and by their place alone they would be read first.
 */
private enum class ReadingOrder {
    BOARD,
    SCORES,
    NEXT,
    DIRECTIONS,
    ROTATIONS,
    ;

    /** From 1: the parts left at the default index, 0, would come first. */
    val index: Float get() = ordinal + 1f
}

/**
 * Where [PlacedParts] puts this part: [rect], in pixels of its space. TalkBack reads it as a whole, at its
 * place in [order].
 */
private fun Modifier.placedAt(
    rect: Rect,
    order: ReadingOrder,
): Modifier = layoutId(rect).semantics {
    isTraversalGroup = true
    traversalIndex = order.index
}

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
    next = nextPieceCells(PieceType.I, ClassicRotationSystem),
    level = Level(5),
    lines = Lines(52),
    score = Points(12_340),
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

@Preview(widthDp = 800, heightDp = 360, fontScale = 2f)
@Composable
internal fun GameScreenLandscapeLargeFontPreview() {
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
internal fun GameScreenNarrowPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState)
}

@Preview(widthDp = 220, heightDp = 640)
@Composable
internal fun GameScreenNarrowOriginalPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState)
}

@Preview(widthDp = 220, heightDp = 640, fontScale = 2f)
@Composable
internal fun GameScreenNarrowLargeFontPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState)
}

// The narrowest multi-window, too low to play: the message in place of the board, the game paused by it (P2.24).

@Preview(widthDp = 220, heightDp = 560)
@Composable
internal fun GameScreenTooSmallOriginalPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState.copy(paused = true))
}

@Preview(widthDp = 220, heightDp = 560)
@Composable
internal fun GameScreenTooSmallColorfulPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState.copy(paused = true))
}

@Preview(widthDp = 220, heightDp = 560, fontScale = 2f)
@Composable
internal fun GameScreenTooSmallLargeFontPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, previewState.copy(paused = true))
}
