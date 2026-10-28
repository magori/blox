package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock

/** The fake shared by the tests of the features keeps the contract of the real kept gardens. */
class FakeGardenLibraryTest : GardenLibraryContract() {
    override fun emptyLibrary(
        scope: CoroutineScope,
        clock: Clock,
    ): GardenLibrary = FakeGardenLibrary(clock)

    @Test
    fun `given a failure, each change fails with it as a file that cannot be written, and the gardens are unchanged`() =
        runTest {
            // Given a kept garden, then a disk full
            val library = FakeGardenLibrary()
            val kept = library.keep("Jardin calme", A_GARDEN)
            val deleted = DeletedGarden(kept.copy(id = GardenId("b"), name = "Orage"), place = 1)
            library.failure = IOException("disk full")

            // When
            val failures = listOf<suspend () -> Unit>(
                { library.keep("Pluie douce", ANOTHER_GARDEN) },
                { library.replace(kept.id, ANOTHER_GARDEN) },
                { library.rename(kept.id, "Berceuse") },
                { library.delete(kept.id) },
                { library.restore(deleted) },
            ).map { change -> runCatching { change() }.exceptionOrNull() }

            // Then
            assertThat(failures).allSatisfy { assertThat(it).isInstanceOf(IOException::class.java) }
            assertThat(library.gardens.first()).containsExactly(kept)
        }
}
