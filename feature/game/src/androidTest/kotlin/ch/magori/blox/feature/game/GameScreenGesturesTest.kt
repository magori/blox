package ch.magori.blox.feature.game

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import de.mannodermaus.junit5.condition.EnabledOnSdkVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.math.roundToInt

/** The height of exclusion the system honours on each edge; it ignores what goes past. */
private const val MAX_EXCLUSION_DP = 200f

/**
 * The controls against the system gestures. In gesture navigation, the system takes a swipe from the
 * left or right edge as Back, over a zone that can be wider than the 16 dp margins of the screen: a
 * thumb on ◀ moving to ▼ would then cancel the fingers, and the game would pause. The controls exclude
 * their rectangles from these gestures (API 29 and above, where the gestures exist).
 *
 * This reads what the screen asks of the system, not what the system does with it: it does not prove
 * that Back is blocked. The system ignores, without a word, what goes past 200 dp on an edge: each
 * rectangle is checked here, the layout of the groups on their edges by `GameLayoutTest`.
 */
@OptIn(ExperimentalTestApi::class)
internal class GameScreenGesturesTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val uiState = GameUiState(
        board = Board.parse(
            """
            IIII.ZZSSL
            """,
        ),
        piece = null,
        next = nextPieceCells(PieceType.I, ClassicRotationSystem),
        level = Level(0),
        lines = Lines(0),
        score = Points(0),
    )

    private lateinit var view: View
    private lateinit var controls: List<String>
    private var pixelsPerDp = 0f

    @Test
    @RequiresApi(Build.VERSION_CODES.Q)
    @EnabledOnSdkVersion(from = Build.VERSION_CODES.Q)
    @DisplayName(
        "every control is excluded from the system gestures, by rectangles of 200 dp high at most that the system honours",
    )
    fun everyControlIsExcludedByRectanglesTheSystemHonours() = extension.use {
        showGameScreen()

        val excluded = runOnIdle { view.systemGestureExclusionRects.toList() }

        controls.forEach { control ->
            val bounds = boundsOf(control)
            assertThat(excluded.any { it.contains(bounds) })
                .describedAs("%s at %s within one of %s", control, bounds, excluded)
                .isTrue()
        }
        excluded.forEach { rect ->
            assertThat(rect.height() / pixelsPerDp)
                .describedAs("height in dp of %s", rect)
                .isLessThanOrEqualTo(MAX_EXCLUSION_DP)
        }
    }

    private fun ComposeContext.showGameScreen() {
        setContent {
            view = LocalView.current
            pixelsPerDp = LocalDensity.current.density
            controls = listOf(
                R.string.feature_game_control_left,
                R.string.feature_game_control_down,
                R.string.feature_game_control_right,
                R.string.feature_game_control_b,
                R.string.feature_game_control_a,
            ).map { stringResource(it) }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
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
        waitForIdle()
    }

    /** The bounds of the control described as [description], in pixels of the view, rounded. */
    private fun ComposeContext.boundsOf(description: String): Rect {
        val bounds = onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        return Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
    }
}
