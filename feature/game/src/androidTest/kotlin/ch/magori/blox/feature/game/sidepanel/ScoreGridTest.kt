package ch.magori.blox.feature.game.sidepanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import ch.magori.blox.core.board.next.nextPieceCells
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.GameLayout
import ch.magori.blox.feature.game.gameLayout
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.floor

private const val GRID = "score_grid"

/**
 * The score above the board, in the narrowest multi-window, against the height the layout reserves for it:
 * measured as each theme draws it, with the normal font and the font twice as large, as the system scales it.
 */
@OptIn(ExperimentalTestApi::class)
internal class ScoreGridTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @ParameterizedTest(name = "{0}, font × {1}")
    @CsvSource("ORIGINAL, 1", "ORIGINAL, 2", "COLORFUL, 1", "COLORFUL, 2")
    @DisplayName("the score above the board fits the height the layout reserves for it, in both themes, font enlarged")
    fun theScoreAboveTheBoardFitsTheHeightReservedForIt(
        kind: BloxThemeKind,
        fontScale: Float,
    ) = extension.use {
        lateinit var layout: GameLayout
        setContent {
            val density = Density(LocalDensity.current.density, fontScale)
            // The space of the game in a window of 220 × 640 dp: without the margins, the top bar and the gap under it.
            layout = with(density) {
                gameLayout(
                    width = floor((220 - 2 * 24) * this.density),
                    height = floor((640 - 2 * 24 - 48 - 16) * this.density),
                    density = this.density,
                    fontScale = fontScale,
                )
            }
            CompositionLocalProvider(LocalDensity provides density) {
                BloxTheme(kind = kind) {
                    Box {
                        ScoreGrid(
                            score = Points(999_999),
                            level = Level(29),
                            lines = Lines(999),
                            next = nextPieceCells(PieceType.I, ClassicRotationSystem),
                            cellSize = layout.cellSize,
                            modifier = Modifier
                                .requiredWidth(with(density) { layout.sidePanel.width.toDp() })
                                .wrapContentHeight(unbounded = true)
                                .testTag(GRID),
                        )
                    }
                }
            }
        }

        val measured = onNodeWithTag(GRID).fetchSemanticsNode().size.height

        assertThat(layout.scoresAbove).isTrue()
        assertThat(measured.toFloat()).isLessThanOrEqualTo(layout.sidePanel.height)
    }
}
