package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import java.time.Instant

/** Supprimer a kept garden, from the menu of its row in « Mes jardins », and Annuler (docs/jardin-musical.md). */
internal class GardenDeletingTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val kept = FakeGardenRepository()

    private val library = FakeGardenLibrary()

    private val viewModel by lazy { gardenViewModel(kept, library = library) }

    @Test
    fun `Supprimer asks to confirm the deletion of the garden`() = runTest {
        // Given
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()

        // When
        viewModel.startDeleting(pluie.id)

        // Then
        assertThat(viewModel.uiState.value?.sheet?.deleting).isEqualTo(GardenDeleting(pluie))
    }

    @Test
    fun `Garder le jardin closes the window and deletes nothing, the sheet still open`() = runTest {
        // Given the deletion of « Pluie douce » to confirm
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)

        // When
        viewModel.cancelDeleting()

        // Then
        assertThat(viewModel.uiState.value?.sheet).isEqualTo(GardensSheet())
        assertThat(library.gardens.first()).containsExactly(pluie)
    }

    @Test
    fun `a garden deleted leaves the list, and its deletion is told with Annuler, the sheet still open`() = runTest {
        // Given the deletion of « Orage » to confirm, after « Pluie douce »
        library.keep("Pluie douce", A_GARDEN)
        val orage = library.keep("Orage", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(orage.id)

        // When
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then
        assertThat(library.gardens.first().map { it.name }).containsExactly("Pluie douce")
        val state = viewModel.uiState.value!!
        assertThat(state.sheet).isEqualTo(GardensSheet())
        assertThat(state.undo).isEqualTo(GardenUndo.Deleted(DeletedGarden(orage, place = 1), linked = false))
    }

    @Test
    fun `Annuler puts the deleted garden back at its place, and is no longer offered`() = runTest {
        // Given « Pluie douce » deleted, before « Orage »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val orage = library.keep("Orage", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        viewModel.confirmDeleting(undoMillis = 4_000)

        // When
        viewModel.undo()

        // Then
        assertThat(library.gardens.first()).containsExactly(pluie, orage)
        assertThat(viewModel.uiState.value?.undo).isNull()
    }

    @Test
    fun `the garden open deleted stays on screen, no longer coming from it`() = runTest {
        // Given the garden under way changed since it was kept as « Pluie douce »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = pluie.id)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)

        // When
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.garden).isEqualTo(Garden.EMPTY)
        assertThat(state.keptFrom).isNull()
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(Garden.EMPTY, keptFrom = null))
        assertThat(state.undo).isEqualTo(GardenUndo.Deleted(DeletedGarden(pluie, place = 0), linked = true))
    }

    @Test
    fun `Annuler after the deletion of the garden open makes the garden under way come from it again`() = runTest {
        // Given the garden open deleted
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = pluie.id)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        viewModel.confirmDeleting(undoMillis = 4_000)

        // When
        viewModel.undo()

        // Then
        assertThat(viewModel.uiState.value?.keptFrom).isEqualTo(pluie.id)
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(Garden.EMPTY, keptFrom = pluie.id))
        assertThat(library.gardens.first()).containsExactly(pluie)
    }

    @Test
    fun `a garden that cannot be deleted leaves the window open, and it tells it`() = runTest {
        // Given the deletion of « Pluie douce » to confirm, on a disk that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        library.failure = IOException("A full disk")

        // When
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then
        assertThat(viewModel.uiState.value?.sheet?.deleting).isEqualTo(GardenDeleting(pluie, failed = true))
        assertThat(viewModel.uiState.value?.undo).isNull()
    }

    @Test
    fun `the garden open whose link cannot be removed is not deleted, and the window tells it`() = runTest {
        // Given the deletion of the garden open to confirm, the garden under way that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = pluie.id)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        kept.failure = IOException("A full disk")

        // When
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then nothing is deleted, the garden still linked, and the window tells the failure
        assertThat(library.gardens.first()).containsExactly(pluie)
        val state = viewModel.uiState.value!!
        assertThat(state.keptFrom).isEqualTo(pluie.id)
        assertThat(state.sheet?.deleting).isEqualTo(GardenDeleting(pluie, failed = true))
        assertThat(state.undo).isNull()
    }

    @Test
    fun `the garden open deleted while the screen is left no longer comes from it all the same`() {
        // Given the deletion of the garden open confirmed, on a slow disk
        val disk = FakeGardenLibrary()
        val pluie = runBlocking { disk.keep("Pluie douce", A_GARDEN) }
        val slow = GatedGardenLibrary(disk)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, factory(slow))[GardenViewModel::class]
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        viewModel.confirmDeleting(undoMillis = 4_000)

        // When the screen is left before it is written
        store.clear()
        slow.gate.complete(Unit)

        // Then
        assertThat(runBlocking { slow.gardens.first() }).isEmpty()
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = null))
    }

    @Test
    fun `Annuler that puts the garden open back but cannot link it again tells it, offered to try again`() = runTest {
        // Given the garden open deleted, then the garden under way that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = pluie.id)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        viewModel.confirmDeleting(undoMillis = 4_000)
        kept.failure = IOException("A full disk")

        // When
        viewModel.undo()

        // Then the garden is back, not linked, and Annuler tells the failure, to try again
        assertThat(library.gardens.first()).containsExactly(pluie)
        val state = viewModel.uiState.value!!
        assertThat(state.keptFrom).isNull()
        assertThat(state.undoFailed).isTrue()
        assertThat(state.undo).isInstanceOf(GardenUndo.Deleted::class.java)
    }

    @Test
    fun `Annuler refused by a library full again since the deletion is told`() = runTest {
        // Given « Pluie douce » deleted, then the library filled again
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        viewModel.confirmDeleting(undoMillis = 4_000)
        repeat(GardenLibrary.MAX_GARDENS) { library.keep("Jardin ${it + 1}", A_GARDEN) }

        // When
        viewModel.undo()

        // Then the failure is told, nothing put back
        assertThat(viewModel.uiState.value?.undoFailed).isTrue()
        assertThat(library.gardens.first()).hasSize(GardenLibrary.MAX_GARDENS)
    }

    /** The factory of the view model, as `app` builds it, on [library]. */
    private fun factory(library: GardenLibrary) = GardenViewModel.Factory(
        kept,
        library,
        CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
        FakeMusicPlayer(),
        FakeSoundEffectPlayer(),
        FakeUserPreferencesRepository(),
    )

    @Test
    fun `while a garden is being deleted a second Supprimer deletes nothing more`() = runTest {
        // Given the deletion of « Pluie douce » to confirm, on a slow disk
        val disk = FakeGardenLibrary()
        val pluie = disk.keep("Pluie douce", A_GARDEN)
        val slow = GatedGardenLibrary(disk)
        val viewModel = gardenViewModel(kept, library = slow)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)

        // When Supprimer is touched twice before it is written
        viewModel.confirmDeleting(undoMillis = 4_000)
        val busy = viewModel.uiState.value?.sheet?.writing
        viewModel.confirmDeleting(undoMillis = 4_000)
        slow.gate.complete(Unit)

        // Then the sheet was busy meanwhile, and a single deletion asked for
        assertThat(busy).isTrue()
        assertThat(slow.deletes).isEqualTo(1)
    }

    @Test
    fun `the garden open that cannot be deleted stays listed, no longer linked, and the window tells it`() = runTest {
        // Given the deletion of the garden open to confirm, the library that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = pluie.id)
        viewModel.openMyGardens()
        viewModel.startDeleting(pluie.id)
        library.failure = IOException("A full disk")

        // When
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then the link is removed first, the garden is still kept, and the failure told
        assertThat(library.gardens.first()).containsExactly(pluie)
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(Garden.EMPTY, keptFrom = null))
        val state = viewModel.uiState.value!!
        assertThat(state.keptFrom).isNull()
        assertThat(state.sheet?.deleting).isEqualTo(GardenDeleting(pluie, failed = true))
        assertThat(state.undo).isNull()
    }

    @Test
    fun `a garden deleted keeps its row at its place, no longer listed, while its deletion may be undone`() = runTest {
        // Given three gardens kept at the same time, listed from the last one kept
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val orage = library.keep("Orage", A_GARDEN)
        val neige = library.keep("Neige", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startDeleting(orage.id)

        // When the one in the middle is deleted
        viewModel.confirmDeleting(undoMillis = 4_000)

        // Then its row stays between the two others, as it leaves
        assertThat(viewModel.uiState.value?.sheetRows).containsExactly(
            SheetRow(neige, listed = true),
            SheetRow(orage, listed = false),
            SheetRow(pluie, listed = true),
        )
    }

    @Test
    fun `a garden being deleted keeps its row, no longer listed, once the library no longer holds it`() {
        // Given « Orage » being deleted, kept between the two others, the library holding them alone already
        val pluie = A_KEPT.copy(id = GardenId("pluie"), name = "Pluie douce")
        val orage = A_KEPT.copy(id = GardenId("orage"), name = "Orage", modifiedAt = A_KEPT.modifiedAt.plusSeconds(1))
        val neige = A_KEPT.copy(id = GardenId("neige"), name = "Neige", modifiedAt = A_KEPT.modifiedAt.plusSeconds(2))
        val state = GardenUiState(
            garden = A_GARDEN,
            myGardens = MyGardens.Listed(listOf(pluie, neige)),
            sheet = GardensSheet(writing = true, deleting = GardenDeleting(orage)),
        )

        // Then
        assertThat(state.sheetRows).containsExactly(
            SheetRow(neige, listed = true),
            SheetRow(orage, listed = false),
            SheetRow(pluie, listed = true),
        )
    }

    private companion object {
        /** A garden kept, at the time of the library of the tests. */
        val A_KEPT = KeptGarden(GardenId("a"), "A", Garden.EMPTY, Instant.EPOCH)

        /** An I on the top row. */
        val A_GARDEN: Garden = Garden.parse(
            "IIII............\n" + List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n"),
        )
    }
}
