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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import ch.magori.blox.core.board.BoardCanvas
import ch.magori.blox.core.board.piece.PieceCells
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.designsystem.theme.BloxEffects
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
import ch.magori.blox.feature.game.controls.ControlChange
import ch.magori.blox.feature.game.controls.DirectionPad
import ch.magori.blox.feature.game.controls.HeldControls
import ch.magori.blox.feature.game.controls.Menu
import ch.magori.blox.feature.game.controls.NoKeyAngle
import ch.magori.blox.feature.game.controls.RotationPad
import ch.magori.blox.feature.game.controls.keySourceOf
import ch.magori.blox.feature.game.controls.menuKey
import ch.magori.blox.feature.game.controls.rememberHeldControls
import ch.magori.blox.feature.game.feedback.FeedbackEffects
import ch.magori.blox.feature.game.feedback.PointsSticker
import ch.magori.blox.feature.game.feedback.lineClearFeedback
import ch.magori.blox.feature.game.feedback.rememberFeedbackEffects
import ch.magori.blox.feature.game.gameover.GameOverScreen
import ch.magori.blox.feature.game.pause.CurtainPhase
import ch.magori.blox.feature.game.pause.PauseCard
import ch.magori.blox.feature.game.pause.PauseCurtain
import ch.magori.blox.feature.game.pause.PauseCurtainEffect
import ch.magori.blox.feature.game.pause.PauseCurtainState
import ch.magori.blox.feature.game.pause.drawnAsCurtain
import ch.magori.blox.feature.game.pause.hiddenByCurtain
import ch.magori.blox.feature.game.pause.rememberPauseCurtainState
import ch.magori.blox.feature.game.sidepanel.NextPiecePanel
import ch.magori.blox.feature.game.sidepanel.ScoreGrid
import ch.magori.blox.feature.game.sidepanel.ScorePanels
import ch.magori.blox.feature.game.sidepanel.SidePanel
import ch.magori.blox.feature.game.sidepanel.SidePanelSizes
import ch.magori.blox.feature.game.sidepanel.bestBarFill
import ch.magori.blox.feature.game.topbar.GameTopBar
import ch.magori.blox.feature.game.topbar.shownBest
import ch.magori.blox.feature.game.ui.ScreenSizes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
 * The system back follows [backAction]: it pauses the game, resumes it as Reprendre does (the curtain of the pause
 * opens, then the game goes on), or, once the game is over or while the window is too small to play, leaves it as the
 * navigation does.
 *
 * The screen says what the variant of the game says ([GameViewModel.labels]): its title, its best to beat, and, for a
 * daily game, Copier at the game over, given to [onCopy].
 */
