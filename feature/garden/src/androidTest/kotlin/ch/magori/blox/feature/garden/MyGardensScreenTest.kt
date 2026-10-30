package ch.magori.blox.feature.garden

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
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
import ch.magori.blox.core.data.DeletedGarden
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.designsystem.component.SettledSheet
import ch.magori.blox.core.designsystem.theme.BloxEffects
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        showSheet({ state }, actions)
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Renommer").performSemanticsAction(SemanticsActions.OnClick)
        // The window of the name opens: its state is seen before it closes.
        waitForIdle()

        // When the window of the name closes
        state = listed

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, a garden deleted from its menu gives the focus to the menu « … » of the next row")
    fun aGardenDeletedGivesTheFocusToTheNextRow() = extension.use {
        // Given / When, from the keyboard, « Ciel clair » deleted, listed above « Pluie douce »
        deleteFromKeyboard(library = listOf(PLUIE, CIEL), deleted = CIEL)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, the bottom garden deleted gives the focus to the menu « … » of the row above")
    fun theLastGardenDeletedGivesTheFocusToTheRowAbove() = extension.use {
        // Given / When, from the keyboard, « Pluie douce » deleted, listed under « Ciel clair »
        deleteFromKeyboard(library = listOf(PLUIE, CIEL), deleted = PLUIE)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Ciel clair").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, the only garden deleted gives the focus to the empty list")
    fun theOnlyGardenDeletedGivesTheFocusToTheEmptyList() = extension.use {
        // Given / When, from the keyboard, « Pluie douce », the only garden, deleted
        deleteFromKeyboard(library = listOf(PLUIE), deleted = PLUIE)

        // Then
        onNode(
            hasText(string(R.string.feature_garden_none_kept)) and
                hasText(string(R.string.feature_garden_none_kept_hint)),
        ).assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, a deletion not confirmed gives the focus back to the menu « … » of its row")
    fun aDeletionNotConfirmedGivesTheFocusBackToTheMenu() = extension.use {
        // Given / When, from the keyboard, the deletion of « Ciel clair » not confirmed
        deleteFromKeyboard(library = listOf(PLUIE, CIEL), deleted = CIEL, confirmed = false)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Ciel clair").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, the garden in the middle deleted gives the focus to the menu « … » of the row under it",
    )
    fun theMiddleGardenDeletedGivesTheFocusToTheRowUnderIt() = extension.use {
        // Given / When, from the keyboard, « Ciel clair » deleted, between « Soleil » above and « Pluie douce » under it
        deleteFromKeyboard(library = listOf(PLUIE, CIEL, SOLEIL), deleted = CIEL)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, a garden deleted whose list is read after its window closed still gives the focus on",
    )
    fun aGardenDeletedListedLateStillGivesTheFocusOn() = extension.use {
        // Given « Pluie douce » then « Ciel clair » kept, the list read late, and the sheet open, from the keyboard
        val fake = FakeGardenLibrary()
        runBlocking {
            fake.keep(PLUIE.name, PLUIE.garden)
            fake.keep(CIEL.name, CIEL.garden)
        }
        val library = LateGardenLibrary(fake)
        runBlocking { library.show() }
        val viewModel = viewModelOf(FakeGardenRepository(I_GARDEN), library)
        val actions = gardenActions(viewModel, onChangeGame = {})
        showSheet({ viewModel.uiState.collectAsState().value ?: GardenUiState(I_GARDEN) }, actions)
        runOnIdle { actions.onOpenMyGardens() }
        openDeletion(CIEL)

        // When the deletion is confirmed, its window closed before the list without « Ciel clair » is read
        runOnIdle { actions.onConfirmDeleting(UNDO_MS) }
        waitUntil { onAllNodesWithText("« Ciel clair » supprimé.").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { library.show() }

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, a garden out of the list before its window closes gives the focus on as it closes")
    fun aGardenOutOfTheListBeforeItsWindowClosesGivesTheFocusOn() = extension.use {
        // Given, from the keyboard, the window of the deletion of « Ciel clair » open, above « Pluie douce »
        val sheet = SheetUnderTest(listOf(PLUIE, CIEL))
        showSheet({ sheet.state }, sheet.actions)
        openDeletion(CIEL)

        // When the list is read without it, the window still open, then the window closes
        sheet.listWithout(CIEL)
        waitForIdle()
        sheet.confirm(CIEL)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, the middle garden out of the list before its window closes gives the focus to the next row",
    )
    fun theMiddleGardenOutOfTheListBeforeItsWindowClosesGivesTheFocusToTheRowUnderIt() = extension.use {
        // Given, from the keyboard, the window of the deletion of « Ciel clair » open, between « Soleil » above and
        // « Pluie douce » under it
        val sheet = SheetUnderTest(listOf(PLUIE, CIEL, SOLEIL))
        showSheet({ sheet.state }, sheet.actions)
        openDeletion(CIEL)

        // When the list is read without it, the window still open, then the window closes
        sheet.listWithout(CIEL)
        waitForIdle()
        sheet.confirm(CIEL)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    @Test
    @DisplayName(
        "from the keyboard, the only garden out of the list before its window closes gives the focus to the empty list",
    )
    fun theOnlyGardenOutOfTheListBeforeItsWindowClosesGivesTheFocusToTheEmptyList() = extension.use {
        // Given, from the keyboard, the window of the deletion of « Pluie douce », the only garden, open
        val sheet = SheetUnderTest(listOf(PLUIE))
        showSheet({ sheet.state }, sheet.actions)
        openDeletion(PLUIE)

        // When the list is read without it, the window still open, then the window closes
        sheet.listWithout(PLUIE)
        waitForIdle()
        sheet.confirm(PLUIE)

        // Then
        onNode(hasText(string(R.string.feature_garden_none_kept))).assertIsFocused()
    }

    @Test
    @DisplayName("by touch, a garden deleted gives the focus to nothing, not even the empty list")
    fun byTouchAGardenDeletedGivesTheFocusToNothing() = extension.use {
        // Given, by touch, the window of the deletion of « Pluie douce », the only garden, open
        // Touch mode is the device's: a test before may have left it for the keyboard.
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        val sheet = SheetUnderTest(listOf(PLUIE))
        showSheet({ sheet.state }, sheet.actions, keyboard = false)
        openDeletion(PLUIE, keyboard = false)

        // When the deletion is confirmed
        sheet.confirm(PLUIE)

        // Then
        onNode(hasText(string(R.string.feature_garden_none_kept))).assertIsNotFocused()
    }

    @Test
    @DisplayName("from the keyboard, after a deletion not confirmed, the focus follows the next deletion confirmed")
    fun afterADeletionNotConfirmedTheFocusFollowsTheNext() = extension.use {
        // Given, from the keyboard, the deletion of « Ciel clair » not confirmed, above « Pluie douce »
        val sheet = SheetUnderTest(listOf(PLUIE, CIEL))
        showSheet({ sheet.state }, sheet.actions)
        openDeletion(CIEL)
        sheet.cancel()
        waitForIdle()

        // When « Pluie douce » is deleted
        openDeletion(PLUIE)
        sheet.confirm(PLUIE)

        // Then the focus goes to the row above it
        onNodeWithContentDescription("Plus d'actions pour Ciel clair").assertIsFocused()
    }

    @Test
    @DisplayName("from the keyboard, in reduced effects, a garden deleted gives the focus to the « … » of the next row")
    fun inReducedEffectsAGardenDeletedGivesTheFocusToTheNextRow() = extension.use {
        // Given / When, from the keyboard, in reduced effects, « Ciel clair » deleted, listed above « Pluie douce »
        deleteFromKeyboard(library = listOf(PLUIE, CIEL), deleted = CIEL, effects = REDUCED)

        // Then
        onNodeWithContentDescription("Plus d'actions pour Pluie douce").assertIsFocused()
    }

    /**
     * The keyboard in use, the sheet listing the gardens of [library], in its order, in [effects]; [deleted] deleted
     * from its menu « … »: its window opens, then closes as the deletion is [confirmed], and it leaves the list, or as
     * it is not.
     */
    private fun ComposeContext.deleteFromKeyboard(
        library: List<KeptGarden>,
        deleted: KeptGarden,
        confirmed: Boolean = true,
        effects: BloxEffects = BloxEffects.Full,
    ) {
        val sheet = SheetUnderTest(library)
        showSheet({ sheet.state }, sheet.actions, effects)
        openDeletion(deleted)
        if (confirmed) sheet.confirm(deleted) else sheet.cancel()
    }

    /** The window of the deletion of [garden] opened from its menu « … », from the [keyboard] or by touch. */
    private fun ComposeContext.openDeletion(
        garden: KeptGarden,
        keyboard: Boolean = true,
    ) {
        listOf(onNodeWithContentDescription("Plus d'actions pour ${garden.name}"), onNodeWithText("Supprimer"))
            .forEach { if (keyboard) it.performSemanticsAction(SemanticsActions.OnClick) else it.performClick() }
        // The window of the deletion opens: its state is seen before it closes.
        waitForIdle()
    }

    /**
     * The sheet « Mes jardins » settled, as [state] says, in [effects], the [keyboard] in use, or touch; the windows
     * are not drawn.
     */
    private fun ComposeContext.showSheet(
        state: @Composable () -> GardenUiState,
        actions: GardenActions,
        effects: BloxEffects = BloxEffects.Full,
        keyboard: Boolean = true,
    ) {
        setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(if (keyboard) InputMode.Keyboard else InputMode.Touch) }
            BloxTheme(kind = BloxThemeKind.COLORFUL, effects = effects) {
                MyGardensSheetContent(state(), actions, messageMillis = 0, undoMillis = 0) { title, bottom, content ->
                    SettledSheet(title, actions = bottom, content = content)
                }
            }
        }
    }

    /** The state of the sheet listing the gardens of [library], in its order, as a test changes it. */
    private class SheetUnderTest(private val library: List<KeptGarden>) {
        private val listed = GardenUiState(I_GARDEN, myGardens = MyGardens.Listed(library), sheet = GardensSheet())

        var state by mutableStateOf(listed)
            private set

        /** Supprimer chosen in the menu of a row opens the window of its deletion. */
        val actions = GardenActions(
            onChangeGame = {},
            onDelete = { id -> state = state.copy(sheet = GardensSheet(deleting = GardenDeleting(gardenOf(id)))) },
        )

        /** The deletion not confirmed: the window closes, the gardens as they were. */
        fun cancel() {
            state = listed
        }

        /** The list read without [deleted], its window still open. */
        fun listWithout(deleted: KeptGarden) {
            state = state.copy(myGardens = MyGardens.Listed(library - deleted))
        }

        /** The deletion of [deleted] confirmed: the window closes, the garden out of the list, Annuler offered. */
        fun confirm(deleted: KeptGarden) {
            state = GardenUiState(
                I_GARDEN,
                myGardens = MyGardens.Listed(library - deleted),
                sheet = GardensSheet(),
                undo = GardenUndo.Deleted(DeletedGarden(deleted, place = library.indexOf(deleted)), linked = false),
            )
        }

        private fun gardenOf(id: GardenId): KeptGarden = library.first { it.id == id }
    }

    private fun ComposeContext.showState(state: GardenUiState) {
        setContent { GardenScreen(uiState = state, actions = GardenActions.None) }
    }

    /** The screen on its view model, opened on the garden [kept], its kept gardens in [library]. */
    private fun ComposeContext.showViewModel(
        kept: FakeGardenRepository,
        library: FakeGardenLibrary,
    ) {
        val viewModel = viewModelOf(kept, library)
        setContent {
            val state by viewModel.uiState.collectAsState()
            GardenScreen(uiState = state, actions = gardenActions(viewModel, onChangeGame = {}))
        }
    }

    /** The view model of the garden [kept], its kept gardens in [library]. */
    private fun viewModelOf(
        kept: FakeGardenRepository,
        library: GardenLibrary,
    ): GardenViewModel = GardenViewModel(
        kept = kept,
        library = library,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        writes = Mutex(),
        music = FakeMusicPlayer(),
        soundEffects = FakeSoundEffectPlayer(),
        preferences = FakeUserPreferencesRepository(),
    )

    /** The gardens of [library], seen only once the test shows them ([show]): a list read late. */
    private class LateGardenLibrary(private val library: FakeGardenLibrary) : GardenLibrary by library {
        private val shown = MutableStateFlow<List<KeptGarden>>(emptyList())

        override val gardens: Flow<List<KeptGarden>> = shown

        suspend fun show() {
            shown.value = library.gardens.first()
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

        /** The effects reduced by the player. */
        val REDUCED = BloxEffects(reduced = true, forcedBySystem = false)

        /** « Ciel clair », kept after [PLUIE]: listed above it. */
        val CIEL = KeptGarden(GardenId("ciel"), "Ciel clair", I_GARDEN, Instant.EPOCH.plusSeconds(1))

        /** « Soleil », kept after [CIEL]: listed above it. */
        val SOLEIL = KeptGarden(GardenId("soleil"), "Soleil", I_GARDEN, Instant.EPOCH.plusSeconds(2))

        /** Longer than a sheet going down, 300 ms. */
        const val SETTLE_MS = 1_000L

        /** Longer than a test: « Annuler » stays offered. */
        const val UNDO_MS = 60_000L
    }
}
