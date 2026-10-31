package ch.magori.blox.feature.game

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ch.magori.blox.core.board.assertDrawnAsFresh
import ch.magori.blox.core.board.drawnContent
import ch.magori.blox.core.board.movePiece
import ch.magori.blox.core.board.next.pieceName
import ch.magori.blox.core.board.piece.PieceCells
import ch.magori.blox.core.board.piece.nextPieceCells
import ch.magori.blox.core.designsystem.test.RecompositionRecorder
import ch.magori.blox.core.designsystem.theme.BloxSizes
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.game.engine.GameEvent
import ch.magori.blox.core.game.model.Board
import ch.magori.blox.core.game.model.Direction
import ch.magori.blox.core.game.model.Level
import ch.magori.blox.core.game.model.Lines
import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Points
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.feature.game.controls.ControlButton
import ch.magori.blox.feature.game.controls.controlTestTag
import ch.magori.blox.feature.game.feedback.GameFeedback
import ch.magori.blox.feature.game.feedback.lineClearFeedback
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
    private lateinit var gameOverTitle: String
    private lateinit var fieldDescription: String
    private var shownField: ShownField = ShownField.SCORE

    private val moveDown = { uiState = uiState.copy(piece = uiState.piece?.moved(Direction.DOWN)) }

    /** The times Rejouer and Copier were pressed. */
    private var replays = 0
    private var copies = 0

    /** The times the pause button was pressed, and the changes of the controls. */
    private var pauses = 0
    private var controlChanges = 0

    /**
     * The game screen for [uiState], drawn once, the size of the device or of [size], even larger than the device, with
     * the [labels] of its variant.
     */
    private fun ComposeContext.showGameScreen(
        size: DpSize? = null,
        labels: GameLabels = GameLabels.Classic,
        kind: BloxThemeKind = BloxThemeKind.COLORFUL,
    ) {
        setContent {
            resume = stringResource(R.string.feature_game_pause_resume)
            replay = stringResource(R.string.feature_game_over_replay)
            gameOverTitle = stringResource(R.string.feature_game_over_title)
            fieldDescription = stringResource(
                R.string.feature_game_value_description,
                stringResource(shownField.label),
                shownField.spokenValue(),
            )
            BloxTheme(kind = kind) {
                Box(modifier = if (size == null) Modifier else Modifier.requiredSize(size)) {
                    key(generation) {
                        GameScreen(
                            uiState = { uiState },
                            onBack = {},
                            onReplay = { replays++ },
                            onControl = { controlChanges++ },
                            onPause = { pauses++ },
                            onResume = {},
                            labels = labels,
                            onCopy = { copies++ },
                        )
                    }
                }
            }
        }
        drawnContent()
        recompositions.start()
    }

    /** The best of the game, as the results give it, and whether the score beats it. */
    internal enum class BestShown(val change: (GameUiState) -> GameUiState) {
        UNKNOWN({ it.copy(best = null) }),
        SHOWN({ it.copy(best = 98_760) }),
        BEATEN({ it.copy(best = 12_000, bestBeaten = true) }),
    }

    @ParameterizedTest
    @EnumSource(BestShown::class)
    @DisplayName("nothing on the screen recomposes when only the piece moves")
    fun nothingOnTheScreenRecomposesWhenOnlyThePieceMoves(best: BestShown) = extension.use {
        uiState = best.change(uiState)
        showGameScreen()

        movePiece(times = 10, moveDown)

        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("nothing on the screen recomposes while the curtain of the pause comes down")
    fun nothingRecomposesWhileTheCurtainComesDown() = extension.use {
        // Given a game that pauses, the curtain starting to come down, its frames handed by hand
        showGameScreen()
        mainClock.autoAdvance = false
        runOnIdle { uiState = uiState.copy(paused = true) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        // When the curtain comes down, for half its 12 ticks
        recompositions.start()
        mainClock.advanceTimeBy(FRAME_MILLIS * 6)

        // Then it is only drawn again
        assertThat(recompositions.recomposed).isEmpty()
    }

    /**
     * The feedback of the game on [tick]: a T turned by A, then locked, on tick 100, filling row 19, cleared on tick 118,
     * the score going from 12 240 to the 12 340 of [uiState].
     */
    private fun feedbackOn(tick: Int): GameFeedback =
        lineClearFeedback(checkNotNull(uiState.piece), listOf(19), 100, 18, Points(12_240), Points(12_340))(tick)

    /** Plays the feedback of [feedbackOn] from tick [from] to tick [to], drawing each tick. */
    private fun ComposeContext.playFeedback(
        from: Int,
        to: Int,
    ) = (from..to).forEach { tick ->
        runOnIdle { uiState = uiState.copy(feedback = feedbackOn(tick)) }
        drawnContent()
    }

    @Test
    @DisplayName("nothing recomposes while an effect plays")
    fun nothingRecomposesWhileAnEffectPlays() = extension.use {
        // Given a T that turned and locked on tick 100, filling row 19
        uiState = uiState.copy(feedback = feedbackOn(100))
        showGameScreen()

        // When the light of the lock, the turn of A and the clear of the row play, up to its last tick
        playFeedback(from = 101, to = 117)

        // Then they are only drawn again
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("while the score counts, only its text recomposes")
    fun whileTheScoreCountsOnlyItsTextRecomposes() = extension.use {
        // Given row 19 cleared on tick 118, the score of 12 340 shown
        uiState = uiState.copy(feedback = feedbackOn(118))
        showGameScreen()

        // When the rows slide down, the sticker rises and the score counts, until they are over
        playFeedback(from = 119, to = 170)

        // Then only the score was composed again, as it counted: its value and the text it calls, once each time, and no
        // other value (level, lines, record), whose text is the same function
        val counts = recompositions.recomposed
            .filter { "feature.game" in it }
            .groupingBy { it.substringBefore(" (").substringAfterLast(".") }
            .eachCount()
        assertThat(counts.keys).containsExactlyInAnyOrder("ScoreValue", "ValueText", "shrinkingFrom")
        assertThat(counts.getValue("ValueText")).isEqualTo(counts.getValue("ScoreValue"))
    }

    @Test
    @DisplayName("nothing but the counting score recomposes while the stack goes out and the confetti fall")
    fun nothingButTheCountingScoreRecomposesDuringTheGameOver() = extension.use {
        // Given a game that ends with a new record, its frames handed by hand
        showGameScreen()
        mainClock.autoAdvance = false
        val over = ShownScreen.GAME_OVER.change(uiState)
        runOnIdle { uiState = over.copy(gameOver = over.gameOver?.copy(newBest = true)) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        // When its stack goes out, for 30 of its 40 ticks
        recompositions.start()
        mainClock.advanceTimeBy(FRAME_MILLIS * 30)

        // Then it is only drawn again
        assertThat(recompositions.recomposed.filter { "feature.game" in it }).isEmpty()

        // And once the card has shown, while its score counts up (30 ticks) and the confetti fall (90), to their end
        mainClock.advanceTimeBy(FRAME_MILLIS * 16)
        onNodeWithText(replay).assertExists()
        recompositions.start()
        mainClock.advanceTimeBy(FRAME_MILLIS * 100)

        // Only the score was composed again, as it counted: its text, once for each value it took, 30 at most
        val counts = recompositions.recomposed
            .filter { "feature.game" in it }
            .groupingBy { it.substringBefore(" (").substringAfterLast(".") }
            .eachCount()
        assertThat(counts.keys).isSubsetOf("CountingScore", "ValueText", "shrinkingFrom")
        assertThat(counts.getValue("CountingScore")).isBetween(1, 30)
    }

    @Test
    @DisplayName("while the stack goes out, the game takes no touch: neither its pause button nor its keys")
    fun whileTheStackGoesOutTheGameTakesNoTouch() = extension.use {
        // Given a game under way, where its pause button and its left key stand
        showGameScreen()
        val pauseButton = onNodeWithTag("game_pause").fetchSemanticsNode().boundsInRoot.center
        val leftKey = onNode(LEFT).fetchSemanticsNode().boundsInRoot.center

        // When it ends, and the player touches them while its stack goes out
        mainClock.autoAdvance = false
        runOnIdle { uiState = ShownScreen.GAME_OVER.change(uiState) }
        mainClock.advanceTimeBy(FRAME_MILLIS * 4)
        onRoot().performTouchInput { click(pauseButton) }
        onRoot().performTouchInput { click(leftKey) }
        mainClock.advanceTimeBy(FRAME_MILLIS * 4)

        // Then neither the pause nor a control was asked for, the card not shown yet
        onNodeWithText(replay).assertDoesNotExist()
        assertThat(pauses).isZero()
        assertThat(controlChanges).isZero()
    }

    @Test
    @DisplayName("the icon that turns is the one of A, the key that turned the piece, and B stays still")
    fun theIconThatTurnsIsTheOneOfTheKeyThatTurnedThePiece() = extension.use {
        // Given A turned the piece on tick 100
        uiState = uiState.copy(feedback = feedbackOn(100))
        showGameScreen()
        val aOnTheTurn = keyImage(ControlButton.A)
        val bOnTheTurn = keyImage(ControlButton.B)

        // When the turn goes on, 3 ticks later
        runOnIdle { uiState = uiState.copy(feedback = feedbackOn(103)) }

        // Then
        assertThat(keyImage(ControlButton.A).sameAs(aOnTheTurn)).isFalse()
        assertThat(keyImage(ControlButton.B).sameAs(bOnTheTurn)).isTrue()
    }

    @ParameterizedTest
    @EnumSource(BloxThemeKind::class)
    @DisplayName("a level up announces the new level to TalkBack")
    fun aLevelUpAnnouncesTheNewLevelToTalkBack(kind: BloxThemeKind) = extension.use {
        // Given a game at level 5
        showGameScreen(kind = kind)

        // When it reaches level 6
        runOnIdle {
            uiState = uiState.copy(
                level = Level(6),
                feedback = GameFeedback().after(100, listOf(GameEvent.LevelUp(Level(6))), null, Points(0), Points(0)),
            )
        }

        // Then the level is a live region, which TalkBack reads as it changes, now at level 6; the only one of the screen:
        // neither the score nor the lines, which change at each clear, are announced
        onNodeWithContentDescription("Niveau : 6")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertCountEquals(1)
    }

    @Test
    @DisplayName("nothing recomposes while the level, the danger and the beaten record play")
    fun nothingRecomposesWhileTheLevelTheDangerAndTheBeatenRecordPlay() = extension.use {
        // Given a game that gained a level, entered the danger and beat the record on tick 100
        val events = listOf(GameEvent.LevelUp(Level(6)), GameEvent.StackDanger(on = true))
        val started = GameFeedback().after(100, events, null, Points(0), Points(0)).withBestBeaten()
        uiState = uiState.copy(level = Level(6), best = 12_000, bestBeaten = true, feedback = started)
        showGameScreen()

        // When the level tilts and is framed, the rows in danger breathe and the record bounces, for 2 seconds
        (101..220).forEach { tick ->
            val playing = started.after(tick, emptyList(), null, Points(0), Points(0))
            runOnIdle { uiState = uiState.copy(feedback = playing) }
            drawnContent()
        }

        // Then they are only drawn again
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("nothing recomposes while the iridescent border and the sticker of four lines play")
    fun nothingRecomposesWhileFourLinesPlay() = extension.use {
        // Given four lines cleared on tick 118, at the same score so that it does not count up
        val fourLines = lineClearFeedback(
            checkNotNull(uiState.piece),
            listOf(16, 17, 18, 19),
            100,
            18,
            uiState.score,
            uiState.score,
        )
        uiState = uiState.copy(feedback = fourLines(118))
        showGameScreen()

        // When the reflection goes around the frame and the sticker « 4 LIGNES » rises, until they are over
        (119..170).forEach { tick ->
            runOnIdle { uiState = uiState.copy(feedback = fourLines(tick)) }
            drawnContent()
        }

        // Then they are only drawn again
        assertThat(recompositions.recomposed).isEmpty()
    }

    @Test
    @DisplayName("the points sticker is not read by TalkBack")
    fun thePointsStickerIsNotReadByTalkBack() = extension.use {
        // Given row 19 cleared on tick 118 for 100 points, the sticker shown
        uiState = uiState.copy(feedback = feedbackOn(120))
        showGameScreen()

        // Then no node says the points of the sticker
        onAllNodesWithText("+100", substring = true).assertCountEquals(0)
        onAllNodesWithContentDescription("+100", substring = true).assertCountEquals(0)
    }

    /** The key of [button] as it is drawn. */
    private fun ComposeContext.keyImage(button: ControlButton): Bitmap =
        onNodeWithTag(controlTestTag(button)).captureToImage().asAndroidBitmap()

    @Test
    @DisplayName("the effects are drawn over the game, and gone once over")
    fun theEffectsAreDrawnOverTheGameAndGoneOnceOver() = extension.use {
        // Given the screen without effects
        showGameScreen()
        val atRest = drawnContent()

        // When a T turns and locks, filling row 19, then once its effects are over
        runOnIdle { uiState = uiState.copy(feedback = feedbackOn(105)) }
        val playing = drawnContent()
        runOnIdle { uiState = uiState.copy(feedback = feedbackOn(200)) }

        // Then
        assertThat(playing.sameAs(atRest)).isFalse()
        assertThat(drawnContent().sameAs(atRest)).isTrue()
    }

    @Test
    @DisplayName("once the record is beaten, the top bar shows NOUVEAU RECORD and the score, read as the new record")
    fun onceTheRecordIsBeatenTheTopBarShowsTheNewRecord() = extension.use {
        // Given a game whose score of 12 340 is under the record
        uiState = uiState.copy(best = 12_360)
        showGameScreen()
        onNodeWithContentDescription("Record : 12360").assertIsDisplayed()

        // When the score beats it
        runOnIdle { uiState = uiState.copy(score = Points(12_380), bestBeaten = true) }

        // Then the sticker in place of the record, the score as its value: TalkBack reads them as one
        onNodeWithContentDescription("Nouveau record : 12380").assertIsDisplayed()
        onNodeWithContentDescription("Record : 12360").assertDoesNotExist()
    }

    @Test
    @DisplayName("a daily game is titled GRAINE DU JOUR and shows the best of the day in place of the record")
    fun aDailyGameIsTitledGraineDuJourAndShowsTheBestOfTheDay() = extension.use {
        // Given a daily game, whose best of the day is 12 360
        uiState = uiState.copy(best = 12_360)

        // When its screen shows
        showGameScreen(labels = GameLabels.Daily)

        // Then its title, read as a heading, and its best, read as one
        onNode(hasText("GRAINE DU JOUR") and isHeading()).assertIsDisplayed()
        onNodeWithText("MODE CLASSIQUE").assertDoesNotExist()
        onNodeWithContentDescription("Meilleur : 12360").assertIsDisplayed()
        onNodeWithContentDescription("Record : 12360").assertDoesNotExist()
    }

    @Test
    @DisplayName("once the best of the day is beaten, the top bar shows NOUVEAU MEILLEUR and the score, read as such")
    fun onceTheBestOfTheDayIsBeatenTheTopBarShowsTheNewBest() = extension.use {
        // Given a daily game whose score of 12 340 is under the best of the day
        uiState = uiState.copy(best = 12_360)
        showGameScreen(labels = GameLabels.Daily)

        // When the score beats it
        runOnIdle { uiState = uiState.copy(score = Points(12_380), bestBeaten = true) }

        // Then the sticker in place of the best, the score as its value
        onNodeWithContentDescription("Nouveau meilleur : 12380").assertIsDisplayed()
        onNodeWithContentDescription("Meilleur : 12360").assertDoesNotExist()
    }

    @Test
    @DisplayName(
        "the game over of a daily game announces MEILLEUR DU JOUR; Rejouer and Copier, read Copier mon score, act",
    )
    fun theGameOverOfADailyGameAnnouncesTheBestAndOffersToCopy() = extension.use {
        // Given a daily game over, whose score beat the best of the day
        uiState = ShownScreen.GAME_OVER.change(uiState).let { it.copy(gameOver = it.gameOver?.copy(newBest = true)) }
        showGameScreen(labels = GameLabels.Daily)

        // When the player presses Rejouer, then Copier
        onNodeWithText("MEILLEUR DU JOUR").assertIsDisplayed()
        onNodeWithText("NOUVEAU RECORD").assertDoesNotExist()
        onNodeWithText(replay).performScrollTo().performClick()
        // Copier shows its short label; TalkBack reads what it does, as one button
        onNodeWithText("Copier").assertExists()
        onNodeWithContentDescription("Copier mon score").assert(hasClickAction()).performScrollTo().performClick()

        // Then each one did its part, once
        assertThat(replays).isEqualTo(1)
        assertThat(copies).isEqualTo(1)
    }

    @Test
    @DisplayName("the game over of a classic game offers no Copier")
    fun theGameOverOfAClassicGameOffersNoCopy() = extension.use {
        // Given a classic game over, whose score beat the record
        uiState = ShownScreen.GAME_OVER.change(uiState).let { it.copy(gameOver = it.gameOver?.copy(newBest = true)) }

        // When its game over shows
        showGameScreen()

        // Then it announces the record, offers Rejouer, and nothing to copy
        onNodeWithText("NOUVEAU RECORD").assertIsDisplayed()
        onNodeWithText(replay).assertExists()
        onNodeWithText("Copier").assertDoesNotExist()
        onNodeWithContentDescription("Copier mon score").assertDoesNotExist()
    }

    @Test
    @DisplayName("the game over of a daily game whose results could not be written offers no Copier")
    fun theGameOverOfADailyGameNotWrittenOffersNoCopy() = extension.use {
        // Given a daily game over, whose try could not be written: the results of the day do not hold it
        uiState = ShownScreen.GAME_OVER.change(uiState)
            .let { it.copy(gameOver = it.gameOver?.copy(resultsKept = false)) }

        // When its game over shows
        showGameScreen(labels = GameLabels.Daily)

        // Then it offers Rejouer, and nothing to copy, which would copy the results without this game
        onNodeWithText(replay).assertExists()
        onNodeWithText("Copier").assertDoesNotExist()
        onNodeWithContentDescription("Copier mon score").assertDoesNotExist()
    }

    @Test
    @DisplayName("the game over of a daily game not counted tells a practice game, and a counted one does not")
    fun theGameOverOfADailyGameNotCountedTellsAPracticeGame() = extension.use {
        // Given a daily game over whose try was ignored, its date already passed
        uiState = ShownScreen.GAME_OVER.change(uiState)
            .let { it.copy(gameOver = it.gameOver?.copy(resultsKept = false, practice = true)) }

        // When its game over shows
        showGameScreen(labels = GameLabels.Daily)

        // Then it tells the player the game was only practice
        onNodeWithText("Partie d’entraînement").assertIsDisplayed()

        // And a counted daily game over says nothing of it
        uiState = uiState.copy(gameOver = uiState.gameOver?.copy(resultsKept = true, practice = false))
        waitForIdle()
        onNodeWithText("Partie d’entraînement").assertDoesNotExist()
    }

    /**
     * A game over of 12 340 points, of a variant ([labels]), and the best before it, beaten or not: the line [shown]
     * under the score, and what TalkBack reads of it after the score ([spoken]); none without a best before.
     */
    internal enum class BestRecalled(
        val labels: GameLabels,
        val newBest: Boolean,
        val previousBest: Int,
        val shown: String?,
        val spoken: String?,
    ) {
        RECORD_BEATEN(GameLabels.Classic, newBest = true, 9_800, "Ancien record 009800", "Ancien record : 9800"),
        RECORD_NOT_BEATEN(GameLabels.Classic, newBest = false, 98_760, "Record 098760", "Record : 98760"),
        DAILY_BEST_BEATEN(GameLabels.Daily, newBest = true, 9_800, "Ancien meilleur 009800", "Ancien meilleur : 9800"),
        DAILY_BEST_NOT_BEATEN(GameLabels.Daily, newBest = false, 98_760, "Meilleur 098760", "Meilleur : 98760"),
        FIRST_GAME(GameLabels.Classic, newBest = true, 0, shown = null, spoken = null),
        FIRST_TRY_OF_THE_DAY(GameLabels.Daily, newBest = true, 0, shown = null, spoken = null),
    }

    @ParameterizedTest
    @EnumSource(BestRecalled::class)
    @DisplayName("the game over recalls the best before the game under the score, read with it; nothing without one")
    fun theGameOverRecallsTheBestBeforeTheGame(case: BestRecalled) = extension.use {
        // Given a game over, and the best before it
        uiState = ShownScreen.GAME_OVER.change(uiState)
            .let { it.copy(gameOver = it.gameOver?.copy(newBest = case.newBest, previousBest = case.previousBest)) }

        // When its game over shows
        showGameScreen(labels = case.labels)

        // Then TalkBack reads the score, then the best: the one beaten, or the one still to beat
        onNodeWithContentDescription(listOfNotNull("Score : 12340", case.spoken).joinToString()).assertIsDisplayed()
        // And the line shows under the score; without a best before, no line, neither label nor zeros, on the card of
        // the game over (the game under it, which TalkBack does not read, keeps its own values)
        val onCard = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, gameOverTitle))
        if (case.shown != null) {
            onNodeWithText(case.shown, useUnmergedTree = true).assertIsDisplayed()
        } else {
            listOf("Ancien", "Record", "Meilleur", "000000").forEach {
                onNode(hasText(it, substring = true) and onCard, useUnmergedTree = true).assertDoesNotExist()
            }
        }
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
            uiState = uiState.copy(next = PieceCells(next.type, next.cells.toSet(), next.columns, next.rows))
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
    internal enum class ShownField(
        @param:StringRes val label: Int,
        val change: (GameUiState) -> GameUiState,
        val labels: GameLabels = GameLabels.Classic,
    ) {
        SCORE(R.string.feature_game_score, { it.copy(score = Points(12_380)) }),
        LEVEL(R.string.feature_game_level, { it.copy(level = Level(6)) }),
        LINES(R.string.feature_game_lines, { it.copy(lines = Lines(60)) }),
        NEXT(R.string.feature_game_next, { it.copy(next = nextPieceCells(PieceType.O, ClassicRotationSystem)) }),

        /** Read from the records once the game has started: null at first. */
        RECORD(R.string.feature_game_top_record, { it.copy(best = 98_760) }),

        /** The best of the day, in place of the record of a daily game. */
        DAILY_BEST(R.string.feature_game_daily_best, { it.copy(best = 98_760) }, GameLabels.Daily),
        ;

        /** The new value as TalkBack reads it, once [change] is made. */
        @Composable
        @ReadOnlyComposable
        fun spokenValue(): String = when (this) {
            SCORE -> "12380"
            LEVEL -> "6"
            LINES -> "60"
            NEXT -> pieceName(PieceType.O)
            RECORD, DAILY_BEST -> "98760"
        }
    }

    @ParameterizedTest
    @EnumSource(ShownField::class)
    @DisplayName("the screen shows the new value of each field beside the board once it changes")
    fun theScreenShowsTheNewValueOfEachFieldOnceItChanges(field: ShownField) = extension.use {
        shownField = field
        showGameScreen(labels = field.labels)
        onNodeWithContentDescription(fieldDescription).assertDoesNotExist()

        runOnIdle { uiState = field.change(uiState) }

        onNodeWithContentDescription(fieldDescription).assertIsDisplayed()
    }

    @Test
    @DisplayName("the card of the pause shows over the game while it is paused")
    fun theCardOfThePauseShowsOverTheGameWhileItIsPaused() = extension.use {
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
                    newBest = false,
                    resultsKept = true,
                    previousBest = 0,
                    practice = false,
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
        /** The duration of a frame of the clock of the test. */
        const val FRAME_MILLIS = 16L

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