@Composable
internal fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onCopy: () -> Unit,
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
    var tooSmallToPlay by remember { mutableStateOf(false) }
    val backAction by rememberPart(uiState) { backAction(it, tooSmallToPlay) }
    val pauseCurtain = rememberPauseCurtainState { uiStateHolder.value.paused }
    // Every pause of the screen brings the curtain back down if Reprendre was opening it: the game must not go on.
    val pause = {
        pauseCurtain.pause()
        viewModel.onPause()
    }
    // A game over or a game already paused ignores the pause.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { pause() }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = backAction != BackAction.LEAVE,
        onBackCompleted = {
            when (backAction) {
                BackAction.PAUSE -> pause()
                BackAction.RESUME -> pauseCurtain.resume()
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
        pauseCurtain = pauseCurtain,
        labels = viewModel.labels,
        onCopy = dropUnlessResumed(block = onCopy),
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
 * The game screen for the state given by [uiState]: the top bar ([onPause], the best), the board, the
 * side column and the controls, whose changes go to [onControl]. Paused, the curtain of [pauseCurtain], of empty
 * cells, comes down over the board and the next piece, then the card of the pause shows over the game: Reprendre
 * opens the curtain, and only once it is open does the game go on, by [onResume]; Menu is [onBack]. The curtain moves
 * on the frame clock of Compose, the loop of the game stopped (ADR 0024). Once the game is over, the game over
 * screen, which has priority (ADR 0011): Menu is [onBack], Rejouer is [onReplay]. No logic here.
 *
 * In a window too small to play ([tooSmallToPlay], measured inside the system bars whatever shows), the message
 * « Agrandissez la fenêtre » in place of the board and of the card of the pause, under the top bar, its pause button
 * disabled. The game is paused by [onPause] once the composition that shows the message is applied, before the next
 * frame and its ticks; the card comes back only with a window large enough, where Reprendre resumes the
 * game. Whether the window is too small goes to [onTooSmallToPlayChange] at the same time.
 *
 * Each part of the screen reads only the part of the state it shows: a move of the piece draws the
 * board again, and recomposes nothing.
 *
 * The title, the label of the best to beat and its sticker, the badge of the game over and whether it offers Copier
 * ([onCopy]) are those of the variant of the game, its [labels] (ADR 0026).
 */
@Composable
internal fun GameScreen(
    uiState: () -> GameUiState,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    labels: GameLabels,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    onTooSmallToPlayChange: (Boolean) -> Unit = {},
    pauseCurtain: PauseCurtainState = rememberPauseCurtainState { uiState().paused },
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
        // A pause of the screen also brings back down the curtain that Reprendre was opening.
        val pause = {
            pauseCurtain.pause()
            onPause()
        }
        SideEffect {
            onTooSmallToPlayChange(tooSmall)
            // While the curtain opens the game is still paused, but it would go on at the end: it pauses too.
            if (tooSmall && (running || pauseCurtain.curtain?.phase == CurtainPhase.OPENING)) pause()
        }
        Screens(
            uiState = uiState,
            tooSmallToPlay = tooSmall,
            onBack = onBack,
            onReplay = onReplay,
            onControl = onControl,
            onPause = pause,
            onResume = onResume,
            pauseCurtain = pauseCurtain,
            labels = labels,
            onCopy = onCopy,
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
    pauseCurtain: PauseCurtainState,
    labels: GameLabels,
    onCopy: () -> Unit,
) {
    val gameOver = rememberPart(uiState) { it.gameOver }.value
    val paused by rememberPart(uiState) { it.paused }
    PauseCurtainEffect(state = pauseCurtain, paused = paused, onResume = onResume)
    val curtainShut by remember(pauseCurtain) {
        derivedStateOf { pauseCurtain.curtain?.phase == CurtainPhase.CLOSED }
    }
    // The game and each menu take the focus as they show, so that the keys of a keyboard or a pad reach them.
    val gameFocus = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }
    val menu = when {
        gameOver != null -> Menu.GAME_OVER
        curtainShut && !tooSmallToPlay -> Menu.PAUSE
        else -> null
    }
    LaunchedEffect(menu) { (if (menu == null) gameFocus else menuFocus).requestFocus() }
    val focusManager = LocalFocusManager.current
    // Whether the menu itself holds the focus, none of its buttons: an arrow then moves it into the buttons.
    var menuFocused by remember { mutableStateOf(false) }
    val menuModifier = { shown: Menu ->
        Modifier
            .onFocusChanged { menuFocused = it.isFocused }
            .takingKeys(menuFocus) {
                val repeatCount = it.nativeKeyEvent.repeatCount
                menuKey(shown, it.key, it.type, repeatCount, menuFocused, pauseCurtain::resume) {
                    focusManager.moveFocus(FocusDirection.Enter)
                }
            }
    }
    if (gameOver != null) {
        GameOverScreen(
            state = gameOver,
            onReplay = onReplay,
            onMenu = onBack,
            modifier = menuModifier(Menu.GAME_OVER),
            labels = labels,
            onCopy = onCopy,
        )
    } else {
        Game(
            uiState = uiState,
            tooSmallToPlay = tooSmallToPlay,
            onControl = onControl,
            onPause = onPause,
            focus = gameFocus,
            curtain = { pauseCurtain.curtain },
            underCard = menu == Menu.PAUSE,
            labels = labels,
        )
        // Over the game, once the curtain hides the board.
        if (menu == Menu.PAUSE) {
            PausedCard(
                uiState = uiState,
                onResume = pauseCurtain::resume,
                onMenu = onBack,
                modifier = menuModifier(Menu.PAUSE),
            )
        }
    }
}

/** The card of the pause, with the score, level and lines of the game given by [uiState]. */
@Composable
private fun PausedCard(
    uiState: () -> GameUiState,
    onResume: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val side by rememberPart(uiState, GameUiState::sideColumn)
    PauseCard(
        score = side.score,
        level = side.level,
        lines = side.lines,
        onResume = onResume,
        onMenu = onMenu,
        modifier = modifier,
    )
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
 *
 * The [curtain] of the pause covers the board and the next piece; [underCard], under the card of the pause, TalkBack
 * reads nothing of the game, and neither the focus nor a key of the system goes into it.
 */
@Composable
private fun Game(
    uiState: () -> GameUiState,
    tooSmallToPlay: Boolean,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    focus: FocusRequester,
    curtain: () -> PauseCurtain?,
    underCard: Boolean,
    labels: GameLabels,
    modifier: Modifier = Modifier,
) {
    // Before it is beaten, the best changes once, when the results give it; then it follows the score.
    val best by rememberPart(uiState) { it.shownBest() }
    val leftHanded by rememberPart(uiState) { it.leftHanded }
    val controls = rememberHeldControls(onControl)
    // The buttons still held are let go as the game pauses: it stays on screen, under the curtain.
    LaunchedEffect(controls) {
        snapshotFlow { uiState().paused }.filter { it }.collect { controls.cancel() }
    }
    Column(
        modifier = modifier
            .then(if (underCard) UnderCard else Modifier)
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
        GameTopBar(best = best, onPause = onPause, pauseEnabled = !tooSmallToPlay, labels = labels)
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
                // Until the settings answer, the room of the keys is kept, but no key is shown: none moves once they answer.
                val known = leftHanded
                val layout = remember(width, height, density, fontScale, known) {
                    gameLayout(width, height, density, fontScale, known == true)
                }
                Play(
                    uiState = uiState,
                    layout = layout,
                    controls = controls,
                    keysShown = known != null,
                    curtain = curtain,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * The game under the card of the pause: TalkBack reads nothing of it, and the focus, moved by the arrows of the
 * system, neither takes it nor goes into it, but goes to the buttons of the card.
 */
private val UnderCard: Modifier = Modifier
    .clearAndSetSemantics {}
    .focusProperties {
        canFocus = false
        onEnter = { cancelFocusChange() }
    }

/** The cells of the curtain of the pause: empty ones only. */
private val EmptyCells = Board.empty()

// Lambdas made once: the curtain keeps its painters from one composition to the next (BoardCanvas).
private val EmptyBoard: () -> Board = { EmptyCells }
private val NoPiece: () -> Piece? = { null }

/** What the side column shows: it changes when a piece locks, not when it moves. */
@Immutable
private data class SideColumn(
    val score: Points,
    val level: Level,
    val lines: Lines,
    val next: PieceCells,
    val bestFill: Float?,
)

private fun GameUiState.sideColumn(): SideColumn = SideColumn(score, level, lines, next, bestBarFill(score, best))

/**
 * The board, the side column and, when [keysShown], the touch controls of [controls], each where [layout] puts it.
 * The score column scrolls when its panels do not fit, as with an enlarged font, rather than pass under the screen or
 * over the controls. The board reads the board and the piece only while drawing, as the [curtain] of the pause, of empty
 * cells, that covers the board and the next piece. In Couleurs, the effects of the game ([FeedbackEffects], ADR 0024)
 * are read while drawing too: on the board, its sticker « +points » and the icons of B and A; only the score that counts
 * up recomposes, its text alone.
 */
@Composable
private fun Play(
    uiState: () -> GameUiState,
    layout: GameLayout,
    controls: HeldControls,
    keysShown: Boolean,
    curtain: () -> PauseCurtain?,
    modifier: Modifier = Modifier,
) {
    val scoreScroll = rememberScrollState()
    val side by rememberPart(uiState, GameUiState::sideColumn)
    val effects = rememberFeedbackEffects(remember(uiState) { { uiState().feedback } })
    val shownScore = remember(effects, uiState) { effects?.let { shown -> { shown.shownScore(uiState().score) } } }
    PlacedParts(modifier = modifier) {
        // First, under the board: its background and frame, around its cells.
        BoardFrame(modifier = Modifier.layoutId(layout.boardFrame))
        BoardCanvas(
            board = { uiState().board },
            piece = { uiState().piece },
            modifier = Modifier.placedAt(layout.board, ReadingOrder.BOARD),
            effects = effects?.board,
        )
        // Over the board, its sticker « +points », under the curtain of the pause, which hides it with the board.
        if (effects != null) PointsSticker(sticker = effects::sticker, modifier = Modifier.layoutId(layout.board))
        // Over the board, the curtain: a board of empty cells, of which only the rows it covers are drawn.
        BoardCanvas(
            board = EmptyBoard,
            piece = NoPiece,
            modifier = Modifier.layoutId(layout.board).drawnAsCurtain(curtain, EmptyCells.rows),
        )
        val nextPiece = layout.nextPiece
        val scores = Modifier.placedAt(layout.sidePanel, ReadingOrder.SCORES).verticalScroll(scoreScroll)
        // Over the next piece, the curtain in the colour of its panel, where empty cells are not drawn.
        val nextCurtain = Modifier.hiddenByCurtain(curtain, SidePanelSizes.PREVIEW_ROWS, BloxTheme.palette.panel)
        when {
            layout.scoresAbove -> ScoreGrid(
                score = side.score,
                level = side.level,
                lines = side.lines,
                next = side.next,
                bestFill = side.bestFill,
                cellSize = layout.cellSize,
                modifier = scores,
                previewModifier = nextCurtain,
                shownScore = shownScore,
            )

            nextPiece == null -> SidePanel(
                score = side.score,
                level = side.level,
                lines = side.lines,
                next = side.next,
                bestFill = side.bestFill,
                cellSize = layout.cellSize,
                modifier = scores,
                previewModifier = nextCurtain,
                shownScore = shownScore,
            )

            else -> {
                ScorePanels(
                    score = side.score,
                    level = side.level,
                    lines = side.lines,
                    bestFill = side.bestFill,
                    modifier = scores,
                    shownScore = shownScore,
                )
                NextPiecePanel(
                    next = side.next,
                    cellSize = layout.cellSize,
                    modifier = Modifier.placedAt(nextPiece, ReadingOrder.NEXT).wrapContentHeight(Alignment.Top),
                    previewModifier = nextCurtain,
                )
            }
        }
        if (keysShown) {
            val directionSpacing = with(LocalDensity.current) { layout.directionSpacing.toDp() }
            DirectionPad(
                controls = controls,
                modifier = Modifier.placedAt(layout.directions, ReadingOrder.DIRECTIONS),
                keys = layout.keys,
                spacing = directionSpacing,
            )
            RotationPad(
                controls = controls,
                modifier = Modifier.placedAt(layout.rotations, ReadingOrder.ROTATIONS),
                keys = layout.keys,
                leftHanded = layout.leftHanded,
                keyAngle = effects?.let { it::keyAngle } ?: NoKeyAngle,
            )
        }
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
 * are on the left, beside the score, and by their place alone they would be read first. The next piece comes before the
 * score in every layout, as in the side column, where it is on top.
 */
private enum class ReadingOrder {
    BOARD,
    NEXT,
    SCORES,
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

/** A game under way, for the previews: a stack, a T falling, an I next, a best to beat. */
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
    best = 98_760,
)

@Composable
private fun GameScreenPreview(
    kind: BloxThemeKind,
    uiState: GameUiState,
    labels: GameLabels = GameLabels.Classic,
) {
    BloxTheme(kind = kind) {
        GameScreen(
            uiState = { uiState },
            onBack = {},
            onReplay = {},
            onControl = {},
            onPause = {},
            onResume = {},
            labels = labels,
            onCopy = {},
        )
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

/** For a left handed player: B and A on the left, the arrows on the right. */
@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenLeftHandedPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState.copy(leftHanded = true))
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenPausedPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState.copy(paused = true))
}

// The curtain of the pause halfway down, in full effects (rows of empty cells) and in reduced ones (a fade).

/** The game of [previewState] paused, its curtain 6 ticks down with [effects], the card not shown yet. */
@Composable
private fun CurtainPreview(effects: BloxEffects) {
    BloxTheme(kind = BloxThemeKind.COLORFUL, effects = effects) {
        val paused = previewState.copy(paused = true)
        val curtain = remember {
            PauseCurtainState(paused = false, effects = effects).apply {
                follow(paused = true, effects = effects)
                advance(ticks = 6)
            }
        }
        GameScreen(
            uiState = { paused },
            onBack = {},
            onReplay = {},
            onControl = {},
            onPause = {},
            onResume = {},
            labels = GameLabels.Classic,
            onCopy = {},
            pauseCurtain = curtain,
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenCurtainPreview() {
    CurtainPreview(BloxEffects.Full)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenCurtainReducedPreview() {
    CurtainPreview(BloxEffects(reduced = true, forcedBySystem = false))
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

// The record beaten during the game: the sticker NOUVEAU RECORD and the score in the top bar, the best bar full (V28).

/** The game of [previewState], its score of 98 790 above the record of 98 760. */
private val newBestState = previewState.copy(score = Points(98_790), best = 98_760, bestBeaten = true)

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenNewRecordPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, newBestState)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenNewRecordOriginalPreview() {
    GameScreenPreview(BloxThemeKind.ORIGINAL, newBestState)
}

// The feedback of the game (V28): a T turned by A and locked, filling rows 18 and 19, 4 ticks later: its cells lit, A
// turning, the middle cells gone, the next ones swelling; then the same rows cleared 4 ticks before: the rows above
// coming down, the sticker « +600 » rising, the score counting.

/** The T that fills rows 18 and 19, locked on tick 100. */
private val previewLocked = Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), Position(6, 18))

/** The rows 18 and 19 full, under the stack of [previewState], once the T locked. */
private val previewFullRows = previewState.copy(
    board = Board.parse(
        """
        J.........
        JJJ..OO..L
        SSTTTTTTLL
        IIIIJZTSSL
        """,
    ),
    piece = null,
)

/** The feedback of the game of [previewFullRows] on each tick: the rows cleared on tick 118, for 600 points. */
private val previewFeedback =
    lineClearFeedback(
        previewLocked,
        listOf(18, 19),
        lockTick = 100,
        clearDelay = 18,
        Points(11_740),
        previewState.score,
    )

/** The game of [previewFullRows] 4 ticks after the lock that filled its rows. */
private val clearingState = previewFullRows.copy(score = Points(11_740), feedback = previewFeedback(104))

/** The game once the rows of [previewFullRows] were cleared, 4 ticks before. */
private val clearedState = previewState.copy(
    board = Board.parse(
        """
        J.........
        JJJ..OO..L
        """,
    ),
    piece = null,
    feedback = previewFeedback(122),
)

@Composable
private fun FeedbackPreview(
    uiState: GameUiState,
    effects: BloxEffects = BloxEffects.Full,
) {
    BloxTheme(kind = BloxThemeKind.COLORFUL, effects = effects) {
        GameScreen(
            uiState = { uiState },
            onBack = {},
            onReplay = {},
            onControl = {},
            onPause = {},
            onResume = {},
            labels = GameLabels.Classic,
            onCopy = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenClearingPreview() {
    FeedbackPreview(clearingState)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenClearingReducedPreview() {
    FeedbackPreview(clearingState, BloxEffects(reduced = true, forcedBySystem = false))
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenClearedPreview() {
    FeedbackPreview(clearedState)
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenClearedReducedPreview() {
    FeedbackPreview(clearedState, BloxEffects(reduced = true, forcedBySystem = false))
}

// A daily game (V39): GRAINE DU JOUR and the best of the day in the top bar, in the Colorful theme only (ADR 0026).

@Preview(widthDp = 360, heightDp = 760)
@Composable
internal fun GameScreenDailyPreview() {
    GameScreenPreview(BloxThemeKind.COLORFUL, previewState, GameLabels.Daily)
}
