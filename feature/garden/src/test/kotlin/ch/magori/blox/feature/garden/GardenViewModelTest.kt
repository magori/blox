package ch.magori.blox.feature.garden

import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

internal class GardenViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val kept = FakeGardenRepository()

    /** What the scope of the application lets through, as Android would crash on it. */
    private val uncaught = mutableListOf<Throwable>()

    /** The scope of the application, where the garden is written, running at once. */
    private val appScope = CoroutineScope(
        SupervisorJob() + UnconfinedTestDispatcher() + CoroutineExceptionHandler { _, failure -> uncaught += failure },
    )

    @Test
    fun `the garden opens on the garden kept`() {
        // Given
        kept.state.value = A_GARDEN

        // When
        val viewModel = garden()

        // Then
        assertThat(viewModel.uiState.value?.garden).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a garden adopted is shown and kept`() {
        // Given
        val viewModel = garden()

        // When
        viewModel.adopt(A_GARDEN)

        // Then
        assertThat(viewModel.uiState.value?.garden).isEqualTo(A_GARDEN)
        assertThat(kept.state.value).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a garden that cannot be kept leaves the garden on screen`() {
        // Given a file that cannot be written, as one of a later version
        kept.failure = IOException("A garden of a later version")
        val viewModel = garden()

        // When
        viewModel.adopt(A_GARDEN)

        // Then the garden stays on screen, and nothing crashes
        assertThat(viewModel.uiState.value?.garden).isEqualTo(A_GARDEN)
        assertThat(kept.state.value).isEqualTo(Garden.EMPTY)
        assertThat(uncaught).isEmpty()
    }

    @Test
    fun `a failure other than an unwritable garden is not swallowed`() {
        // Given a bug in the repository
        val bug = IllegalStateException("A bug")
        kept.failure = bug
        val viewModel = garden()

        // When
        viewModel.adopt(A_GARDEN)

        // Then it goes through to the scope of the application
        assertThat(uncaught).containsExactly(bug)
    }

    @Test
    fun `the gardens adopted are kept in the order they were adopted`() {
        // Given a disk slow to write the first garden
        val slow = SlowGardenRepository(slowFirstWrite = true)
        val viewModel = garden(slow)

        // When a second garden is adopted while the first is being written
        viewModel.adopt(A_GARDEN)
        viewModel.adopt(ANOTHER_GARDEN)
        slow.firstWrite.complete(Unit)

        // Then the last one adopted is the one kept
        assertThat(slow.state.value).isEqualTo(ANOTHER_GARDEN)
    }

    @Test
    fun `a garden adopted while the garden kept is read stays on screen`() {
        // Given the garden kept slow to be read
        val slow = SlowGardenRepository(slowRead = true)
        val viewModel = garden(slow)

        // When a garden is adopted before the garden kept is read
        viewModel.adopt(A_GARDEN)
        slow.read.complete(ANOTHER_GARDEN)

        // Then the garden adopted stays on screen
        assertThat(viewModel.uiState.value?.garden).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a garden opens once the garden of the previous visit is kept`() {
        // Given a first visit whose garden is still being written
        val slow = SlowGardenRepository(slowFirstWrite = true)
        val factory = GardenViewModel.Factory(slow, appScope)
        visit(factory).adopt(A_GARDEN)

        // When the garden is opened again
        val next = visit(factory)

        // Then it waits for that garden, and opens on it once kept
        assertThat(next.uiState.value).isNull()
        slow.firstWrite.complete(Unit)
        assertThat(next.uiState.value?.garden).isEqualTo(A_GARDEN)
    }

    @Test
    fun `the gardens adopted in two visits are kept in their order`() {
        // Given a first visit whose garden is still being written
        val slow = SlowGardenRepository(slowFirstWrite = true)
        val factory = GardenViewModel.Factory(slow, appScope)
        visit(factory).adopt(A_GARDEN)

        // When the next visit adopts another garden
        visit(factory).adopt(ANOTHER_GARDEN)
        slow.firstWrite.complete(Unit)

        // Then the last one adopted is the one kept
        assertThat(slow.state.value).isEqualTo(ANOTHER_GARDEN)
    }

    /** A visit to the garden: its view model, as `app` builds it. */
    private fun visit(factory: GardenViewModel.Factory): GardenViewModel =
        factory.create(GardenViewModel::class, CreationExtras.Empty)

    private fun garden(repository: GardenRepository = kept) = GardenViewModel(repository, appScope, Mutex())

    /**
     * The garden kept on a slow disk: when [slowRead], the garden read is the one [read] gives, once let through; when
     * [slowFirstWrite], the first write waits for [firstWrite].
     */
    private class SlowGardenRepository(private val slowRead: Boolean = false, slowFirstWrite: Boolean = false) :
        GardenRepository {
        val read = CompletableDeferred<Garden>()
        val firstWrite = CompletableDeferred<Unit>().apply { if (!slowFirstWrite) complete(Unit) }
        val state = MutableStateFlow(Garden.EMPTY)
        private var writes = 0

        override val garden: Flow<Garden> get() = if (slowRead) flow { emit(read.await()) } else state

        override suspend fun keep(garden: Garden) {
            if (writes++ == 0) firstWrite.await()
            state.value = garden
        }
    }

    private companion object {
        /** No piece, heard in the epic mood. */
        val ANOTHER_GARDEN: Garden = Garden.EMPTY.withMood(GardenMood.EPIC)

        /** An I on the top row, heard in the joyful mood. */
        val A_GARDEN: Garden = Garden.parse(
            """
            IIII............
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            ................
            """,
        ).withMood(GardenMood.JOYFUL)
    }
}
