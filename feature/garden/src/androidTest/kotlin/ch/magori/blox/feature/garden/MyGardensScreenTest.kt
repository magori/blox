package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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
import ch.magori.blox.core.designsystem.component.SettledSheet
import ch.magori.blox.core.designsystem.theme.BloxTheme
import ch.magori.blox.core.designsystem.theme.BloxThemeKind
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
import ch.magori.blox.core.designsystem.R as DesignSystemR

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
        onNodeWithText("Un autre jardin porte déjà ce nom.").assertExists()
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

    @Test
    @DisplayName("a row touched opens its garden, told with Annuler, which gives back the garden from before")
    fun aRowTouchedOpensItsGarden() = extension.use {
        // Given « Pluie douce » kept, another garden under way
        val library = FakeGardenLibrary()
        val pluie = runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        val kept = FakeGardenRepository(I_GARDEN)
        showViewModel(kept, library)
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()

        // When its row is touched
        onNodeWithContentDescription("Pluie douce, Joyeux, 1 pièce").performClick()

        // Then it is opened and told, the sheet closed
        onNodeWithText("« Pluie douce » ouvert").assertExists()
        onNodeWithText(string(R.string.feature_garden_keep_copy)).assertDoesNotExist()
        runOnIdle { assertThat(kept.currentState.value).isEqualTo(CurrentGarden(pluie.garden, keptFrom = pluie.id)) }
        // And Annuler gives the garden from before back
        onNodeWithText(string(R.string.feature_garden_undo)).performClick()
        runOnIdle { assertThat(kept.currentState.value).isEqualTo(CurrentGarden(I_GARDEN, keptFrom = null)) }
    }

    @Test
    @DisplayName("the Annuler of a deletion, on the sheet going down once a garden is opened, gives nothing back")
    fun theAnnulerOfTheSheetGoingDownGivesNothingBack() = extension.use {
        // Given « Pluie douce » and « Orage » kept, the sheet open, « Orage » deleted, its Annuler in the sheet
        val library = FakeGardenLibrary()
        val pluie = runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        runBlocking { library.keep("Orage", I_GARDEN.withMood(GardenMood.EPIC)) }
        val kept = FakeGardenRepository(I_GARDEN)
        showViewModel(kept, library)
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()
        onNodeWithContentDescription("Plus d'actions pour Orage").performClick()
        onNodeWithText("Supprimer").performClick()
        onNode(hasText("Supprimer") and hasClickAction()).performClick()
        onNodeWithText("« Orage » supprimé.").assertExists()

        // When « Pluie douce » is opened, then, as the sheet goes down, its Annuler is touched
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Pluie douce, Joyeux, 1 pièce").performClick()
        mainClock.advanceTimeByFrame()
        onNode(hasText(string(R.string.feature_garden_undo)) and hasAnyAncestor(isDialog())).performClick()
        mainClock.advanceTimeBy(SETTLE_MS)

        // Then « Pluie douce » stays opened, and « Orage » deleted
        runOnIdle {
            assertThat(kept.currentState.value).isEqualTo(CurrentGarden(pluie.garden, keptFrom = pluie.id))
            assertThat(runBlocking { library.gardens.first() }.map { it.name }).containsExactly("Pluie douce")
        }
    }

    @Test
    @DisplayName("a row is read in one, opened by « Ouvrir », Renommer and Supprimer as its actions")
    fun aRowIsReadInOneWithItsActions() = extension.use {
        // Given / When the sheet open on « Pluie douce »
        showState(
            GardenUiState(I_GARDEN, myGardens = MyGardens.Listed(listOf(PLUIE)), sheet = GardensSheet()),
        )

        // Then
        onNodeWithContentDescription("Pluie douce, Joyeux, 1 pièce")
            .assert(
                SemanticsMatcher("opened by « Ouvrir »") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "Ouvrir"
                },
            )
            .assert(
                SemanticsMatcher("Renommer and Supprimer") { node ->
                    node.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label } ==
                        listOf("Renommer", "Supprimer")
                },
            )
    }

    @Test
    @DisplayName("Renommer from the menu of a row renames it under the name typed")
    fun renommerFromTheMenuRenamesTheGarden() = extension.use {
        // Given « Pluie douce » kept
        val library = FakeGardenLibrary()
        runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        showViewModel(FakeGardenRepository(I_GARDEN), library)
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()

        // When the player renames it from the menu of its row
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").performClick()
        onNodeWithText("Renommer").performClick()
        onNodeWithText("Renommer « Pluie douce »").assertExists()
        onNodeWithText("Pluie douce").performTextReplacement("Pluie fine")
        onNode(hasText("Renommer") and hasClickAction()).performClick()

        // Then
        onNodeWithContentDescription("Pluie fine, Joyeux, 1 pièce").assertExists()
        runOnIdle {
            assertThat(runBlocking { library.gardens.first() }.map { it.name }).containsExactly("Pluie fine")
        }
    }

    @Test
    @DisplayName("Supprimer from the menu asks to confirm, then tells the deletion with Annuler, which puts it back")
    fun supprimerFromTheMenuDeletesTheGardenWithAnnuler() = extension.use {
        // Given « Pluie douce » kept
        val library = FakeGardenLibrary()
        runBlocking { library.keep(PLUIE.name, PLUIE.garden) }
        showViewModel(FakeGardenRepository(I_GARDEN), library)
        onNodeWithContentDescription(string(R.string.feature_garden_my_gardens)).performClick()

        // When the player deletes it from the menu of its row
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").performClick()
        onNodeWithText("Supprimer").performClick()
        onNodeWithText(string(R.string.feature_garden_delete_keep)).assertExists()
        onNode(hasText("Supprimer") and hasClickAction()).performClick()

        // Then it leaves the list, its deletion told in the sheet
        onNodeWithContentDescription("Pluie douce, Joyeux, 1 pièce").assertDoesNotExist()
        onNodeWithText("« Pluie douce » supprimé.").assertExists()
        // And Annuler puts it back
        onNodeWithText(string(R.string.feature_garden_undo)).performClick()
        onNodeWithContentDescription("Pluie douce, Joyeux, 1 pièce").assertExists()
    }

    @Test
    @DisplayName("while a garden is being deleted, its window is greyed")
    fun aGardenBeingDeletedGreysItsWindow() = extension.use {
        // Given / When the deletion of « Pluie douce » being written
        showState(
            GardenUiState(
                I_GARDEN,
                myGardens = MyGardens.Listed(listOf(PLUIE)),
                sheet = GardensSheet(deleting = GardenDeleting(PLUIE), writing = true),
            ),
        )

        // Then
        onNode(hasText(string(R.string.feature_garden_delete_keep)) and hasClickAction()).assertIsNotEnabled()
        onNode(
            hasText(string(DesignSystemR.string.core_designsystem_window_delete)) and hasClickAction(),
        ).assertIsNotEnabled()
    }

    @Test
    @DisplayName("an Annuler that could not be written says the operation failed, Annuler still offered")
    fun anAnnulerThatFailedIsTold() = extension.use {
        // Given / When « Pluie douce » opened, its Annuler that could not be written
        showState(
            GardenUiState(
                PLUIE.garden,
                keptFrom = PLUIE.id,
                myGardens = MyGardens.Listed(listOf(PLUIE)),
                undo = GardenUndo.Opened(PLUIE.name, I_GARDEN, keptFrom = null),
                undoFailed = true,
            ),
        )

        // Then
        onNodeWithText(string(R.string.feature_garden_failed)).assertExists()
        onNodeWithText(string(R.string.feature_garden_undo)).assertExists()
    }

    @Test
    @DisplayName("from the keyboard, Renommer closed gives the focus back to the menu « … » of its row")
    fun renommerClosedGivesTheFocusBackToTheMenu() = extension.use {
        // Given the keyboard in use, the sheet open on « Pluie douce »
        val listed = GardenUiState(I_GARDEN, myGardens = MyGardens.Listed(listOf(PLUIE)), sheet = GardensSheet())
        var state by mutableStateOf(listed)
        val actions = GardenActions(
            onChangeGame = {},
            onRename = {
                state = state.copy(sheet = GardensSheet(naming = GardenNaming(PLUIE.name, renaming = PLUIE)))
            },
        )
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            BloxTheme(kind = BloxThemeKind.COLORFUL) {
                MyGardensSheetContent(state, actions, messageMillis = 0, undoMillis = 0) { title, bottom, content ->
                    SettledSheet(title, actions = bottom, content = content)
                }
            }
        }
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Renommer").performSemanticsAction(SemanticsActions.OnClick)
        // The window of the name opens: its state is seen before it closes.
        waitForIdle()

        // When the window of the name closes
        state = listed

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
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

        /** Longer than a sheet going down, 300 ms. */
        const val SETTLE_MS = 1_000L
    }
}
