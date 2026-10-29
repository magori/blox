package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.CurrentGarden
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.feature.garden.navigation.gardenActions
import de.mannodermaus.junit5.compose.ComposeContext
import de.mannodermaus.junit5.compose.createComposeExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalTestApi::class)
// Sentences in @DisplayName: below DEX 040 (minSdk 26), a method name may not hold a space.
internal class MyGardensScreenTest {
    @JvmField
    @RegisterExtension
    val extension = createComposeExtension()

    @Test
    @DisplayName("Mes jardins bears its dot and is read « jardin modifié » while the garden changed since it was kept")
    fun myGardensBearsItsDotWhileTheGardenChanged() = extension.use {
        // Given / When the garden under way changed since it was kept as « Pluie douce »
        showState(PLUIE.let { GardenUiState(I_GARDEN, keptFrom = it.id, myGardens = MyGardens.Listed(listOf(it))) })

        // Then
        onNodeWithTag(GARDEN_MODIFIED_DOT_TAG).assertExists()
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    string(R.string.feature_garden_modified),
                ),
            )
    }

    @Test
    @DisplayName("Mes jardins bears no dot, nor its state, while the garden is as it was kept")
    fun myGardensBearsNoDotWhileTheGardenIsAsKept() = extension.use {
        // Given / When the garden under way as it was kept
        showState(GardenUiState(PLUIE.garden, keptFrom = PLUIE.id, myGardens = MyGardens.Listed(listOf(PLUIE))))

        // Then
        onNodeWithTag(GARDEN_MODIFIED_DOT_TAG).assertDoesNotExist()
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    @DisplayName("a garden kept from the sheet under the name proposed is told kept, and listed as open")
    fun aGardenKeptFromTheSheetIsToldKept() = extension.use {
        // Given a garden of a piece, never kept
        val library = FakeGardenLibrary()
        showViewModel(FakeGardenRepository(I_GARDEN), library)

        // When the player keeps it under the name proposed
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()
        onNodeWithText(string(R.string.feature_garden_keep)).performClick()
        onNodeWithText("Jardin calme").assertExists()
        onNodeWithText(string(R.string.feature_garden_keep_confirm)).performClick()

        // Then it is kept, and told
        onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.feature_garden_kept,
                "Jardin calme",
            ),
        )
            .assertExists()
        runOnIdle {
            assertThat(runBlocking { library.gardens.first() }.map { it.name }).containsExactly("Jardin calme")
        }
        // And the sheet opened again lists it as open, read in one
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()
        onNodeWithContentDescription("Jardin calme, Calme, 1 pièce, ouvert").assertExists()
    }

    @Test
    @DisplayName("a name another garden has is refused under the field, and Garder is greyed")
    fun aNameTakenIsRefused() = extension.use {
        // Given a garden kept as « Pluie douce », and another one under way
        val library = FakeGardenLibrary()
        runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        showViewModel(FakeGardenRepository(I_GARDEN), library)
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()
        onNodeWithText(string(R.string.feature_garden_keep)).performClick()

        // When the player types its name
        onNodeWithText("Jardin calme").performTextReplacement("pluie douce")

        // Then
        onNodeWithText(string(R.string.feature_garden_name_taken)).assertExists()
        onNode(hasText(string(R.string.feature_garden_keep_confirm))).assertIsNotEnabled()
    }

    @Test
    @DisplayName("the garden under way linked to a kept garden offers to save it in its place, or to keep a copy")
    fun aLinkedGardenOffersToSaveInPlaceOrCopy() = extension.use {
        // Given the garden under way changed since it was kept as « Pluie douce »
        val library = FakeGardenLibrary()
        val pluie = runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        val kept = FakeGardenRepository().apply { currentState.value = CurrentGarden(I_GARDEN, keptFrom = pluie.id) }
        showViewModel(kept, library)

        // When
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()

        // Then
        onNodeWithText("Enregistrer « Pluie douce »").assertExists()
        onNodeWithText(string(R.string.feature_garden_keep_copy)).assertExists()
        onNodeWithText(string(R.string.feature_garden_keep)).assertDoesNotExist()
    }

    @Test
    @DisplayName("a garden whose kept garden is no longer listed offers « Garder ce jardin »")
    fun aGardenWhoseKeptGardenIsGoneOffersToKeepIt() = extension.use {
        // Given the garden under way coming from a kept garden deleted since
        showState(
            GardenUiState(
                I_GARDEN,
                keptFrom = PLUIE.id,
                myGardens = MyGardens.Listed(emptyList()),
                sheet = GardensSheet(),
            ),
        )

        // When / Then
        onNodeWithText(string(R.string.feature_garden_keep)).assertExists()
        onNodeWithText(string(R.string.feature_garden_keep_copy)).assertDoesNotExist()
    }

    @Test
    @DisplayName("a garden as it was kept greys « Enregistrer « X » » and says why")
    fun aGardenAsKeptSaysWhy() = extension.use {
        // Given / When the sheet open on a garden as it was kept
        showState(
            GardenUiState(
                PLUIE.garden,
                keptFrom = PLUIE.id,
                myGardens = MyGardens.Listed(listOf(PLUIE)),
                sheet = GardensSheet(),
            ),
        )

        // Then
        onNodeWithText("Enregistrer « Pluie douce »").assertIsNotEnabled()
        onNodeWithText(string(R.string.feature_garden_unchanged)).assertExists()
    }

    private fun ComposeContext.showState(state: GardenUiState) {
        setContent { GardenScreen(uiState = state, actions = GardenActions.None) }
    }

    /** The screen on its view model, opened on the garden [kept], its kept gardens in [library]. */
    private fun ComposeContext.showViewModel(
        kept: FakeGardenRepository,
        library: FakeGardenLibrary,
    ) {
        val viewModel = GardenViewModel(
            kept = kept,
            library = library,
            appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            writes = Mutex(),
            music = FakeMusicPlayer(),
            soundEffects = FakeSoundEffectPlayer(),
            preferences = FakeUserPreferencesRepository(),
        )
        setContent {
            val state by viewModel.uiState.collectAsState()
            GardenScreen(uiState = state, actions = gardenActions(viewModel, onChangeGame = {}))
        }
    }

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private companion object {
        /** An I lying on the bottom row, in the calm mood. */
        val I_GARDEN: Garden = Garden.parse(
            List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n") + "\nIIII............",
        )

        /** « Pluie douce », the I of [I_GARDEN] heard in the joyful mood. */
        val PLUIE = KeptGarden(GardenId("pluie"), "Pluie douce", I_GARDEN.withMood(GardenMood.JOYFUL), Instant.EPOCH)
    }
}
