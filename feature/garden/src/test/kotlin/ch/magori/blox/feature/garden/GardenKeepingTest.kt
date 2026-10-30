package ch.magori.blox.feature.garden

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import ch.magori.blox.core.audio.FakeMusicPlayer
import ch.magori.blox.core.audio.FakeSoundEffectPlayer
import ch.magori.blox.core.data.CurrentGarden
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.FakeUserPreferencesRepository
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.MainDispatcherExtension
import ch.magori.blox.core.data.MutableClock
import ch.magori.blox.core.data.NameProblem
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

/** « Mes jardins » from the garden (docs/jardin-musical.md, Mes jardins): its disc, its sheet, keeping a garden. */
internal class GardenKeepingTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val kept = FakeGardenRepository()

    private val clock = MutableClock()

    private val library = FakeGardenLibrary(clock)

    private val viewModel by lazy { gardenViewModel(kept, library = library) }

    @Test
    fun `a garden that comes from no kept garden is not marked as changed`() {
        // Given a garden never kept
        kept.state.value = A_GARDEN

        // When / Then
        assertThat(viewModel.uiState.value?.modified).isFalse()
    }

    @Test
    fun `a garden opened from a kept garden and not changed since is not marked as changed`() = runTest {
        // Given the garden under way coming from a kept garden, as it was kept
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = kept.currentState.value.copy(garden = A_GARDEN, keptFrom = pluie.id)

        // When / Then
        assertThat(viewModel.uiState.value?.modified).isFalse()
    }

    @Test
    fun `a garden changed since it was kept is marked as changed`() = runTest {
        // Given the garden under way coming from a kept garden
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = kept.currentState.value.copy(garden = A_GARDEN, keptFrom = pluie.id)

        // When the player changes it
        viewModel.chooseMood(GardenMood.EPIC)

        // Then
        assertThat(viewModel.uiState.value?.modified).isTrue()
    }

    @Test
    fun `Mes jardins opens its sheet, and closes it`() {
        // Given
        val viewModel = viewModel

        // When
        viewModel.openMyGardens()

        // Then
        assertThat(viewModel.uiState.value?.sheet).isEqualTo(GardensSheet())
        viewModel.closeMyGardens()
        assertThat(viewModel.uiState.value?.sheet).isNull()
    }

    @Test
    fun `the sheet lists the kept gardens from the most recent to the oldest`() = runTest {
        // Given three gardens kept one after the other, the second one replaced since
        library.keep("Pluie douce", A_GARDEN)
        val second = library.keep("Brume", A_GARDEN)
        clock.now = clock.now.plusSeconds(1)
        library.keep("Orage", A_GARDEN)
        clock.now = clock.now.plusSeconds(1)
        library.replace(second.id, Garden.EMPTY)

        // When / Then
        assertThat(
            viewModel.uiState.value?.keptGardens?.map {
                it.name
            },
        ).containsExactly("Brume", "Orage", "Pluie douce")
    }

    @Test
    fun `gardens kept at the same time are listed from the last one kept`() = runTest {
        // Given two gardens kept at the same millisecond
        library.keep("Pluie douce", A_GARDEN)
        library.keep("Brume", A_GARDEN)

        // When / Then
        assertThat(viewModel.uiState.value?.keptGardens?.map { it.name }).containsExactly("Brume", "Pluie douce")
    }

    @Test
    fun `the kept gardens are loading until the library answers`() {
        // Given a library that does not answer
        val viewModel = gardenViewModel(kept, library = SilentGardenLibrary())

        // When / Then
        assertThat(viewModel.uiState.value?.myGardens).isEqualTo(MyGardens.Loading)
        assertThat(viewModel.uiState.value?.keptGardens).isEmpty()
    }

    @Test
    fun `gardens that cannot be read, as those of a later version, are told unreadable`() {
        // Given a library whose file cannot be read: an empty list, then the flow ends
        val viewModel = gardenViewModel(kept, library = UnreadableGardenLibrary())

        // When / Then
        assertThat(viewModel.uiState.value?.myGardens).isEqualTo(MyGardens.Unreadable)
    }

    @Test
    fun `an empty garden cannot be kept, and the sheet says why`() {
        // Given the empty garden
        val viewModel = viewModel

        // When the player wants to keep it
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Calme")

        // Then no name is asked for
        assertThat(viewModel.uiState.value?.keepRefusal).isEqualTo(KeepRefusal.EMPTY_GARDEN)
        assertThat(viewModel.uiState.value?.sheet?.naming).isNull()
    }

    @Test
    fun `a library of 30 gardens refuses another one, and the sheet says why`() = runTest {
        // Given a full library
        repeat(GardenLibrary.MAX_GARDENS) { library.keep("Jardin ${it + 1}", A_GARDEN) }
        kept.state.value = A_GARDEN

        // When the player wants to keep the garden under way
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // Then
        assertThat(viewModel.uiState.value?.keepRefusal).isEqualTo(KeepRefusal.FULL)
        assertThat(viewModel.uiState.value?.sheet?.naming).isNull()
    }

    @Test
    fun `nothing is kept while the gardens are not read, nor when they cannot be`() {
        // Given a garden to keep
        kept.state.value = A_GARDEN

        val loading = SilentGardenLibrary()
        val unreadable = UnreadableGardenLibrary()

        // When the player tries to keep it, the gardens not read, then unreadable
        val refusals = listOf(loading, unreadable).map { library ->
            val viewModel = gardenViewModel(kept, library = library)
            viewModel.openMyGardens()
            viewModel.startKeeping(moodName = "Joyeux")
            viewModel.confirmKeeping(messageMillis = 4_000)
            assertThat(viewModel.uiState.value?.sheet?.naming).isNull()
            viewModel.uiState.value?.keepRefusal
        }

        // Then nothing is asked of the libraries, and the sheet says why
        assertThat(refusals).containsExactly(KeepRefusal.LOADING, KeepRefusal.UNREADABLE)
        assertThat(loading.keeps + unreadable.keeps).isZero()
    }

    @Test
    fun `Garder ce jardin proposes « Jardin » then the mood in lower case, numbered if it is taken`() = runTest {
        // Given a garden called after the joyful mood already
        library.keep("Jardin joyeux", A_GARDEN)
        kept.state.value = A_GARDEN

        // When
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // Then
        assertThat(viewModel.uiState.value?.sheet?.naming).isEqualTo(GardenNaming(name = "Jardin joyeux 2"))
    }

    @Test
    fun `a name another garden has, the case ignored, or a blank one, is refused`() = runTest {
        // Given the window of the name open
        library.keep("Pluie douce", A_GARDEN)
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When / Then
        viewModel.changeName("pluie DOUCE")
        assertThat(viewModel.uiState.value?.sheet?.naming?.problem).isEqualTo(NameProblem.TAKEN)
        viewModel.changeName("   ")
        assertThat(viewModel.uiState.value?.sheet?.naming?.problem).isEqualTo(NameProblem.BLANK)
        viewModel.changeName("Orage")
        assertThat(viewModel.uiState.value?.sheet?.naming?.problem).isNull()
    }

    @Test
    fun `a name typed is cut at 40 characters`() {
        // Given the window of the name open
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When
        viewModel.changeName("a".repeat(45))

        // Then
        assertThat(viewModel.uiState.value?.sheet?.naming?.name).isEqualTo("a".repeat(40))
    }

    @Test
    fun `Annuler closes the window of the name, the sheet still open`() {
        // Given the window of the name open
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When
        viewModel.cancelNaming()

        // Then
        assertThat(viewModel.uiState.value?.sheet).isEqualTo(GardensSheet())
    }

    @Test
    fun `a garden kept is the one the garden under way comes from, no longer changed, and its name is told`() =
        runTest {
            // Given the window of the name open on a garden
            kept.state.value = A_GARDEN
            viewModel.openMyGardens()
            viewModel.startKeeping(moodName = "Joyeux")
            viewModel.changeName("  Pluie douce ")

            // When
            viewModel.confirmKeeping(messageMillis = 4_000)

            // Then it is kept under its name, the garden under way coming from it, in its place on the device too
            val pluie = library.gardens.first().single()
            assertThat(pluie.name).isEqualTo("Pluie douce")
            assertThat(pluie.garden).isEqualTo(A_GARDEN)
            val state = viewModel.uiState.value!!
            assertThat(state.keptFrom).isEqualTo(pluie.id)
            assertThat(state.modified).isFalse()
            assertThat(kept.currentState.value).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = pluie.id))
            // The window and the sheet closed, the name told
            assertThat(state.sheet).isNull()
            assertThat(state.keptName).isEqualTo("Pluie douce")
        }

    @Test
    fun `the name of a garden kept is told for a while`() {
        // Given a garden kept
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")
        viewModel.confirmKeeping(messageMillis = 4_000)

        // When / Then it shows until the time of the message is over
        mainDispatcher.scheduler.advanceTimeBy(3_999)
        assertThat(viewModel.uiState.value?.keptName).isEqualTo("Jardin joyeux")
        mainDispatcher.scheduler.advanceTimeBy(2)
        assertThat(viewModel.uiState.value?.keptName).isNull()
    }

    @Test
    fun `a garden that cannot be kept leaves the window open, and it tells it`() {
        // Given the window of the name open, on a disk that cannot be written
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")
        library.failure = IOException("A full disk")

        // When
        viewModel.confirmKeeping(messageMillis = 4_000)

        // Then
        val state = viewModel.uiState.value!!
        assertThat(state.sheet?.naming).isEqualTo(GardenNaming(name = "Jardin joyeux", failed = true))
        assertThat(state.keptFrom).isNull()
        assertThat(state.keptName).isNull()
    }

    @Test
    fun `a name refused is not kept`() = runTest {
        // Given a name taken typed
        library.keep("Pluie douce", A_GARDEN)
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")
        viewModel.changeName("Pluie douce")

        // When
        viewModel.confirmKeeping(messageMillis = 4_000)

        // Then
        assertThat(library.gardens.first()).hasSize(1)
        assertThat(viewModel.uiState.value?.sheet?.naming?.name).isEqualTo("Pluie douce")
    }

    @Test
    fun `Enregistrer « X » is offered only once the garden under way changed since it was kept`() = runTest {
        // Given the garden under way coming from a kept garden, unchanged
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        assertThat(viewModel.uiState.value?.canSaveInPlace).isFalse()

        // When it changes
        viewModel.chooseMood(GardenMood.EPIC)

        // Then
        assertThat(viewModel.uiState.value?.canSaveInPlace).isTrue()
    }

    @Test
    fun `Enregistrer « X » replaces X with the garden under way, and tells it`() = runTest {
        // Given the garden under way changed since it was kept as X, beside another garden
        val pluie = library.keep("Pluie douce", A_GARDEN)
        library.keep("Orage", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()

        // When
        viewModel.saveInPlace(messageMillis = 4_000)

        // Then X holds the garden under way, under its name and identity, and nothing else is kept
        val gardens = library.gardens.first()
        assertThat(gardens.map { it.name }).containsExactly("Pluie douce", "Orage")
        assertThat(gardens.first().garden).isEqualTo(A_GARDEN.withMood(GardenMood.EPIC))
        val state = viewModel.uiState.value!!
        assertThat(state.keptFrom).isEqualTo(pluie.id)
        assertThat(state.modified).isFalse()
        assertThat(state.sheet).isNull()
        assertThat(state.keptName).isEqualTo("Pluie douce")
    }

    @Test
    fun `each garden kept is a keeping of its own, told again though its name is the same`() = runTest {
        // Given the garden under way kept as « Pluie douce »
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()
        viewModel.saveInPlace(messageMillis = 4_000)
        val first = viewModel.uiState.value!!.keeps

        // When it changes and is saved in its place again, while its name still shows
        viewModel.chooseMood(GardenMood.CALM)
        viewModel.openMyGardens()
        viewModel.saveInPlace(messageMillis = 4_000)

        // Then the same name, told by another keeping
        val state = viewModel.uiState.value!!
        assertThat(state.keptName).isEqualTo("Pluie douce")
        assertThat(state.keeps).isEqualTo(first + 1)
    }

    @Test
    fun `a full library still replaces X`() = runTest {
        // Given a full library, the garden under way changed since it was kept as the first one
        val gardens = List(GardenLibrary.MAX_GARDENS) { library.keep("Jardin ${it + 1}", A_GARDEN) }
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = gardens.first().id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()

        // When
        viewModel.saveInPlace(messageMillis = 4_000)

        // Then
        assertThat(library.gardens.first().first().garden.mood).isEqualTo(GardenMood.EPIC)
        assertThat(viewModel.uiState.value?.modified).isFalse()
    }

    @Test
    fun `a garden that cannot be saved in place leaves the sheet open, and it tells it`() = runTest {
        // Given the garden under way changed since it was kept, on a disk that cannot be written
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()
        library.failure = IOException("A full disk")

        // When
        viewModel.saveInPlace(messageMillis = 4_000)

        // Then
        assertThat(viewModel.uiState.value?.sheet).isEqualTo(GardensSheet(failed = true))
        assertThat(viewModel.uiState.value?.modified).isTrue()
    }

    @Test
    fun `Garder une copie keeps another garden, which the garden under way then comes from`() = runTest {
        // Given the garden under way changed since it was kept as X
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()

        // When
        viewModel.startKeeping(moodName = "Épique")
        viewModel.confirmKeeping(messageMillis = 4_000)

        // Then X is as it was, the copy holds the garden under way, which comes from it
        val (kept, copy) = library.gardens.first()
        assertThat(kept.garden).isEqualTo(A_GARDEN)
        assertThat(copy.name).isEqualTo("Jardin épique")
        assertThat(copy.garden).isEqualTo(A_GARDEN.withMood(GardenMood.EPIC))
        assertThat(viewModel.uiState.value?.keptFrom).isEqualTo(copy.id)
    }

    @Test
    fun `a garden whose kept garden is no longer listed is offered to be kept as a new one`() = runTest {
        // Given the garden under way coming from a kept garden deleted since
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN.withMood(GardenMood.EPIC), keptFrom = pluie.id)
        library.delete(pluie.id)

        // When / Then
        val state = viewModel.uiState.value!!
        assertThat(state.linked).isNull()
        assertThat(state.modified).isFalse()
        assertThat(state.canSaveInPlace).isFalse()
        assertThat(state.unchanged).isFalse()
        assertThat(state.keepRefusal).isNull()
    }

    @Test
    fun `a garden kept while the screen is left is still kept as the one the garden under way comes from`() {
        // Given a garden being kept on a slow disk
        val slow = GatedGardenLibrary()
        kept.state.value = A_GARDEN
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, factory(slow))[GardenViewModel::class]
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")
        viewModel.confirmKeeping(messageMillis = 4_000)

        // When the screen is left before it is written
        store.clear()
        slow.gate.complete(Unit)

        // Then the garden under way comes from it all the same
        val pluie = runBlocking { slow.gardens.first() }.single()
        assertThat(kept.currentState.value).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = pluie.id))
    }

    @Test
    fun `a garden kept whose link cannot be written leaves the sheet open and tells it, not kept as linked`() =
        runTest {
            // Given the window of the name open, the garden under way that cannot be written
            kept.state.value = A_GARDEN
            viewModel.openMyGardens()
            viewModel.startKeeping(moodName = "Joyeux")
            kept.failure = IOException("A full disk")

            // When
            viewModel.confirmKeeping(messageMillis = 4_000)

            // Then the kept garden is listed, but the garden under way does not come from it, and the sheet says so
            assertThat(library.gardens.first().map { it.name }).containsExactly("Jardin joyeux")
            val state = viewModel.uiState.value!!
            assertThat(state.keptFrom).isNull()
            assertThat(state.keptName).isNull()
            assertThat(state.sheet).isEqualTo(GardensSheet(failed = true))
        }

    @Test
    fun `Enregistrer « X » writes no link, the garden under way comes from X already`() = runTest {
        // Given the garden under way changed since it was kept as X, its link not to be written again
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)
        viewModel.chooseMood(GardenMood.EPIC)
        viewModel.openMyGardens()
        kept.failure = IOException("Not to be written")

        // When
        viewModel.saveInPlace(messageMillis = 4_000)

        // Then X is replaced and told, nothing failed
        assertThat(library.gardens.first().single().garden.mood).isEqualTo(GardenMood.EPIC)
        assertThat(viewModel.uiState.value?.keptName).isEqualTo("Pluie douce")
        assertThat(viewModel.uiState.value?.sheet).isNull()
    }

    @Test
    fun `a garden as it was kept is told unchanged, Enregistrer « X » greyed`() = runTest {
        // Given the garden under way as it was kept
        val pluie = library.keep("Pluie douce", A_GARDEN)
        kept.currentState.value = CurrentGarden(A_GARDEN, keptFrom = pluie.id)

        // When / Then
        assertThat(viewModel.uiState.value?.unchanged).isTrue()
        viewModel.chooseMood(GardenMood.EPIC)
        assertThat(viewModel.uiState.value?.unchanged).isFalse()
    }

    @Test
    fun `a name typed again after a failure no longer tells the failure`() {
        // Given the window of the name telling a failure
        kept.state.value = A_GARDEN
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")
        library.failure = IOException("A full disk")
        viewModel.confirmKeeping(messageMillis = 4_000)

        // When
        viewModel.changeName("Orage")

        // Then
        assertThat(viewModel.uiState.value?.sheet?.naming).isEqualTo(GardenNaming(name = "Orage"))
    }

    @Test
    fun `while a garden is being kept the sheet is busy, and a second Garder keeps nothing more`() {
        // Given a garden being kept on a slow disk
        val slow = GatedGardenLibrary()
        kept.state.value = A_GARDEN
        val viewModel = gardenViewModel(kept, library = slow)
        viewModel.openMyGardens()
        viewModel.startKeeping(moodName = "Joyeux")

        // When Garder is touched twice before it is written
        viewModel.confirmKeeping(messageMillis = 4_000)
        val busy = viewModel.uiState.value?.sheet?.writing
        viewModel.confirmKeeping(messageMillis = 4_000)
        slow.gate.complete(Unit)

        // Then the sheet was busy meanwhile, and a single garden is kept
        assertThat(busy).isTrue()
        assertThat(slow.keeps).isEqualTo(1)
    }

    @Test
    fun `a garden adopted while the garden kept is read keeps the kept garden it comes from, and the list`() = runTest {
        // Given a garden kept from « Pluie douce », slow to be read
        val pluie = library.keep("Pluie douce", A_GARDEN)
        val read = CompletableDeferred<CurrentGarden>()
        val slow = object : GardenRepository by kept {
            override val current: Flow<CurrentGarden> = flow { emit(read.await()) }
        }
        val viewModel = gardenViewModel(slow, library = library)

        // When a garden is adopted before it is read
        viewModel.adopt(A_GARDEN.withMood(GardenMood.EPIC))
        read.complete(CurrentGarden(A_GARDEN, keptFrom = pluie.id))

        // Then the garden adopted is shown, from « Pluie douce », changed since, the list known
        val state = viewModel.uiState.value!!
        assertThat(state.garden.mood).isEqualTo(GardenMood.EPIC)
        assertThat(state.keptFrom).isEqualTo(pluie.id)
        assertThat(state.myGardens).isEqualTo(MyGardens.Listed(listOf(pluie)))
        assertThat(state.modified).isTrue()
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

    private companion object {
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
