package ch.magori.blox.feature.locksmith

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.locksmith.Padlock
import ch.magori.blox.core.locksmith.PadlockGame
import ch.magori.blox.core.locksmith.solved
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
            onNodeWithContentDescription(string(R.string.feature_locksmith_tile, type.name)).assertDoesNotExist()
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

    /** The screen of [game], each action that matters here noted in [touched] by its name. */
    private fun ComposeContext.showLocksmith(
        game: PadlockGame,
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
        setContent { LocksmithScreen(state = LocksmithUiState(level = 1, game = game), actions = actions) }
    }

    private fun string(
        id: Int,
        vararg arguments: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *arguments)
}
