package ch.magori.blox.feature.locksmith

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.locksmith.PadlockResult
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import ch.magori.blox.core.designsystem.R as DesignSystemR

/** The screen « Serrures » (P7.35): the summary, Reprendre, the grid of the padlocks, read by TalkBack; starting over (P7.37). */
@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class PadlocksScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("the summary tells the open padlocks and the perfect ones, read as a heading")
    fun theSummaryTellsTheOpenAndThePerfectPadlocks() = extension.use {
        // Given padlocks 1 to 3 open, 1 and 3 perfectly
        showPadlocks(PadlocksUiState(listOf(PERFECT, WITH_A_HINT, PERFECT)))

        // Then
        val summary = string(
            R.string.feature_locksmith_padlocks_summary,
            plural(R.plurals.feature_locksmith_padlocks_open, 3),
            plural(R.plurals.feature_locksmith_padlocks_perfect, 2),
        )
        onNodeWithText(summary).assertIsDisplayed().assert(isHeading())
    }

    @Test
    @DisplayName("Reprendre opens the frontier")
    fun reprendreOpensTheFrontier() = extension.use {
        // Given padlocks 1 to 3 open
        val opened = mutableListOf<Int>()
        showPadlocks(PadlocksUiState(listOf(PERFECT, WITH_A_HINT, PERFECT)), opened = opened)

        // When
        onNodeWithText(string(R.string.feature_locksmith_padlocks_resume, 4)).performClick()

        // Then
        runOnIdle { assertThat(opened).containsExactly(4) }
    }

    @Test
    @DisplayName("a padlock is read in one go: its number, its state, whether the player comes from it, its best")
    fun aPadlockIsReadInOneGo() = extension.use {
        // Given padlock 1 open perfectly, padlock 2 with a hint, the player coming from padlock 2
        showPadlocks(PadlocksUiState(listOf(PERFECT, WITH_A_HINT)), current = 2)

        // Then
        val perfect = listOf(
            string(R.string.feature_locksmith_padlocks_cell, 1),
            string(R.string.feature_locksmith_padlocks_open),
            string(R.string.feature_locksmith_padlocks_perfect),
            best(PERFECT),
        )
        val current = listOf(
            string(R.string.feature_locksmith_padlocks_cell, 2),
            string(R.string.feature_locksmith_padlocks_open),
            string(R.string.feature_locksmith_padlocks_current),
            best(WITH_A_HINT),
        )
        val frontier = listOf(
            string(R.string.feature_locksmith_padlocks_cell, 3),
            string(R.string.feature_locksmith_padlocks_frontier),
        )
        tile(1).assertContentDescriptionEquals(perfect.joinToString(", "))
        tile(2).assertContentDescriptionEquals(current.joinToString(", "))
        tile(3).assertContentDescriptionEquals(frontier.joinToString(", "))
    }

    @Test
    @DisplayName("touching an open padlock or the frontier asks to open it, by its number")
    fun touchingAPadlockAsksToOpenIt() = extension.use {
        // Given padlocks 1 and 2 open
        val opened = mutableListOf<Int>()
        showPadlocks(PadlocksUiState(listOf(PERFECT, WITH_A_HINT)), opened = opened)

        // When
        tile(2).performClick()
        tile(3).performClick()

        // Then
        runOnIdle { assertThat(opened).containsExactly(2, 3) }
    }

    @Test
    @DisplayName("a locked padlock, which completes the row of the frontier, is read locked and cannot be touched")
    fun aLockedPadlockIsReadLockedAndCannotBeTouched() = extension.use {
        // Given padlock 4 open, on a grid of 4 columns: the journeymen begin with 4, 5 the frontier, 6 and 7 locked
        showPadlocks(PadlocksUiState(listOf(PERFECT, PERFECT, PERFECT, PERFECT)))

        // Then
        val locked = listOf(
            string(R.string.feature_locksmith_padlocks_cell, 6),
            string(R.string.feature_locksmith_padlocks_locked),
        )
        tile(6).assertContentDescriptionEquals(locked.joinToString(", ")).assertHasNoClickAction()
        tile(7).assertHasNoClickAction()
        tile(8).assertDoesNotExist()
        tile(5).assertHasClickAction()
    }

    @Test
    @DisplayName("each tier has its header, its name and the pieces of its padlocks, read as a heading")
    fun eachTierHasItsHeader() = extension.use {
        // Given padlocks 1 to 4 open: apprentices, then journeymen
        showPadlocks(PadlocksUiState(listOf(PERFECT, PERFECT, PERFECT, PERFECT)))

        // Then
        val apprentice = string(
            R.string.feature_locksmith_padlocks_tier,
            string(R.string.feature_locksmith_padlocks_tier_apprentice),
            string(R.string.feature_locksmith_padlocks_tier_pieces_range, 3, 5),
        )
        val journeyman = string(
            R.string.feature_locksmith_padlocks_tier,
            string(R.string.feature_locksmith_padlocks_tier_journeyman),
            string(R.string.feature_locksmith_padlocks_tier_pieces, 6),
        )
        onNodeWithText(apprentice).assertIsDisplayed().assert(isHeading())
        onNodeWithText(journeyman).assertIsDisplayed().assert(isHeading())
    }

    @Test
    @DisplayName("without an opening, the screen asks to open padlock 1, and offers no Reprendre")
    fun withoutAnOpeningTheScreenAsksToOpenPadlock1() = extension.use {
        // Given
        showPadlocks(PadlocksUiState(emptyList()))

        // Then
        onNodeWithText(string(R.string.feature_locksmith_padlocks_empty)).assertIsDisplayed()
        onNodeWithText(string(R.string.feature_locksmith_padlocks_resume, 1)).assertDoesNotExist()
        tile(1).assertHasClickAction()
    }

    @Test
    @DisplayName("the grid, a collection, opens scrolled on the frontier")
    fun theGridOpensScrolledOnTheFrontier() = extension.use {
        // Given padlocks 1 to 60 open
        showPadlocks(PadlocksUiState(List(60) { PERFECT }))

        // Then
        onNode(hasTestTag(PADLOCKS_GRID_TAG))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
        tile(61).assertIsDisplayed()
        // Off the screen, or not even composed by the lazy grid.
        if (onAllNodes(hasTestTag(padlockTileTag(1))).fetchSemanticsNodes().isNotEmpty()) tile(1).assertIsNotDisplayed()
    }

    @Test
    @DisplayName(
        "TalkBack reads the grid by rows and columns, the summary, Reprendre, the headers and Recommencer across a row",
    )
    fun talkBackReadsTheGridByRowsAndColumns() = extension.use {
        // Given padlocks 1 to 4 open, on a phone of 390 dp, a grid of 4 columns: the summary, Reprendre, the
        // apprentices under their header, 1 to 3, the journeymen under theirs, 4 to 7, then Recommencer
        showPadlocks(PadlocksUiState(listOf(PERFECT, PERFECT, PERFECT, PERFECT)))

        // Then: 7 rows of 4 columns
        val grid = onNode(hasTestTag(PADLOCKS_GRID_TAG)).fetchSemanticsNode().config
        assertThat(grid[SemanticsProperties.CollectionInfo].let { it.rowCount to it.columnCount }).isEqualTo(7 to 4)
        // Each tile at its row and its column
        assertThat(placeOf(tile(1))).isEqualTo(Place(row = 3, column = 0, columns = 1))
        assertThat(placeOf(tile(3))).isEqualTo(Place(row = 3, column = 2, columns = 1))
        assertThat(placeOf(tile(4))).isEqualTo(Place(row = 5, column = 0, columns = 1))
        assertThat(placeOf(tile(7))).isEqualTo(Place(row = 5, column = 3, columns = 1))
        // The others across their row
        val summary = string(
            R.string.feature_locksmith_padlocks_summary,
            plural(R.plurals.feature_locksmith_padlocks_open, 4),
            plural(R.plurals.feature_locksmith_padlocks_perfect, 4),
        )
        val journeyman = string(
            R.string.feature_locksmith_padlocks_tier,
            string(R.string.feature_locksmith_padlocks_tier_journeyman),
            string(R.string.feature_locksmith_padlocks_tier_pieces, 6),
        )
        assertThat(placeOf(onNodeWithText(summary))).isEqualTo(Place(row = 0, column = 0, columns = 4))
        assertThat(placeOf(onNodeWithText(string(R.string.feature_locksmith_padlocks_resume, 5))))
            .isEqualTo(Place(row = 1, column = 0, columns = 4))
        assertThat(placeOf(onNodeWithText(journeyman))).isEqualTo(Place(row = 4, column = 0, columns = 4))
        assertThat(placeOf(onNodeWithText(string(R.string.feature_locksmith_padlocks_start_over))))
            .isEqualTo(Place(row = 6, column = 0, columns = 4))
    }

    @Test
    @DisplayName("without an opening, TalkBack reads the invitation on the first row, and no Recommencer at the end")
    fun withoutAnOpeningTalkBackReadsTheInvitationOnTheFirstRow() = extension.use {
        // Given no padlock open, on a grid of 4 columns: the invitation, the header of the apprentices, 1 to 3
        showPadlocks(PadlocksUiState(emptyList()))

        // Then: 3 rows of 4 columns
        val grid = onNode(hasTestTag(PADLOCKS_GRID_TAG)).fetchSemanticsNode().config
        assertThat(grid[SemanticsProperties.CollectionInfo].let { it.rowCount to it.columnCount }).isEqualTo(3 to 4)
        assertThat(placeOf(onNodeWithText(string(R.string.feature_locksmith_padlocks_empty))))
            .isEqualTo(Place(row = 0, column = 0, columns = 4))
        assertThat(placeOf(tile(1))).isEqualTo(Place(row = 2, column = 0, columns = 1))
        assertThat(placeOf(tile(3))).isEqualTo(Place(row = 2, column = 2, columns = 1))
    }

    @Test
    @DisplayName("in a font twice as large the grid has fewer columns, its tiles 80 dp at least")
    fun inALargeFontTheGridHasFewerColumns() = extension.use {
        // Given padlocks 1 and 2 open, on a phone of 390 dp, which holds the 3 apprentices on a row in a normal font
        val state = PadlocksUiState(listOf(PERFECT, PERFECT))

        // When: in a font twice as large
        showPadlocks(state, fontScale = 2f)

        // Then: 1 and 2 on a row, 3 under them
        val one = tile(1).getBoundsInRoot()
        val two = tile(2).getBoundsInRoot()
        assertThat(two.top).isEqualTo(one.top)
        assertThat(tile(3).getBoundsInRoot().top).isGreaterThan(one.bottom)
        assertThat(one.right - one.left).isGreaterThanOrEqualTo(TILE)
        assertThat(one.bottom - one.top).isGreaterThanOrEqualTo(TILE)
        // And TalkBack is told so
        val grid = onNode(hasTestTag(PADLOCKS_GRID_TAG)).fetchSemanticsNode().config
        assertThat(grid[SemanticsProperties.CollectionInfo].columnCount).isEqualTo(2)
    }

    @Test
    @DisplayName(
        "in a large font on a low screen, the summary and Reprendre scroll with the grid, a row of tiles shown whole",
    )
    fun inALargeFontOnALowScreenARowOfTilesIsShownWhole() = extension.use {
        // Given padlocks 1 to 12 open, on a screen of 360 by 400 dp, in a font twice as large
        showPadlocks(PadlocksUiState(List(12) { PERFECT }), fontScale = 2f, height = LOW_SCREEN, width = 360.dp)

        // Then: the frontier, 13, shown whole, as high as a tile, within the screen
        val frontier = tile(13).assertIsDisplayed().getBoundsInRoot()
        assertThat(frontier.bottom - frontier.top).isGreaterThanOrEqualTo(TILE)
        assertThat(frontier.top).isGreaterThanOrEqualTo(0.dp)
        assertThat(frontier.bottom).isLessThanOrEqualTo(LOW_SCREEN)
    }

    @Test
    @DisplayName("a tile that can be touched offers TalkBack to open it, « Ouvrir »")
    fun aTileOffersToOpenIt() = extension.use {
        // Given padlock 1 open
        showPadlocks(PadlocksUiState(listOf(PERFECT)))

        // Then
        val open = string(R.string.feature_locksmith_padlocks_open_action)
        tile(1).assert(SemanticsMatcher("its click is « $open »") { it.config[SemanticsActions.OnClick].label == open })
        tile(2).assert(SemanticsMatcher("its click is « $open »") { it.config[SemanticsActions.OnClick].label == open })
    }

    @Test
    @DisplayName("Recommencer depuis le début, at the end of the grid, asks whether to start over")
    fun startingOverIsAskedAtTheEndOfTheGrid() = extension.use {
        // Given padlocks 1 to 3 open
        val asked = mutableListOf<String>()
        showPadlocks(PadlocksUiState(listOf(PERFECT, WITH_A_HINT, PERFECT)), asked = asked)

        // When
        onNodeWithText(string(R.string.feature_locksmith_padlocks_start_over)).performScrollTo().performClick()

        // Then
        runOnIdle { assertThat(asked).containsExactly(START_OVER) }
    }

    @Test
    @DisplayName("without an opening, there is nothing to start over")
    fun withoutAnOpeningThereIsNothingToStartOver() = extension.use {
        // Given no padlock open
        showPadlocks(PadlocksUiState(emptyList()))

        // Then
        onNodeWithText(string(R.string.feature_locksmith_padlocks_start_over)).assertDoesNotExist()
    }

    @Test
    @DisplayName(
        "the confirmation tells how many padlocks will be forgotten, Garder first, then Effacer, each answering",
    )
    fun theConfirmationTellsWhatWillBeForgotten() = extension.use {
        // Given padlocks 1 to 3 open, the confirmation asked
        val asked = mutableListOf<String>()
        showPadlocks(
            PadlocksUiState(listOf(PERFECT, WITH_A_HINT, PERFECT), ProgressClearing.ASKED),
            asked = asked,
        )

        // Then the question, a heading, and what it forgets
        onNodeWithText(string(R.string.feature_locksmith_clear_title)).assertIsDisplayed().assert(isHeading())
        onNodeWithText(plural(R.plurals.feature_locksmith_clear_note, 3)).assertIsDisplayed()
        val keep = onNodeWithText(string(R.string.feature_locksmith_clear_keep)).assertIsDisplayed()
        val clear = onNodeWithText(string(R.string.feature_locksmith_clear_confirm)).assertIsDisplayed()
        assertThat(keep.getBoundsInRoot().top).isLessThan(clear.getBoundsInRoot().top)

        // When
        keep.performClick()
        clear.performClick()

        // Then
        runOnIdle { assertThat(asked).containsExactly(KEEP, CLEAR) }
    }

    @Test
    @DisplayName("while the clear is written, both answers are disabled")
    fun whileTheClearIsWrittenBothAnswersAreDisabled() = extension.use {
        // Given padlock 1 open, its clear being written
        showPadlocks(PadlocksUiState(listOf(PERFECT), ProgressClearing.CLEARING))

        // Then
        onNodeWithText(string(R.string.feature_locksmith_clear_keep)).assertIsNotEnabled()
        onNodeWithText(string(R.string.feature_locksmith_clear_confirm)).assertIsNotEnabled()
    }

    @Test
    @DisplayName(
        "once the progress is cleared, the confirmation stays open, its answers disabled, until the screen goes",
    )
    fun onceClearedTheConfirmationStaysOpenUntilTheScreenGoes() = extension.use {
        // Given padlock 1 open when the clear was asked, the progress now cleared
        showPadlocks(PadlocksUiState(listOf(PERFECT), ProgressClearing.CLEARED))

        // Then
        onNodeWithText(plural(R.plurals.feature_locksmith_clear_note, 1)).assertIsDisplayed()
        onNodeWithText(string(R.string.feature_locksmith_clear_keep)).assertIsNotEnabled()
        onNodeWithText(string(R.string.feature_locksmith_clear_confirm)).assertIsNotEnabled()
    }

    @Test
    @DisplayName("a clear that could not be written is told, the confirmation still open to try again")
    fun aClearThatFailedIsTold() = extension.use {
        // Given padlock 1 open, its clear failed
        showPadlocks(PadlocksUiState(listOf(PERFECT), ProgressClearing.FAILED))

        // Then
        onNodeWithText(string(DesignSystemR.string.core_designsystem_window_failed)).assertIsDisplayed()
        onNodeWithText(string(R.string.feature_locksmith_clear_confirm)).assertIsEnabled()
    }

    /**
     * The screen of [state] on a phone of [width] by [height], in a font of [fontScale], the padlocks asked noted in
     * [opened], the answers about starting over in [asked].
     */
    private fun ComposeContext.showPadlocks(
        state: PadlocksUiState,
        current: Int? = null,
        opened: MutableList<Int> = mutableListOf(),
        asked: MutableList<String> = mutableListOf(),
        fontScale: Float = 1f,
        height: Dp = 844.dp,
        width: Dp = 390.dp,
    ) {
        val actions = PadlocksActions(
            onBack = {},
            onOpen = { opened += it },
            onStartOver = { asked += START_OVER },
            onKeepProgress = { asked += KEEP },
            onClearProgress = { asked += CLEAR },
        )
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.size(width, height)) {
                    PadlocksScreen(state = state, current = current, actions = actions)
                }
            }
        }
    }

    /** The tile of padlock [level], read as one. */
    private fun ComposeContext.tile(level: Int) = onNode(hasTestTag(padlockTileTag(level)))

    /** Where an item of the grid is, for TalkBack: its row, its column, and how many columns it spans. */
    private data class Place(val row: Int, val column: Int, val columns: Int)

    /** The place of [node] in the collection of the grid, on one row always. */
    private fun placeOf(node: SemanticsNodeInteraction): Place {
        val info = node.fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertThat(info.rowSpan).isEqualTo(1)
        return Place(row = info.rowIndex, column = info.columnIndex, columns = info.columnSpan)
    }

    /** What a tile reads of the best opening [result]. */
    private fun best(result: PadlockResult): String = string(
        R.string.feature_locksmith_padlocks_best,
        result.moves,
        result.refusals,
        plural(R.plurals.feature_locksmith_open_best_hints, result.hints),
    )

    private fun string(
        id: Int,
        vararg arguments: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *arguments)

    private fun plural(
        id: Int,
        count: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

    private companion object {
        val PERFECT = PadlockResult(pieces = 3, moves = 3, refusals = 0, hints = 0)
        val WITH_A_HINT = PadlockResult(pieces = 4, moves = 5, refusals = 1, hints = 1)

        /** A screen as low as a phone in landscape. */
        val LOW_SCREEN = 400.dp

        /** The answers about starting over, as noted. */
        const val START_OVER = "start over"
        const val KEEP = "keep"
        const val CLEAR = "clear"
    }
}
