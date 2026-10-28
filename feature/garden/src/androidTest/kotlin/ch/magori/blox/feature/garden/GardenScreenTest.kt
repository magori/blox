package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.feature.garden.navigation.gardenActions
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class GardenScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    private val empty = GardenUiState(Garden.EMPTY)

    @Test
    @DisplayName("the title of the bar shows JARDIN MUSICAL and is read as a heading")
    fun theTitleOfTheBarIsReadAsAHeading() = extension.use {
        // Given / When
        showGarden()

        // Then
        val title = string(R.string.feature_garden_title)
        onNodeWithText(title.uppercase()).assertExists()
        onNodeWithContentDescription(title).assert(isHeading())
    }

    @Test
    @DisplayName("Changer de jeu is handed to app")
    fun changerDeJeuIsHandedToApp() = extension.use {
        // Given
        var changes = 0
        showGarden(GardenActions(onChangeGame = { changes++ }))

        // When
        onNodeWithContentDescription(string(R.string.feature_garden_change_game)).performClick()

        // Then
        runOnIdle { assertThat(changes).isEqualTo(1) }
    }

    @Test
    @DisplayName("Changer de jeu counts only while the garden is resumed")
    fun changerDeJeuCountsOnlyWhileResumed() = extension.use {
        // Given the garden no longer resumed, as once a first tap left it (its transition under way)
        var changes = 0
        val owner = PausedLifecycleOwner()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GardenScreen(uiState = empty, actions = GardenActions(onChangeGame = { changes++ }))
                }
            }
        }

        // When
        onNodeWithContentDescription(string(R.string.feature_garden_change_game)).performClick()

        // Then nothing is handed to app
        runOnIdle { assertThat(changes).isZero() }
    }

    @Test
    @DisplayName("each cell is read with its eighth, from 1, its note in the mood, and what it holds")
    fun eachCellIsReadWithItsEighthItsNoteAndWhatItHolds() = extension.use {
        // Given an I on the bottom row, columns 0 to 3, in the calm mood: do, ré, mi from the bottom
        showGarden(state = GardenUiState(I_AT_THE_BOTTOM))

        // Then
        onNode(cellAt(4, 11)).assertContentDescriptionEquals("Croche 5, mi : libre")
        onNode(cellAt(0, 13)).assertContentDescriptionEquals("Croche 1, do : pièce I, note de l'accord")
        onNode(cellAt(3, 13)).assertContentDescriptionEquals("Croche 4, do : pièce I, note de l'accord")
    }

    @Test
    @DisplayName("the grid is a collection of 14 rows and 16 columns")
    fun theGridIsACollection() = extension.use {
        // Given / When
        showGarden()

        // Then
        val collections = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.CollectionInfo] }
        assertThat(collections.map { it.rowCount to it.columnCount }).contains(Garden.ROWS to Garden.COLUMNS)
    }

    @Test
    @DisplayName("the tray offers the 7 pieces as one group of choice, the chosen one selected, Retirer apart")
    fun theTrayOffersTheSevenPiecesAsOneGroup() = extension.use {
        // Given / When
        showGarden()

        // Then
        val group = hasContentDescription(string(R.string.feature_garden_pieces))
        val tiles = onAllNodes(hasAnyAncestor(group) and hasContentDescription("Pièce ", substring = true))
        assertThat(tiles.fetchSemanticsNodes()).hasSize(7)
        onNodeWithContentDescription("Pièce T").assert(isSelected())
        val remove = string(R.string.feature_garden_remove)
        assertThat(onAllNodes(hasAnyAncestor(group) and hasContentDescription(remove)).fetchSemanticsNodes()).isEmpty()
        onNodeWithText(remove).assertIsNotEnabled()
    }

    @Test
    @DisplayName("touching a cell shows the chosen piece there, and a second touch on it places it")
    fun touchingACellThenThePreviewPlacesThePiece() = extension.use {
        // Given the garden on its view model, empty and calm
        showViewModel(FakeGardenRepository())

        // When a touch on the 6th eighth, mi, then on the pivot of the T shown there
        onNode(cellAt(5, 11)).assertContentDescriptionEquals("Croche 6, mi : libre").performScrollTo().performClick()
        onNode(cellAt(5, 11)).assertContentDescriptionEquals("Croche 6, mi : aperçu de la pièce T").performClick()

        // Then the T is placed
        onNode(cellAt(5, 11)).assertContentDescriptionEquals("Croche 6, mi : pièce T, note de l'accord")
        assertThat(onAllNodesWithContentDescription("aperçu", substring = true).fetchSemanticsNodes()).isEmpty()
    }

    @Test
    @DisplayName("Tout effacer offers Annuler as long as the accessibility of the phone asks")
    fun toutEffacerOffersAnnulerAsLongAsAccessibilityAsks() = extension.use {
        // Given a player who asked Android for more time to act: 30 s
        var offered: Long? = null
        val slow = object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(
                originalTimeoutMillis: Long,
                containsIcons: Boolean,
                containsText: Boolean,
                containsControls: Boolean,
            ): Long = 30_000
        }
        setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides slow) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GardenScreen(
                        uiState = GardenUiState(I_AT_THE_BOTTOM),
                        actions = GardenActions(onChangeGame = {}, onClearAll = { offered = it }),
                    )
                }
            }
        }

        // When
        onNodeWithText(string(R.string.feature_garden_clear_all)).performClick()

        // Then
        runOnIdle { assertThat(offered).isEqualTo(30_000L) }
    }

    @Test
    @DisplayName("while Annuler is offered, the page scrolls far enough that it covers no tile of the tray")
    fun annulerCoversNoTile() = extension.use {
        // Given a garden with an I, cleared, in a font twice as large: the page scrolls
        showViewModel(FakeGardenRepository(I_AT_THE_BOTTOM), fontScale = 2f)
        onNodeWithText(string(R.string.feature_garden_clear_all)).performClick()

        // When the page is scrolled to its end
        onNode(hasScrollAction()).performScrollToNode(hasText(string(R.string.feature_garden_remove)))
        onNode(hasScrollAction()).performTouchInput { repeat(3) { swipeUp() } }

        // Then Retirer lies above the bar
        val remove = onNodeWithText(string(R.string.feature_garden_remove)).getUnclippedBoundsInRoot()
        val bar = onNodeWithText(string(R.string.feature_garden_cleared)).onParent().getUnclippedBoundsInRoot()
        assertThat(remove.bottom.value).isLessThanOrEqualTo(bar.top.value)
    }

    @Test
    @DisplayName("Tout effacer shows Jardin effacé with Annuler, which gives the garden back")
    fun toutEffacerOffersAnnuler() = extension.use {
        // Given a garden with an I
        val kept = FakeGardenRepository(I_AT_THE_BOTTOM)
        showViewModel(kept)

        // When
        onNodeWithText(string(R.string.feature_garden_clear_all)).performClick()

        // Then the bar offers to undo it
        onNodeWithText(string(R.string.feature_garden_cleared)).assertExists()
        onNode(cellAt(0, 13)).assertContentDescriptionEquals("Croche 1, do : libre")

        // When
        onNodeWithText(string(R.string.feature_garden_undo)).performClick()

        // Then the I is back, and the bar gone
        onNode(cellAt(0, 13)).assertContentDescriptionEquals("Croche 1, do : pièce I, note de l'accord")
        onNodeWithText(string(R.string.feature_garden_cleared)).assertDoesNotExist()
        runOnIdle { assertThat(kept.state.value).isEqualTo(I_AT_THE_BOTTOM) }
    }

    /** A screen started but not resumed: paused, as during the transition to the next one. */
    private class PausedLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.STARTED }
    }

    /** [state] under the Original theme of the application: it draws itself in the Colorful theme. */
    private fun ComposeContext.showGarden(
        actions: GardenActions = GardenActions.None,
        state: GardenUiState = empty,
    ) {
        setContent {
            BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                GardenScreen(uiState = state, actions = actions)
            }
        }
    }

    /** The screen on its view model, opened on the garden [kept], as the entry shows it. */
    private fun ComposeContext.showViewModel(
        kept: FakeGardenRepository,
        fontScale: Float = 1f,
    ) {
        val viewModel = GardenViewModel(kept, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), Mutex())
        setContent {
            val state by viewModel.uiState.collectAsState()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                BloxTheme(kind = BloxThemeKind.ORIGINAL) {
                    GardenScreen(uiState = state, actions = gardenActions(viewModel, onChangeGame = {}))
                }
            }
        }
    }

    /** The cell of the grid at [column] and [row], found by its place in the collection: a note recurs, octave apart. */
    private fun cellAt(
        column: Int,
        row: Int,
    ) = SemanticsMatcher("cell ($column, $row)") { node ->
        val info = SemanticsProperties.CollectionItemInfo
        info in node.config && node.config[info].let { it.columnIndex == column && it.rowIndex == row }
    }

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private companion object {
        /** An I lying on the bottom row, columns 0 to 3, in the calm mood. */
        val I_AT_THE_BOTTOM: Garden = Garden.parse(
            List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n") + "\nIIII............",
        )
    }
}
