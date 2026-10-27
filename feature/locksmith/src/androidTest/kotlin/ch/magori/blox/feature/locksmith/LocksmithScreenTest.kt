package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.board.RecompositionRecorder
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.designsystem.theme.BloxEffects
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.model.Rotation
import ch.magori.blox.core.locksmith.Hint
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockEvent
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.PadlockUpdate
import ch.magori.blox.core.locksmith.placing
import ch.magori.blox.core.locksmith.solved
import ch.magori.blox.core.locksmith.turnedTo
import ch.magori.blox.feature.locksmith.navigation.LocksmithRoute
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class LocksmithScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @JvmField
    @RegisterExtension
    val recompositions = RecompositionRecorder()

    private val start = PadlockGame.start(Padlock.generate(level = 1, seed = 7))

    @Test
    @DisplayName("on the card of an open padlock, Serrure suivante goes to the next padlock")
    fun onTheCardSerrureSuivanteGoesToTheNextPadlock() = extension.use {
        // Given
        val touched = mutableListOf<String>()
        showLocksmith(start.solved(), touched)

        // When
        onNodeWithText(string(R.string.feature_locksmith_next)).performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(touched).containsExactly("next") }
    }

    @Test
    @DisplayName("on the card of an open padlock, Rejouer cette serrure restarts the padlock")
    fun onTheCardRejouerRestartsThePadlock() = extension.use {
        // Given
        val touched = mutableListOf<String>()
        showLocksmith(start.solved(), touched)

        // When
        onNodeWithText(string(R.string.feature_locksmith_replay)).performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(touched).containsExactly("restart") }
    }

    @Test
    @DisplayName("once the padlock is open, neither Place, nor Hint, nor the tiles are left")
    fun onceOpenNeitherPlaceNorHintNorTilesAreLeft() = extension.use {
        // Given
        showLocksmith(start.solved())

        // Then
        onNodeWithText(string(R.string.feature_locksmith_place)).assertDoesNotExist()
        onNodeWithText(string(R.string.feature_locksmith_hint)).assertDoesNotExist()
        start.given.forEach { type ->
            onNode(hasContentDescription(string(R.string.feature_locksmith_tile, type.name, ""), substring = true))
                .assertDoesNotExist()
        }
    }

    @Test
    @DisplayName("while the padlock is closed, the card is not shown")
    fun whileClosedTheCardIsNotShown() = extension.use {
        // Given
        showLocksmith(start)

        // Then
        onNode(hasTextExactly(string(R.string.feature_locksmith_open_title))).assertDoesNotExist()
        onNode(hasText(string(R.string.feature_locksmith_next))).assertDoesNotExist()
    }

    @Test
    @DisplayName("the back button of the bar goes back")
    fun theBackButtonGoesBack() = extension.use {
        // Given
        val touched = mutableListOf<String>()
        showLocksmith(start, touched)

        // When
        onNodeWithContentDescription(string(R.string.feature_locksmith_back)).performClick()

        // Then
        runOnIdle { assertThat(touched).containsExactly("back") }
    }

    @Test
    @DisplayName("the journey of the benchmark finds a hole, Place and the first tile by their tags")
    fun theJourneyFindsAHolePlaceAndTheFirstTileByTheirTags() = extension.use {
        // Given
        showLocksmith(start)

        // Then
        assertThat(onAllNodes(hasTestTag("locksmith_cell_hole")).fetchSemanticsNodes()).isNotEmpty()
        onNode(hasTestTag("locksmith_place")).assertExists()
        onNode(hasTestTag("locksmith_tile_0")).assertExists()
    }

    @Test
    @DisplayName("once aimed, the cells of the preview are tagged as such, no longer as the hole")
    fun onceAimedTheCellsOfThePreviewAreTaggedAsSuch() = extension.use {
        // Given: the selected piece aimed at a cell of the hole
        val state = LocksmithUiState(level = 1, game = start, aim = start.padlock.hole.first())
        val preview = checkNotNull(state.preview).cells.filter { it.isInside }

        // When
        setContent { LocksmithScreen(state = state, actions = LocksmithActions.None) }

        // Then
        onAllNodes(hasTestTag("locksmith_cell_preview")).assertCountEquals(preview.size)
        onAllNodes(hasTestTag("locksmith_cell_hole")).assertCountEquals(start.padlock.hole.count { it !in preview })
    }

    @Test
    @DisplayName("the screen keeps the reduced effects of the application: a button pressed stays in place")
    fun theScreenKeepsTheReducedEffects() = extension.use {
        // Given the screen in reduced effects, on a clock that only moves when told
        mainClock.autoAdvance = false
        setContent {
            BloxTheme(effects = BloxEffects(reduced = true, forcedBySystem = false)) {
                LocksmithScreen(state = LocksmithUiState(level = 1, game = start), actions = LocksmithActions.None)
            }
        }
        mainClock.advanceTimeByFrame()
        val restart = string(R.string.feature_locksmith_restart)
        val atRest = onNodeWithText(restart, useUnmergedTree = true).getBoundsInRoot().top

        // When Recommencer is held, long enough for the column that scrolls to let the press through
        onNodeWithText(restart).performTouchInput { down(center) }
        mainClock.advanceTimeBy(HELD_MILLIS)

        // Then it does not go down
        assertThat(onNodeWithText(restart, useUnmergedTree = true).getBoundsInRoot().top).isEqualTo(atRest)
    }

    @Test
    @DisplayName("after a refusal, the padlock is shaken from its second frame, and back in place 18 ticks later")
    fun afterARefusalThePadlockIsShakenThenBackInPlace() = extension.use {
        // Given the screen, on a clock that only moves when told
        mainClock.autoAdvance = false
        val state = showHosted(LocksmithUiState(level = 1, game = start))
        mainClock.advanceTimeByFrame()
        val atRest = cellLeft(SHAKEN_CELL)

        // When a placement is refused
        val aim = Position(1, 1)
        runOnIdle { state.value = shown(start.place(aim)).copy(aim = aim) }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        // Then a cell of the padlock is aside, and back in place once the shake is over
        assertThat(cellLeft(SHAKEN_CELL)).isNotEqualTo(atRest)
        repeat(FRAMES_OF_A_SHAKE) { mainClock.advanceTimeByFrame() }
        assertThat(cellLeft(SHAKEN_CELL)).isEqualTo(atRest)
    }

    @Test
    @DisplayName("in reduced effects, a refusal never shakes the padlock")
    fun inReducedEffectsARefusalNeverShakesThePadlock() = extension.use {
        // Given the screen in reduced effects, on a clock that only moves when told
        mainClock.autoAdvance = false
        val state = mutableStateOf(LocksmithUiState(level = 1, game = start))
        setContent {
            BloxTheme(effects = BloxEffects(reduced = true, forcedBySystem = false)) {
                LocksmithScreen(state = state.value, actions = LocksmithActions.None)
            }
        }
        mainClock.advanceTimeByFrame()
        val atRest = cellLeft(SHAKEN_CELL)

        // When a placement is refused, frame by frame
        val aim = Position(1, 1)
        runOnIdle { state.value = shown(start.place(aim)).copy(aim = aim) }
        val lefts = (1..FRAMES_OF_A_SHAKE).map {
            mainClock.advanceTimeByFrame()
            cellLeft(SHAKEN_CELL)
        }

        // Then
        assertThat(lefts).containsOnly(atRest)
    }

    @Test
    @DisplayName("a screen shown with a refusal already made is not shaken")
    fun aScreenShownWithARefusalIsNotShaken() = extension.use {
        // Given the padlock at rest
        mainClock.autoAdvance = false
        val state = mutableStateOf(LocksmithUiState(level = 1, game = start))
        val showing = mutableStateOf(0)
        setContent {
            key(showing.value) { LocksmithScreen(state = state.value, actions = LocksmithActions.None) }
        }
        mainClock.advanceTimeByFrame()
        val atRest = cellLeft(SHAKEN_CELL)

        // When the screen is shown again, as on coming back to it, on a refusal made before
        val aim = Position(1, 1)
        runOnIdle {
            state.value = shown(start.place(aim)).copy(aim = aim)
            showing.value += 1
        }
        val lefts = (1..FRAMES_OF_A_SHAKE).map {
            mainClock.advanceTimeByFrame()
            cellLeft(SHAKEN_CELL)
        }

        // Then
        assertThat(lefts).containsOnly(atRest)
    }

    @Test
    @DisplayName("the grid does not recompose while the padlock shakes")
    fun theGridDoesNotRecomposeWhileThePadlockShakes() = extension.use {
        // Given a refusal, its first frame drawn
        mainClock.autoAdvance = false
        val state = showHosted(LocksmithUiState(level = 1, game = start))
        mainClock.advanceTimeByFrame()
        val aim = Position(1, 1)
        runOnIdle { state.value = shown(start.place(aim)).copy(aim = aim) }
        mainClock.advanceTimeByFrame()
        recompositions.start()

        // When the shake plays to its end
        repeat(FRAMES_OF_A_SHAKE) { mainClock.advanceTimeByFrame() }

        // Then
        assertThat(recompositions.recomposed.filter { "PadlockGrid" in it || "PadlockCell" in it }).isEmpty()
    }

    @Test
    @DisplayName("the grid is read as a collection of 8 rows and 8 columns, each cell at its place, the ring left out")
    fun theGridIsReadAsACollection() = extension.use {
        // Given a preview on the ring, refused there
        val aim = Position(1, 1)
        showLocksmith(shown(start.place(aim)).copy(aim = aim))

        // Then
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.CollectionInfo, CollectionInfo(8, 8))).assertExists()
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo)).assertCountEquals(64)
        onAllNodes(hasContentDescription(string(R.string.feature_locksmith_cell, 0, 1, ""), substring = true))
            .assertCountEquals(0)
        val item = onNode(hasContentDescription(string(R.string.feature_locksmith_cell, 3, 5, ""), substring = true))
            .fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertThat(listOf(item.rowIndex, item.columnIndex)).containsExactly(4, 2)
    }

    @Test
    @DisplayName("turning the chosen piece tells its new orientation by the message, its tile keeping it to be read")
    fun turningTheChosenPieceTellsItsOrientation() = extension.use {
        // Given the first piece chosen
        val state = showHosted(LocksmithUiState(level = 1, game = start))
        val type = start.given.first().name

        // When it is turned clockwise
        runOnIdle { state.value = state.value.copy(game = start.turn(Rotation.CLOCKWISE), turned = true) }

        // Then the message tells it, and the tile reads it as its state, without being announced
        val turned = string(orientationOf(start.turn(Rotation.CLOCKWISE), 0))
        onNodeWithText(string(R.string.feature_locksmith_message_turned, type, turned))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithContentDescription(string(R.string.feature_locksmith_tile, type))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, turned))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    @DisplayName("after a placement, and after a choice, the message is the one region TalkBack announces")
    fun theMessageIsTheOneLiveRegion() = extension.use {
        // Given a piece placed where the solution has it
        val piece = start.padlock.solution.first { it.type == start.given.first() }
        val placed = start.select(0).turnedTo(piece.orientation).place(piece.pivot)
        val state = showHosted(shown(placed))
        val liveRegions = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
        onAllNodes(liveRegions).assertCountEquals(1)

        // When another piece is chosen
        val other = placed.game.given.indices.last()
        runOnIdle { state.value = state.value.copy(game = placed.game.select(other), events = emptyList()) }

        // Then
        onAllNodes(liveRegions).assertCountEquals(1)
    }

    @Test
    @DisplayName("once open, the message reads the cadence by its chords, as the band does")
    fun onceOpenTheMessageReadsTheCadenceByItsChords() = extension.use {
        // Given
        showLocksmith(start.solved())

        // Then: V–I would be read letter by letter
        onNodeWithText(string(R.string.feature_locksmith_message_cadence))
            .assert(hasContentDescription("La cadence 5 majeur, 1 majeur a fermé la phrase."))
    }

    @Test
    @DisplayName("the band of the chords reads the chords heard and those to come, by their degree")
    fun theBandReadsTheChords() = extension.use {
        // Given padlock 1: three pieces, IV – V – I, none placed
        showLocksmith(start)

        // Then
        onNodeWithContentDescription("Aucun accord entendu. À venir : 4 majeur, 5 majeur, 1 majeur.").assertExists()
    }

    @Test
    @DisplayName("a second refusal in a row is told again, under its number")
    fun aSecondRefusalIsToldAgain() = extension.use {
        // Given a refusal
        val aim = Position(1, 1)
        val first = start.place(aim)
        val state = showHosted(shown(first).copy(aim = aim))
        val reason = string(R.string.feature_locksmith_message_out_of_hole)
        onNodeWithText(
            reason,
        ).assert(hasContentDescription(string(R.string.feature_locksmith_message_refusal, 1, reason)))

        // When the same placement is refused again
        val second = first.game.place(aim)
        runOnIdle { state.value = shown(second).copy(aim = aim, move = 2) }

        // Then the same reason, told under a new number, so that TalkBack tells it again
        onNodeWithText(
            reason,
        ).assert(hasContentDescription(string(R.string.feature_locksmith_message_refusal, 2, reason)))
    }

    @Test
    @DisplayName("the piece named by the hint is read in the message and on the cells it marks")
    fun theHintedPieceIsNamed() = extension.use {
        // Given a hint asked at the start
        val update = start.hint()
        val hint = (update.events.single() as PadlockEvent.HintGiven).hint as Hint.Fit
        showLocksmith(shown(update).copy(hint = hint))
        val type = hint.piece.type.name

        // Then
        onNodeWithText(string(R.string.feature_locksmith_message_hint, type)).assertExists()
        onAllNodes(hasContentDescription(string(R.string.feature_locksmith_cell_hint, type), substring = true))
            .assertCountEquals(hint.piece.cells.size)
    }

    @Test
    @DisplayName("on opening, « Serrure ouverte ! » is a heading and takes the focus")
    fun onOpeningTheTitleOfTheCardTakesTheFocus() = extension.use {
        // Given the padlock but one piece placed
        val solution = start.padlock.solution
        val almost = solution.dropLast(1).fold(start) { game, piece -> game.placing(piece) }
        val state = showHosted(LocksmithUiState(level = 1, game = almost))

        // When the last one opens it
        runOnIdle { state.value = state.value.copy(game = start.solved(), events = listOf(PadlockEvent.PadlockOpened)) }

        // Then
        onNode(hasTextExactly(string(R.string.feature_locksmith_open_title)))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsFocused()
    }

    @Test
    @DisplayName("once open, a single node says « Serrure ouverte ! »")
    fun onceOpenASingleNodeTellsIt() = extension.use {
        // Given
        showLocksmith(start.solved())

        // Then
        val open = string(R.string.feature_locksmith_open_title)
        onAllNodes(
            hasTextExactly(open) or SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(open)),
        )
            .assertCountEquals(1)
    }

    @Test
    @DisplayName("the message is a polite live region while the padlock is closed, and no longer once it is open")
    fun theMessageIsALiveRegionWhileClosed() = extension.use {
        // Given
        val state = showHosted(LocksmithUiState(level = 1, game = start))

        // Then, closed
        onNodeWithText(string(R.string.feature_locksmith_message_aim))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

        // When it opens
        runOnIdle { state.value = state.value.copy(game = start.solved()) }

        // Then
        onNodeWithText(string(R.string.feature_locksmith_message_cadence))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    @DisplayName("the padlock of the bar has no node, the title of the bar tells the padlock closed, then open")
    fun theTitleOfTheBarTellsThePadlockState() = extension.use {
        // Given
        val state = showHosted(LocksmithUiState(level = 3, game = start))

        // Then
        onNodeWithContentDescription("Serrure 03, fermée")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        // When it opens
        runOnIdle { state.value = state.value.copy(game = start.solved()) }

        // Then
        onNodeWithContentDescription("Serrure 03, ouverte").assertExists()
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Serrure ouverte")))
            .assertCountEquals(0)
    }

    @Test
    @DisplayName("after Rejouer cette serrure, the message takes the focus")
    fun afterReplayTheMessageTakesTheFocus() = extension.use {
        // Given an open padlock, replayed from its card
        val state = mutableStateOf(LocksmithUiState(level = 1, game = start.solved()))
        val actions = LocksmithActions.None.copyWith(
            onRestart = { state.value = LocksmithUiState(level = 1, game = start) },
        )
        setContent { LocksmithScreen(state = state.value, actions = actions) }

        // When
        onNodeWithText(string(R.string.feature_locksmith_replay)).performScrollTo().performClick()

        // Then
        onNodeWithText(string(R.string.feature_locksmith_message_aim)).assertIsFocused()
    }

    @Test
    @DisplayName("with its view model: a placement refused, then accepted, then taken back")
    fun placeRefuseAndTakeBack() = extension.use {
        // Given padlock 1 of seed 7, its first piece chosen, and where the solution has it
        val viewModel =
            LocksmithViewModel(
                LocksmithRoute(level = 1, seed = 7),
                FakeSoundEffectPlayer(),
                FakeUserPreferencesRepository(),
            )
        setContent {
            val state by viewModel.uiState.collectAsState()
            LocksmithScreen(state = state, actions = actionsOf(viewModel))
        }
        val type = start.given.first()
        val piece = start.padlock.solution.first { it.type == type }
        val turns = generateSequence(start) { it.turn(Rotation.CLOCKWISE) }.indexOfFirst {
            it.orientationOf(0) ==
                piece.orientation
        }

        // When it is aimed at the corner, half on the metal, and placed there
        cell(Position(1, 1)).performClick()
        cell(Position(1, 1)).performClick()

        // Then it is refused, and told why
        onNodeWithText(string(R.string.feature_locksmith_message_out_of_hole)).assertExists()
        onNodeWithText(string(R.string.feature_locksmith_moves, 0)).assertExists()

        // When it is turned as the solution has it, aimed by its pivot, and placed
        repeat(turns) { onNodeWithContentDescription(string(R.string.feature_locksmith_turn_right)).performClick() }
        cell(piece.pivot).performClick()
        onNodeWithText(string(R.string.feature_locksmith_place)).performClick()

        // Then it lies there, a move counted
        cell(
            piece.pivot,
        ).assert(hasContentDescription(string(R.string.feature_locksmith_cell_placed, type.name), substring = true))
        onNodeWithText(string(R.string.feature_locksmith_moves, 1)).assertExists()

        // When it is touched, it is taken back: the preview, where it lay
        cell(piece.pivot).performClick()

        // Then
        cell(
            piece.pivot,
        ).assert(hasContentDescription(string(R.string.feature_locksmith_cell_preview, type.name), substring = true))
        onNodeWithContentDescription(string(R.string.feature_locksmith_tile_placed, type.name)).assertDoesNotExist()
    }

    /** Where the cell at [position] stands from the left of the screen. */
    private fun ComposeContext.cellLeft(position: Position) = cell(position).getBoundsInRoot().left

    /** The cell of the padlock at [position], found by what TalkBack reads of it. */
    private fun ComposeContext.cell(position: Position) = onNode(
        hasContentDescription(
            string(R.string.feature_locksmith_cell, position.column, position.row, ""),
            substring = true,
        ),
    )

    /** The screen of [game], each action that matters here noted in [touched] by its name. */
    private fun ComposeContext.showLocksmith(
        game: PadlockGame,
        touched: MutableList<String> = mutableListOf(),
    ) {
        showLocksmith(LocksmithUiState(level = 1, game = game), touched)
    }

    /** The screen of [state], each action that matters here noted in [touched] by its name. */
    private fun ComposeContext.showLocksmith(
        state: LocksmithUiState,
        touched: MutableList<String> = mutableListOf(),
    ) {
        val actions = LocksmithActions(
            onBack = { touched += "back" },
            onTap = {},
            onSelect = {},
            onTakeBack = {},
            onTurn = {},
            onPlace = { touched += "place" },
            onHint = { touched += "hint" },
            onRestart = { touched += "restart" },
            onNext = { touched += "next" },
        )
        setContent { LocksmithScreen(state = state, actions = actions) }
    }

    /** The screen of a state the test changes, starting from [initial]. */
    private fun ComposeContext.showHosted(initial: LocksmithUiState): MutableState<LocksmithUiState> {
        val state = mutableStateOf(initial)
        setContent { LocksmithScreen(state = state.value, actions = LocksmithActions.None) }
        return state
    }

    /** What the screen shows after [update], the first move. */
    private fun shown(update: PadlockUpdate): LocksmithUiState =
        LocksmithUiState(level = 1, game = update.game, events = update.events, move = 1)

    /** The actions of the screen, played on [viewModel], as its navigation entry wires them. */
    private fun actionsOf(viewModel: LocksmithViewModel) = LocksmithActions(
        onBack = {},
        onTap = viewModel::tap,
        onSelect = viewModel::select,
        onTakeBack = viewModel::takeBack,
        onTurn = viewModel::turn,
        onPlace = viewModel::placeAim,
        onHint = viewModel::hint,
        onRestart = viewModel::restart,
        onNext = {},
    )

    /** These actions, Restart replaced by [onRestart]. */
    private fun LocksmithActions.copyWith(onRestart: () -> Unit) = LocksmithActions(
        onBack = onBack,
        onTap = onTap,
        onSelect = onSelect,
        onTakeBack = onTakeBack,
        onTurn = onTurn,
        onPlace = onPlace,
        onHint = onHint,
        onRestart = onRestart,
        onNext = onNext,
    )

    private companion object {
        /** A cell inside the padlock, away from the preview of the refusals. */
        val SHAKEN_CELL = Position(5, 5)

        /** The frames of a shake, 18 ticks, and a few more. */
        const val FRAMES_OF_A_SHAKE = 24

        /** Longer than the wait of a column that scrolls before it lets a press through to a button. */
        const val HELD_MILLIS = 500L
    }

    private fun string(
        id: Int,
        vararg arguments: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *arguments)
}
