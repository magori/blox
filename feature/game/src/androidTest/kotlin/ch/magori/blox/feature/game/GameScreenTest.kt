package ch.magori.blox.feature.game

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.RecompositionRecorder
import ch.magori.blox.core.board.assertDrawnAsFresh
import ch.magori.blox.core.board.drawnContent
import ch.magori.blox.core.board.movePiece
import ch.magori.blox.core.board.next.NextPieceCells
import ch.magori.blox.core.board.next.nextPieceCells
import ch.magori.blox.core.board.next.pieceName
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.gameover.GameOverUiState
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalTestApi::class)
internal class GameScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    private var uiState by mutableStateOf(
        GameUiState(
            board = Board.parse(
                """
                JJJ..OO..L
                SSTTTOO.LL
                IIII.ZZSSL
                """,
            ),
            piece = Piece(
                PieceType.T,
                ClassicRotationSystem.spawnOrientation(PieceType.T),
                Position(column = 4, row = 2),
            ),
            next = nextPieceCells(PieceType.I, ClassicRotationSystem),
            level = Level(5),
            lines = Lines(52),
            score = Points(12_340),
        ),
    )

    /** Changing it composes the screen again from scratch, as a screen shown for the first time. */
    private var generation by mutableIntStateOf(0)

    /** Texts read from the resources by [showGameScreen], with [shownField] for the side column test. */
    private lateinit var resume: String
    private lateinit var replay: String
    private lateinit var fieldDescription: String
    private var shownField: ShownField = ShownField.SCORE

    private val moveDown = { uiState = uiState.copy(piece = uiState.piece?.moved(Direction.DOWN)) }

    /** The game screen for [uiState], drawn once, the size of the device or of [size], even larger than the device. */
    private fun ComposeContext.showGameScreen(size: DpSize? = null) {
        setContent {
            resume = stringResource(R.string.feature_game_pause_resume)
            replay = stringResource(R.string.feature_game_over_replay)
            fieldDescription = stringResource(
                R.string.feature_game_value_description,
                stringResource(shownField.label),
                shownField.spokenValue(),
            )
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                Box(modifier = if (size == null) Modifier else Modifier.requiredSize(size)) {
                    key(generation) {
                        GameScreen(
                            uiState = { uiState },
                            onBack = {},
                            onReplay = {},
                            onControl = {},
                            onPause = {},
                            onResume = {},
                        )
                    }
                }
            }
        }
        drawnContent()
        recompositions.start()
    }

    /** The record of the game, as the records give it, and whether the score beats it. */
    internal enum class RecordShown(val change: (GameUiState) -> GameUiState) {
        UNKNOWN({ it.copy(record = null) }),
        SHOWN({ it.copy(record = 98_760) }),
        BEATEN({ it.copy(record = 12_000, recordBeaten = true) }),
    }

    @ParameterizedTest
    @EnumSource(RecordShown::class)
    @DisplayName("nothing on the screen recomposes when only the piece moves")
    fun nothingOnTheScreenRecomposesWhenOnlyThePieceMoves(record: RecordShown) = extension.use {
        uiState = record.change(uiState)
        showGameScreen()

        movePiece(times = 10, moveDown)

        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("once the record is beaten, the top bar shows NOUVEAU RECORD and the score, read as the new record")
    fun onceTheRecordIsBeatenTheTopBarShowsTheNewRecord() = extension.use {
        // Given a game whose score of 12 340 is under the record
        uiState = uiState.copy(record = 12_360)
        showGameScreen()
        onNodeWithContentDescription("Record : 12360").assertIsDisplayed()

        // When the score beats it
        runOnIdle { uiState = uiState.copy(score = Points(12_380), recordBeaten = true) }

        // Then the sticker in place of the record, the score as its value: TalkBack reads them as one
        onNodeWithContentDescription("Nouveau record : 12380").assertIsDisplayed()
        onNodeWithContentDescription("Record : 12360").assertDoesNotExist()
    }

    @Test
    @DisplayName("the side column shows the next piece, then the score, the lines and the level, from the top")
    fun theSideColumnShowsTheNextPieceFirst() = extension.use {
        // Given a phone in portrait: the next piece in the side column
        showGameScreen(DpSize(360.dp, 760.dp))

        // When
        val tops = listOf(NEXT, SCORE, LINES, LEVEL).map { onNode(it).fetchSemanticsNode().boundsInRoot.top }

        // Then
        assertThat(tops).isSorted()
        assertThat(tops).doesNotHaveDuplicates()
    }

    @Test
    @DisplayName("the side column does not recompose while the piece moves")
    fun theSideColumnDoesNotRecomposeWhileThePieceMoves() = extension.use {
        // Given the screen of a game under way
        showGameScreen()

        // When the piece moves, each state bringing an equal but new next piece, as a state computed afresh would
        movePiece(times = 10) {
            moveDown()
            val next = uiState.next
            uiState = uiState.copy(next = NextPieceCells(next.type, next.cells.toSet(), next.columns, next.rows))
        }

        // Then
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("a moved piece is drawn on the screen as on a screen drawn with it from the start")
    fun aMovedPieceIsDrawnAsOnAScreenDrawnWithItFromTheStart() = extension.use {
        showGameScreen()

        assertDrawnAsFresh(move = moveDown, composeAfresh = { generation++ })
    }

    /** A value shown beside the board, read by TalkBack as its [label] and its [spokenValue]. */
    internal enum class ShownField(@param:StringRes val label: Int, val change: (GameUiState) -> GameUiState) {
        SCORE(R.string.feature_game_score, { it.copy(score = Points(12_380)) }),
        LEVEL(R.string.feature_game_level, { it.copy(level = Level(6)) }),
        LINES(R.string.feature_game_lines, { it.copy(lines = Lines(60)) }),
        NEXT(R.string.feature_game_next, { it.copy(next = nextPieceCells(PieceType.O, ClassicRotationSystem)) }),

        /** Read from the records once the game has started: null at first. */
        RECORD(R.string.feature_game_top_record, { it.copy(record = 98_760) }),
        ;

        /** The new value as TalkBack reads it, once [change] is made. */
        @Composable
        @ReadOnlyComposable
        fun spokenValue(): String = when (this) {
            SCORE -> "12380"
            LEVEL -> "6"
            LINES -> "60"
            NEXT -> pieceName(PieceType.O)
            RECORD -> "98760"
        }
    }

    @ParameterizedTest
    @EnumSource(ShownField::class)
    @DisplayName("the screen shows the new value of each field beside the board once it changes")
    fun theScreenShowsTheNewValueOfEachFieldOnceItChanges(field: ShownField) = extension.use {
        shownField = field
        showGameScreen()
        onNodeWithContentDescription(fieldDescription).assertDoesNotExist()

        runOnIdle { uiState = field.change(uiState) }

        onNodeWithContentDescription(fieldDescription).assertIsDisplayed()
    }

    @Test
    @DisplayName("the pause screen takes the place of the game while it is paused")
    fun thePauseScreenTakesThePlaceOfTheGameWhileItIsPaused() = extension.use {
        showGameScreen()

        runOnIdle { uiState = uiState.copy(paused = true) }
        onNodeWithText(resume).assertIsDisplayed()
        runOnIdle { uiState = uiState.copy(paused = false) }

        onNodeWithText(resume).assertDoesNotExist()
    }

    @Test
    @DisplayName("the game over screen takes the place of the game once it is over")
    fun theGameOverScreenTakesThePlaceOfTheGameOnceItIsOver() = extension.use {
        showGameScreen()

        runOnIdle { uiState = ShownScreen.GAME_OVER.change(uiState) }

        onNodeWithText(replay).assertIsDisplayed()
    }

    /** A screen shown in place of the game screen, or the game screen itself, once [change] is made. */
    internal enum class ShownScreen(val change: (GameUiState) -> GameUiState) {
        GAME({ it }),
        PAUSE({ it.copy(paused = true) }),
        GAME_OVER({
            it.copy(
                piece = null,
                gameOver = GameOverUiState(
                    score = Points(12_340),
                    lines = Lines(52),
                    level = Level(5),
                    fourLineClears = 1,
                    ticksPlayed = 3_600,
                    newRecord = false,
                ),
            )
        }),
    }

    @ParameterizedTest
    @EnumSource(ShownScreen::class)
    @DisplayName("every button of the screen is 48 dp wide and high at least")
    fun everyButtonIsLargeEnough(screen: ShownScreen) = extension.use {
        // Given
        uiState = screen.change(uiState)
        showGameScreen()

        // Then: 48 dp visible, a rule of BloX, stricter than WCAG 2.2 (2.5.8, 24 px) and than Android, whose
        // 48 dp is a touch area that may pass beyond what is drawn
        val buttons = onAllNodes(hasClickAction())
        val count = buttons.fetchSemanticsNodes().size
        assertThat(count).isGreaterThan(1)
        repeat(count) { index ->
            buttons[index].assertWidthIsAtLeast(
                BloxSizes.minTouchTarget,
            ).assertHeightIsAtLeast(BloxSizes.minTouchTarget)
        }
    }

    /**
     * A layout of the game screen in [size]: its parts, found by something they hold, in the order TalkBack must
     * read them, and the place of each in that order.
     */
    internal enum class ScreenLayout(val size: DpSize, val parts: List<SemanticsMatcher>, val order: List<Float>) {
        /** The next piece is in the side column, above the score: the column is read as one part. */
        PORTRAIT(
            DpSize(360.dp, 760.dp),
            listOf(BOARD, NEXT, LEFT, A),
            listOf(1f, 3f, 4f, 5f),
        ),

        /** The arrows on the left, beside the score; the next piece on the right of the board, read before the score. */
        LANDSCAPE(
            DpSize(800.dp, 360.dp),
            listOf(BOARD, NEXT, SCORE, LEFT, A),
            listOf(1f, 2f, 3f, 4f, 5f),
        ),
    }

    @ParameterizedTest
    @EnumSource(ScreenLayout::class)
    @DisplayName("TalkBack reads the board, the next piece, the score, the arrows, then B and A, each part as a whole")
    fun talkBackReadsThePartsInOrder(layout: ScreenLayout) = extension.use {
        // Given
        showGameScreen(layout.size)

        // When
        val parts = layout.parts.map { matcher ->
            placedPart(onNode(matcher, useUnmergedTree = true).fetchSemanticsNode())
        }

        // Then the order does not depend on where the layout puts the parts
        assertThat(parts.map { it.config[SemanticsProperties.TraversalIndex] }).isEqualTo(layout.order)
        assertThat(parts.map { it.config.getOrNull(SemanticsProperties.IsTraversalGroup) }).containsOnly(true)
        assertThat(parts.map { it.parent?.config?.getOrNull(SemanticsProperties.IsTraversalGroup) }).containsOnly(true)
    }

    @Test
    @DisplayName("TalkBack reads « B, rotation anti-horaire » and « A, rotation horaire »")
    fun talkBackReadsTheNamesOfBAndA() = extension.use {
        // Given
        showGameScreen()

        // Then the name of each key, which shows its pictogram only, as one button
        onNode(B).assertContentDescriptionEquals("B, rotation anti-horaire").assert(hasClickAction())
        onNode(A).assertContentDescriptionEquals("A, rotation horaire").assert(hasClickAction())
    }

    @Test
    @DisplayName("until the settings answer, where the keys stand is not known, and no key is shown")
    fun noKeyIsShownUntilTheSettingsAnswer() = extension.use {
        // Given settings that have not answered yet
        uiState = uiState.copy(leftHanded = null, running = false)
        showGameScreen()

        // Then the board shows, no key; once they answer, the keys come
        onNode(BOARD).assertExists()
        listOf(LEFT, B, A).forEach { onNode(it).assertDoesNotExist() }
        uiState = uiState.copy(leftHanded = true, running = true)
        listOf(LEFT, B, A).forEach { onNode(it).assertExists() }
    }

    @ParameterizedTest(name = "left handed: {0}")
    @ValueSource(booleans = [false, true])
    @DisplayName("the reading order stays arrows then B and A, left handed or not")
    fun theReadingOrderStaysArrowsThenBAndA(leftHanded: Boolean) = extension.use {
        // Given a left handed player, or not
        uiState = uiState.copy(leftHanded = leftHanded)
        showGameScreen()

        // When
        val arrows = placedPart(onNode(LEFT, useUnmergedTree = true).fetchSemanticsNode())
        val rotations = placedPart(onNode(A, useUnmergedTree = true).fetchSemanticsNode())
        val b = onNode(B, useUnmergedTree = true).fetchSemanticsNode()
        val a = onNode(A, useUnmergedTree = true).fetchSemanticsNode()

        // Then the groups are where the hand puts them, B and A mirrored too, A outside, for the left hand…
        if (leftHanded) {
            assertThat(rotations.boundsInRoot.right).isLessThanOrEqualTo(arrows.boundsInRoot.left)
            assertThat(a.boundsInRoot.right).isLessThanOrEqualTo(b.boundsInRoot.left)
        } else {
            assertThat(arrows.boundsInRoot.right).isLessThanOrEqualTo(rotations.boundsInRoot.left)
            assertThat(b.boundsInRoot.right).isLessThanOrEqualTo(a.boundsInRoot.left)
        }
        // …and read in the same order: the arrows, then B, then A
        assertThat(arrows.config[SemanticsProperties.TraversalIndex])
            .isLessThan(rotations.config[SemanticsProperties.TraversalIndex])
        assertThat(
            b.config[SemanticsProperties.TraversalIndex],
        ).isLessThan(a.config[SemanticsProperties.TraversalIndex])
    }

    /**
     * The part of the game screen that holds [node], or [node] itself: the first group that has a place in the order.
     */
    private fun placedPart(node: SemanticsNode): SemanticsNode {
        var part: SemanticsNode = node
        while (SemanticsProperties.TraversalIndex !in part.config ||
            part.config.getOrNull(SemanticsProperties.IsTraversalGroup) != true
        ) {
            part = checkNotNull(part.parent) { "No part of the screen holds $node" }
        }
        return part
    }

    private companion object {
        val BOARD = hasContentDescription("Plateau : pile de 3 rangées sur 20")
        val SCORE = hasContentDescription("Score : 12340")
        val NEXT = hasContentDescription("Suivante : pièce I")
        val LINES = hasContentDescription("Lignes : 52")
        val LEVEL = hasContentDescription("Niveau : 5")
        val LEFT = hasTestTag("control_left")
        val B = hasTestTag("control_b")
        val A = hasTestTag("control_a")
    }
}
