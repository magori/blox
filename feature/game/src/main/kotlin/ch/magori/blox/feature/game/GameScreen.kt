package ch.magori.blox.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import ch.magori.blox.feature.game.controls.GameControls
import ch.magori.blox.feature.game.gameover.GameOverScreen
import ch.magori.blox.feature.game.pause.PauseScreen
import ch.magori.blox.feature.game.sidepanel.SidePanel
import ch.magori.blox.feature.game.topbar.GameTopBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The 24 dp margins the controls are sized for (P2.5). */
private val ScreenMargin = 24.dp
private val SidePanelWidth = 112.dp
private val Gap = 16.dp

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    val backAction = backAction(uiState)
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
 * The game screen for [uiState]: the top bar ([onPause], the record), the board, the side column and
 * the controls, whose changes go to [onControl]. Paused, the pause screen in their place, the board
 * hidden: Reprendre is [onResume], Menu is [onBack]. Once the game is over, the game over screen,
 * which has priority (ADR 0011): Menu is [onBack], Rejouer is [onReplay]. No logic here.
 */
@Composable
internal fun GameScreen(
    uiState: GameUiState,
    onBack: () -> Unit,
    onReplay: () -> Unit,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gameOver = uiState.gameOver
    when {
        gameOver != null -> GameOverScreen(state = gameOver, onReplay = onReplay, onMenu = onBack, modifier = modifier)
        uiState.paused -> PauseScreen(onResume = onResume, onMenu = onBack, modifier = modifier)
        else -> Game(uiState = uiState, onControl = onControl, onPause = onPause, modifier = modifier)
    }
}

/** The game under way: the top bar, the board, the side column and the controls. */
@Composable
private fun Game(
    uiState: GameUiState,
    onControl: (ControlChange) -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BloxTheme.palette.page)
            .safeDrawingPadding()
            .padding(ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        GameTopBar(record = uiState.record, onPause = onPause)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Gap),
        ) {
            BoardCanvas(
                board = uiState.board,
                piece = uiState.piece,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SidePanel(
                score = uiState.score,
                level = uiState.level,
                lines = uiState.lines,
                next = uiState.next,
                rotationSystem = uiState.rotationSystem,
                modifier = Modifier.width(SidePanelWidth),
            )
        }
        GameControls(onChange = onControl, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
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
        GameScreen(uiState = uiState, onBack = {}, onReplay = {}, onControl = {}, onPause = {}, onResume = {})
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
