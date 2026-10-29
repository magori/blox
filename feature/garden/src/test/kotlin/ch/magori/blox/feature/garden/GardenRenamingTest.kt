package ch.magori.blox.feature.garden

import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/** Renommer a kept garden, from the menu of its row in « Mes jardins » (docs/jardin-musical.md, Mes jardins). */
internal class GardenRenamingTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val library = FakeGardenLibrary()

    private val viewModel by lazy { gardenViewModel(FakeGardenRepository(), library = library) }

    @Test
    fun `Renommer opens the window of the name on the name of the garden`() = runTest {
        // Given
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()

        // When
        viewModel.startRenaming(pluie.id)

        // Then
        assertThat(viewModel.uiState.value?.sheet?.naming).isEqualTo(GardenNaming("Pluie douce", renaming = pluie))
    }

    @Test
    fun `a garden renamed may keep its own name, whatever its case, not the name of another one`() = runTest {
        // Given the window of the name open on « Pluie douce », beside « Orage »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        library.keep("Orage", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startRenaming(pluie.id)

        // When / Then
        viewModel.changeName("PLUIE douce")
        assertThat(viewModel.uiState.value?.sheet?.naming)
            .isEqualTo(GardenNaming("PLUIE douce", renaming = pluie))
        viewModel.changeName("orage")
        assertThat(viewModel.uiState.value?.sheet?.naming?.problem).isEqualTo(NameProblem.TAKEN)
    }

    @Test
    fun `a garden renamed takes the name typed, and the window closes, the sheet still open`() = runTest {
        // Given the window of the name open on « Pluie douce »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startRenaming(pluie.id)
        viewModel.changeName("  Pluie fine ")

        // When
        viewModel.confirmRenaming()

        // Then
        assertThat(library.gardens.first().map { it.name }).containsExactly("Pluie fine")
        assertThat(viewModel.uiState.value?.sheet).isEqualTo(GardensSheet())
    }

    @Test
    fun `a name refused renames nothing, the window still open`() = runTest {
        // Given the name of another garden typed for « Pluie douce »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        library.keep("Orage", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startRenaming(pluie.id)
        viewModel.changeName("ORAGE")

        // When
        viewModel.confirmRenaming()

        // Then
        assertThat(library.gardens.first().map { it.name }).containsExactly("Pluie douce", "Orage")
        assertThat(viewModel.uiState.value?.sheet?.naming?.name).isEqualTo("ORAGE")
    }

    @Test
    fun `a garden that cannot be renamed leaves the window open, and it tells it`() = runTest {
        // Given the window of the name open, on a disk that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.startRenaming(pluie.id)
        viewModel.changeName("Pluie fine")
        library.failure = IOException("A full disk")

        // When
        viewModel.confirmRenaming()

        // Then
        assertThat(viewModel.uiState.value?.sheet?.naming)
            .isEqualTo(GardenNaming("Pluie fine", failed = true, renaming = pluie))
    }

    @Test
    fun `while a garden is being renamed a second Renommer renames nothing more`() = runTest {
        // Given the window of the name open on « Pluie douce », on a slow disk
        val disk = FakeGardenLibrary()
        val pluie = disk.keep("Pluie douce", A_GARDEN)
        val slow = GatedGardenLibrary(disk)
        val viewModel = gardenViewModel(FakeGardenRepository(), library = slow)
        viewModel.openMyGardens()
        viewModel.startRenaming(pluie.id)
        viewModel.changeName("Pluie fine")

        // When Renommer is touched twice before it is written
        viewModel.confirmRenaming()
        viewModel.confirmRenaming()
        slow.gate.complete(Unit)

        // Then
        assertThat(slow.renames).isEqualTo(1)
    }

    private companion object {
        /** An I on the top row. */
        val A_GARDEN: Garden = Garden.parse(
            "IIII............\n" + List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n"),
        )
    }
}
