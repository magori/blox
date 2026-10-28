package ch.magori.blox.core.data

import app.cash.turbine.test
import ch.magori.blox.core.garden.Garden
import ch.magori.blox.core.garden.GardenMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * The contract of [GardenRepository], what every implementation does, the real one and the fake shared by the tests of
 * the features alike. A subclass gives an empty repository; what is proper to an implementation (a file, its format,
 * its errors) is tested in the subclass.
 */
abstract class GardenRepositoryContract {
    /** A repository without any garden kept, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): GardenRepository

    /**
     * A repository holding [A_GARDEN], as a full disk: the garden is read as kept, but every write fails with an
     * IOException.
     */
    protected abstract suspend fun unwritableRepository(scope: CoroutineScope): GardenRepository

    @Test
    fun `without a garden kept the garden is empty and calm`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val garden = repository.garden.first()

        // Then
        assertThat(garden).isEqualTo(Garden.EMPTY)
        assertThat(garden.pieces).isEmpty()
        assertThat(garden.mood).isEqualTo(GardenMood.CALM)
    }

    @Test
    fun `without a garden kept the garden under way comes from no kept garden`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val current = repository.current.first()

        // Then
        assertThat(current).isEqualTo(CurrentGarden(Garden.EMPTY, keptFrom = null))
    }

    @Test
    fun `a garden kept with the kept garden it comes from is read with it`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.keep(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))

        // Then
        assertThat(repository.current.first()).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))
        assertThat(repository.garden.first()).isEqualTo(A_GARDEN)
    }

    @Test
    fun `a change of the garden kept leaves it coming from the same kept garden`() = runTest {
        // Given a garden that comes from a kept garden
        val repository = emptyRepository(backgroundScope)
        repository.keep(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))

        // When a change of it is kept, as at each gesture
        repository.keep(ANOTHER_GARDEN)

        // Then
        assertThat(repository.current.first()).isEqualTo(CurrentGarden(ANOTHER_GARDEN, keptFrom = A_KEPT_GARDEN))
    }

    @Test
    fun `a garden kept as coming from no kept garden is no longer linked`() = runTest {
        // Given a garden that comes from a kept garden
        val repository = emptyRepository(backgroundScope)
        repository.keep(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))

        // When it is kept as coming from none, as when its kept garden is deleted
        repository.keep(CurrentGarden(A_GARDEN, keptFrom = null))

        // Then
        assertThat(repository.current.first()).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = null))
    }

    @Test
    fun `the garden under way read follows each garden kept with its link, in a single step`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.current.test {
            assertThat(awaitItem()).isEqualTo(CurrentGarden.EMPTY)

            // When / Then neither the garden without its link, nor the link without its garden
            repository.keep(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))
            assertThat(awaitItem()).isEqualTo(CurrentGarden(A_GARDEN, keptFrom = A_KEPT_GARDEN))
            repository.keep(CurrentGarden(ANOTHER_GARDEN, keptFrom = null))
            assertThat(awaitItem()).isEqualTo(CurrentGarden(ANOTHER_GARDEN, keptFrom = null))
        }
    }

    @Test
    fun `the last garden kept is the one read, in place of the one before`() = runTest {
        // Given a garden kept, then another one in another mood
        val repository = emptyRepository(backgroundScope)
        repository.keep(A_GARDEN)
        repository.keep(ANOTHER_GARDEN)

        // When
        val garden = repository.garden.first()

        // Then
        assertThat(garden).isEqualTo(ANOTHER_GARDEN)
    }

    @Test
    fun `a garden that cannot be kept fails with an IOException, and the garden kept before is read`() = runTest {
        // Given a repository holding a garden, as a full disk
        val repository = unwritableRepository(backgroundScope)

        // When
        val failure = runCatching { repository.keep(ANOTHER_GARDEN) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(repository.garden.first()).isEqualTo(A_GARDEN)
    }

    @Test
    fun `the garden read follows each garden kept`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.garden.test {
            assertThat(awaitItem()).isEqualTo(Garden.EMPTY)

            // When / Then
            repository.keep(A_GARDEN)
            assertThat(awaitItem()).isEqualTo(A_GARDEN)
            repository.keep(ANOTHER_GARDEN)
            assertThat(awaitItem()).isEqualTo(ANOTHER_GARDEN)
        }
    }
}

/** Two pieces, an I lying on the top row and a T on the bottom one, heard in the joyful mood. */
internal val A_GARDEN: Garden = Garden.parse(
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
    .............TTT
    ..............T.
    """,
).withMood(GardenMood.JOYFUL)

/** A single O, in the middle, heard in the epic mood. */
internal val ANOTHER_GARDEN: Garden = Garden.parse(
    """
    ................
    ................
    ................
    ................
    ................
    ................
    .......OO.......
    .......OO.......
    ................
    ................
    ................
    ................
    ................
    ................
    """,
).withMood(GardenMood.EPIC)

/** The identity of a garden kept in « Mes jardins », the one the garden under way comes from. */
internal val A_KEPT_GARDEN: GardenId = GardenId("8c1f5a3e-2b7d-4e90-a6c4-1d2e3f405162")
