package ch.magori.blox.core.board

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalTestApi::class)
internal class BoardCanvasTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    private var board by mutableStateOf(
        Board.parse(
            """
            J.........
            JJJ..OO..L
            SSTTTOO.LL
            IIII.ZZSSL
            """,
        ),
    )

    private var piece by mutableStateOf(
        Piece(PieceType.T, ClassicRotationSystem.spawnOrientation(PieceType.T), pivot = Position(column = 4, row = 2)),
    )
    private val moveDown = { piece = piece.moved(Direction.DOWN) }

    private val built = AtomicInteger()
    private val countingPainters: PaintersFactory = { variant, cellSize, background, density ->
        built.incrementAndGet()
        paintersFor(variant, cellSize, background, density)
    }

    /** What may change between two drawings, besides the piece. */
    private var width by mutableStateOf(240.dp)
    private var theme by mutableStateOf(BloxThemeKind.COLORFUL)
    private var darkSystem by mutableStateOf(false)
    private var background by mutableStateOf(Color.Black)

    /** How many times the density of the device the board is drawn at, for the same size in pixels. */
    private var densityScale by mutableFloatStateOf(1f)

    /** Changing it composes the board again from scratch, as a board shown for the first time. */
    private var generation by mutableIntStateOf(0)

    /**
     * The board and [piece] in [theme], in the mode [darkSystem], [width] wide, on [background], at [densityScale] times
     * the density of the device, drawn once.
     */
    private fun ComposeContext.showBoard() {
        setContent {
            val device = LocalDensity.current
            val density = Density(device.density * densityScale, device.fontScale)
            // The size in pixels of [width] × 480 dp on the device, whatever the density: only the density changes.
            val size = with(device) { DpSize(width, 480.dp).toSize() }
            CompositionLocalProvider(LocalDensity provides density) {
                BloxTheme(kind = theme, darkSystem = darkSystem) {
                    key(generation) {
                        BoardCanvas(
                            board = { board },
                            piece = { piece },
                            modifier = Modifier.size(with(density) { size.toDpSize() }),
                            background = background,
                            painters = countingPainters,
                            effects = effects,
                        )
                    }
                }
            }
        }
        drawnContent()
        recompositions.start()
    }

    @Test
    @DisplayName("the painters are built once while the piece moves")
    fun paintersAreBuiltOnceWhileThePieceMoves() = extension.use {
        // Given bubbles in the Colorful theme, in cells of 24 dp (240 dp over 10 columns): their shapes, gradients and
        // glosses are built with the painters
        showBoard()

        movePiece(times = 10, moveDown)

        assertThat(built.get()).isEqualTo(1)
    }

    @Test
    @DisplayName("the painters are built again when the density changes")
    fun paintersAreBuiltAgainWhenTheDensityChanges() = extension.use {
        showBoard()

        // The size in pixels stays the same: only the density changes, and with it the size of the cells in dp
        runOnIdle { densityScale = 2f }
        drawnContent()

        assertThat(built.get()).isEqualTo(2)
    }

    @Test
    @DisplayName("nothing recomposes while the piece moves")
    fun nothingRecomposesWhileThePieceMoves() = extension.use {
        showBoard()

        movePiece(times = 10, moveDown)

        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("a moved piece is drawn as on a board drawn with it from the start")
    fun aMovedPieceIsDrawnAsOnABoardDrawnWithItFromTheStart() = extension.use {
        showBoard()

        assertDrawnAsFresh(move = moveDown, composeAfresh = { generation++ })
    }

    @Test
    @DisplayName("the painters are built again when the size changes")
    fun paintersAreBuiltAgainWhenTheSizeChanges() = extension.use {
        showBoard()

        runOnIdle { width = 120.dp }
        drawnContent()

        assertThat(built.get()).isEqualTo(2)
    }

    @Test
    @DisplayName("the painters are built again when the theme changes")
    fun paintersAreBuiltAgainWhenTheThemeChanges() = extension.use {
        showBoard()

        // The background is given: only the theme changes.
        runOnIdle { theme = BloxThemeKind.ORIGINAL }
        drawnContent()

        assertThat(built.get()).isEqualTo(2)
    }

    @Test
    @DisplayName("the painters are built again when the variant changes")
    fun paintersAreBuiltAgainWhenTheVariantChanges() = extension.use {
        showBoard()

        // The theme and the background are given: only the variant changes, from the day to the night.
        runOnIdle { darkSystem = true }
        drawnContent()

        assertThat(built.get()).isEqualTo(2)
    }

    @Test
    @DisplayName("the painters are built again when the background changes")
    fun paintersAreBuiltAgainWhenTheBackgroundChanges() = extension.use {
        showBoard()

        runOnIdle { background = Color.White }
        drawnContent()

        assertThat(built.get()).isEqualTo(2)
    }

    /** How far the effects of [playingEffects] went, from 0 to [EFFECT_TICKS]. */
    private var effectTick by mutableIntStateOf(0)

    /**
     * Effects that play as [effectTick] goes on: the bottom row glows and its cells shrink, the rows above it come down
     * from a cell higher, and row 18 is lit.
     */
    private val playingEffects = object : BoardEffects {
        override fun rowLift(row: Int): Float = if (row < 19) 1f - effectTick.toFloat() / EFFECT_TICKS else 0f

        override fun rowGlows(row: Int): Boolean = row == 19

        override fun cellScale(
            column: Int,
            row: Int,
        ): Float = if (row == 19) 1f - effectTick.toFloat() / EFFECT_TICKS else 1f

        override fun cellLight(
            column: Int,
            row: Int,
        ): Float = if (row == 18) 0.5f else 0f
    }

    /** The effects the board is drawn with, none unless a test gives some. */
    private var effects: BoardEffects? by mutableStateOf(null)

    @Test
    @DisplayName("the painters are built once while effects play")
    fun paintersAreBuiltOnceWhileEffectsPlay() = extension.use {
        // Given the board drawn with effects that play
        effects = playingEffects
        showBoard()
        val first = drawnContent()

        // When they play to their end
        repeat(EFFECT_TICKS) {
            runOnIdle { effectTick++ }
            drawnContent()
        }

        // Then they were drawn, with the painters built once, and nothing recomposed
        assertThat(drawnContent().sameAs(first)).isFalse()
        assertThat(built.get()).isEqualTo(1)
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("a board whose effects are at rest is drawn as a board without effects")
    fun aBoardWhoseEffectsAreAtRestIsDrawnAsABoardWithoutEffects() = extension.use {
        // Given the board drawn without effects
        showBoard()
        val withoutEffects = drawnContent()

        // When it is drawn with effects at rest
        runOnIdle {
            effects = object : BoardEffects {}
            generation++
        }

        // Then
        assertThat(drawnContent().sameAs(withoutEffects)).isTrue()
    }

    /** The board once a T locks on top of its stack, one row higher. */
    private val higherBoard = Board.parse(
        """
        ...TTT....
        J...T.....
        JJJ..OO..L
        SSTTTOO.LL
        IIII.ZZSSL
        """,
    )

    @Test
    @DisplayName("the painters are not built again when a piece locks and the stack grows")
    fun paintersAreNotBuiltAgainWhenAPieceLocks() = extension.use {
        showBoard()

        runOnIdle { board = higherBoard }
        drawnContent()

        assertThat(built.get()).isEqualTo(1)
    }

    @Test
    @DisplayName("TalkBack reads the height of the stack on the board, in rows")
    fun talkBackReadsTheHeightOfTheStack() = extension.use {
        // Given a stack 4 rows high
        showBoard()

        // Then
        onNodeWithContentDescription("Plateau : pile de 4 rangées sur 20").assertExists()
    }

    @Test
    @DisplayName("TalkBack reads a stack of one row in the singular")
    fun talkBackReadsAStackOfOneRowInTheSingular() = extension.use {
        // Given
        board = Board.parse("IIII.ZZSSL")
        showBoard()

        // Then
        onNodeWithContentDescription("Plateau : pile de 1 rangée sur 20").assertExists()
    }

    @Test
    @DisplayName("TalkBack reads an empty board as empty")
    fun talkBackReadsAnEmptyBoardAsEmpty() = extension.use {
        // Given
        board = Board.empty()
        showBoard()

        // Then
        onNodeWithContentDescription("Plateau : vide").assertExists()
    }

    @Test
    @DisplayName("TalkBack reads the new height of the stack once a piece locks on top of it")
    fun talkBackReadsTheNewHeightOfTheStack() = extension.use {
        // Given a stack 4 rows high
        showBoard()

        // When a piece locks on top of it, one row higher
        runOnIdle { board = higherBoard }

        // Then
        onNodeWithContentDescription("Plateau : pile de 5 rangées sur 20").assertExists()
    }
}

/** The ticks the effects of the tests play for. */
private const val EFFECT_TICKS = 8
