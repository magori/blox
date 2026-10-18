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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.board.BoardCanvas
import ch.magori.blox.feature.game.controls.GameControls
import ch.magori.blox.feature.game.sidepanel.SidePanel

/** The 24 dp margins the controls are sized for (P2.5). */
private val ScreenMargin = 24.dp
private val SidePanelWidth = 112.dp
private val Gap = 16.dp

/**
 * The game screen of [viewModel]. The screen owns the frame clock (ADR 0012): each time it is
 * started, it tells the view model that frames start again, then hands it every frame.
 */
@Composable
internal fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.onFramesStarted()
            while (true) {
                withFrameNanos(viewModel::onFrame)
            }
        }
    }
    GameScreen(uiState = uiState, onBack = onBack, modifier = modifier)
}

/**
 * The game screen for [uiState]: the board, the side column and the controls. No logic here; the
 * controls report nothing yet, until the engine takes inputs (P1.12d).
 */
@Composable
internal fun GameScreen(
    uiState: GameUiState,
    onBack: () -> Unit,
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
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.feature_game_back), color = BloxTheme.palette.ink)
        }
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
        GameControls(onChange = {}, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/** A game under way, for the previews: a stack, a T falling, an I next. */
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
    score = 12_340,
    rotationSystem = ClassicRotationSystem,
)

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenOriginalPreview() {
    BloxTheme(kind = BloxThemeKind.ORIGINAL) {
        GameScreen(uiState = previewState, onBack = {})
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun GameScreenColorfulPreview() {
    BloxTheme(kind = BloxThemeKind.COLORFUL) {
        GameScreen(uiState = previewState, onBack = {})
    }
}
