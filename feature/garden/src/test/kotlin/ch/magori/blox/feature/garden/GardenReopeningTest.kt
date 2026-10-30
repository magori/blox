package ch.magori.blox.feature.garden

import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.data.CurrentGarden
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import ch.magori.blox.core.garden.toScore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/** Rouvrir a kept garden from « Mes jardins » (docs/jardin-musical.md, Mes jardins, Rouvrir). */
internal class GardenReopeningTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val kept = FakeGardenRepository()

    private val library = FakeGardenLibrary()

    private val viewModel by lazy { gardenViewModel(kept, library = library) }

    @Test
    fun `a kept garden opened takes the place of the garden under way, which comes from it, and the sheet closes`() =
        runTest {
            // Given a garden under way, and « Pluie douce » kept
            val pluie = library.keep("Pluie douce", A_GARDEN)
            kept.state.value = A_GARDEN.withMood(GardenMood.EPIC).clear()
            viewModel.openMyGardens()

            // When the player touches its row
            viewModel.openKept(pluie.id, undoMillis = 4_000)

            // Then it is the garden under way, coming from « Pluie douce », on the device too
            val state = viewModel.uiState.value!!
            assertThat(state.garden).isEqualTo(A_GARDEN)
            assertThat(state.keptFrom).isEqualTo(pluie.id)
            assertThat(kept.currentState.value).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = pluie.id))
            assertThat(state.sheet).isNull()
        }

    @Test
    fun `a kept garden opened is told, with Annuler`() = runTest {
        // Given the garden under way coming from « Orage », and « Pluie douce » kept
        val orage = library.keep("Orage", A_GARDEN.withMood(GardenMood.EPIC))
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = orage.id)
        viewModel.openMyGardens()

        // When
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // Then
        assertThat(viewModel.uiState.value?.undo)
            .isEqualTo(GardenUndo.Opened("Pluie douce", Garden.EMPTY, keptFrom = orage.id))
    }

    @Test
    fun `Annuler gives back the garden from before, with the kept garden it came from`() = runTest {
        // Given « Pluie douce » opened in place of a garden coming from « Orage »
        val orage = library.keep("Orage", A_GARDEN.withMood(GardenMood.EPIC))
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(Garden.EMPTY, keptFrom = orage.id)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // When
        viewModel.undo()

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.garden).isEqualTo(Garden.EMPTY)
        assertThat(state.keptFrom).isEqualTo(orage.id)
        assertThat(state.undo).isNull()
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(Garden.EMPTY, keptFrom = orage.id))
    }

    @Test
    fun `touching the garden open, as it was kept, only closes the sheet`() = runTest {
        // Given the garden under way as « Pluie douce » was kept, nothing to be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        kept.failure = IOException("Not to be written")
        viewModel.openMyGardens()

        // When
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // Then the sheet closes, nothing else
        val state = viewModel.uiState.value!!
        assertThat(state.sheet).isNull()
        assertThat(state.undo).isNull()
    }

    @Test
    fun `the garden open but changed since is opened again as it was kept, with Annuler`() = runTest {
        // Given the garden under way changed since it was kept as « Pluie douce »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()

        // When
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.garden).isEqualTo(A_GARDEN)
        assertThat(state.undo)
            .isEqualTo(GardenUndo.Opened("Pluie douce", A_GARDEN.withMood(GardenMood.EPIC), keptFrom = pluie.id))
    }

    @Test
    fun `while listening, the garden opened, then the one Annuler gives back, are heard at the bar line`() = runTest {
        // Given the garden listened to, « Pluie douce » kept
        val music = FakeMusicPlayer()
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val viewModel = gardenViewModel(kept, music = music, library = library)
        viewModel.listen()
        viewModel.openMyGardens()

        // When « Pluie douce » is opened, then Annuler touched
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        viewModel.undo()

        // Then each one is switched to, without stopping the loop
        assertThat(music.switchedScores).containsExactly(A_GARDEN.toScore(), Garden.EMPTY.toScore())
        assertThat(music.calls).doesNotContain("stop")
    }

    @Test
    fun `a kept garden that cannot be opened leaves the sheet open and the garden under way, and it tells it`() =
        runTest {
            // Given « Pluie douce » kept, the garden under way that cannot be written
            val pluie = library.keep("Pluie douce", A_GARDEN)
            viewModel.openMyGardens()
            kept.failure = IOException("A full disk")

            // When
            viewModel.openKept(pluie.id, undoMillis = 4_000)

            // Then
            val state = viewModel.uiState.value!!
            assertThat(state.sheet).isEqualTo(GardensSheet(failed = true))
            assertThat(state.garden).isEqualTo(Garden.EMPTY)
            assertThat(state.keptFrom).isNull()
            assertThat(state.undo).isNull()
        }

    @Test
    fun `Annuler that cannot be written leaves the garden opened, and Annuler offered to try again`() = runTest {
        // Given « Pluie douce » opened, then the garden under way that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        kept.failure = IOException("A full disk")

        // When
        viewModel.undo()

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.garden).isEqualTo(A_GARDEN)
        assertThat(state.keptFrom).isEqualTo(pluie.id)
        assertThat(state.undo).isEqualTo(GardenUndo.Opened("Pluie douce", Garden.EMPTY, keptFrom = null))
    }

    @Test
    fun `while a kept garden is being opened the sheet is busy, and a second touch opens nothing more`() = runTest {
        // Given two kept gardens, on a slow disk
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val orage = library.keep("Orage", A_GARDEN.withMood(GardenMood.EPIC))
        val gate = CompletableDeferred<Unit>()
        val opened = mutableListOf<CurrentGarden>()
        val slow = object : GardenRepository by kept {
            override suspend fun keep(current: CurrentGarden) {
                gate.await()
                opened += current
                kept.keep(current)
            }
        }
        val viewModel = gardenViewModel(slow, library = library)
        viewModel.openMyGardens()

        // When a row is touched, then another before the first is written
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        val busy = viewModel.uiState.value?.sheet?.writing
        viewModel.openKept(orage.id, undoMillis = 4_000)
        gate.complete(Unit)

        // Then the sheet was busy meanwhile, and only the first one is opened
        assertThat(busy).isTrue()
        assertThat(opened).containsExactly(CurrentGarden(A_GARDEN, keptFrom = pluie.id))
    }

    @Test
    fun `a piece placed while Annuler is being written stays on screen, kept with the link shown`() = runTest {
        // Given « Pluie douce » opened, on a disk that turns slow
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val slow = GatedGardenRepository(kept)
        val viewModel = gardenViewModel(slow, library = library)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        val gate = CompletableDeferred<Unit>()
        slow.gate = gate

        // When Annuler is touched, then a piece placed before it is written
        viewModel.undo()
        viewModel.tap(Position(10, 5))
        viewModel.place()
        val placed = viewModel.uiState.value!!.garden
        gate.complete(Unit)

        // Then the piece placed stays, on screen and on the device, with the link shown
        val state = viewModel.uiState.value!!
        assertThat(placed.pieces).hasSize(2)
        assertThat(state.garden).isEqualTo(placed)
        assertThat(state.keptFrom).isEqualTo(pluie.id)
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(placed, keptFrom = pluie.id))
    }

    @Test
    fun `a kept garden being opened while the sheet is closed and a piece placed leaves the garden shown`() = runTest {
        // Given « Pluie douce » being opened on a slow disk
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val gate = CompletableDeferred<Unit>()
        val slow = GatedGardenRepository(kept).apply { this.gate = gate }
        val viewModel = gardenViewModel(slow, library = library)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // When the sheet is closed, then a piece placed, before it is written
        viewModel.closeMyGardens()
        viewModel.tap(Position(10, 5))
        viewModel.place()
        val placed = viewModel.uiState.value!!.garden
        gate.complete(Unit)

        // Then the garden shown stays, from no kept garden, on the device too
        val state = viewModel.uiState.value!!
        assertThat(state.garden).isEqualTo(placed)
        assertThat(state.keptFrom).isNull()
        assertThat(state.undo).isNull()
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(placed, keptFrom = null))
    }

    @Test
    fun `a kept garden opened shows no preview`() = runTest {
        // Given a preview shown on the garden under way
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.tap(Position(10, 5))
        viewModel.openMyGardens()

        // When
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // Then
        assertThat(viewModel.uiState.value?.aim).isNull()
    }

    @Test
    fun `a kept garden opened falls into place as after Au hasard, a change of its own`() = runTest {
        // Given « Pluie douce » kept, and the sheet open
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        val move = viewModel.uiState.value!!.move

        // When
        viewModel.openKept(pluie.id, undoMillis = 4_000)

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.change).isEqualTo(GardenChange.Opened)
        assertThat(state.move).isEqualTo(move + 1)
    }

    @Test
    fun `Annuler after an opening gives the garden from before back at once, without a fall`() = runTest {
        // Given « Pluie douce » opened
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        val move = viewModel.uiState.value!!.move

        // When
        viewModel.undo()

        // Then a change of the garden that the screen does not animate
        val state = viewModel.uiState.value!!
        assertThat(state.change).isNull()
        assertThat(state.move).isEqualTo(move + 1)
    }

    @Test
    fun `while Annuler is being written a second Annuler gives back nothing more`() = runTest {
        // Given « Pluie douce » opened, on a disk that turns slow
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val slow = GatedGardenRepository(kept)
        val viewModel = gardenViewModel(slow, library = library)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        val gate = CompletableDeferred<Unit>()
        slow.gate = gate
        val before = slow.linkWrites

        // When Annuler is touched twice before it is written
        viewModel.undo()
        viewModel.undo()
        gate.complete(Unit)

        // Then
        assertThat(slow.linkWrites - before).isEqualTo(1)
    }

    @Test
    fun `an Annuler that cannot be written is told`() = runTest {
        // Given « Pluie douce » opened, then the garden under way that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        viewModel.openMyGardens()
        viewModel.openKept(pluie.id, undoMillis = 4_000)
        kept.failure = IOException("A full disk")

        // When
        viewModel.undo()

        // Then
        assertThat(viewModel.uiState.value?.undoFailed).isTrue()
    }

    private companion object {
        /** An I on the top row, heard in the joyful mood. */
        val A_GARDEN: Garden = Garden.parse(
            "IIII............\n" + List(Garden.ROWS - 1) { ".".repeat(Garden.COLUMNS) }.joinToString("\n"),
        ).withMood(GardenMood.JOYFUL)
    }
}
