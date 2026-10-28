package ch.magori.blox.core.data

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant

/**
 * The contract of [GardenLibrary], what every implementation does, the real one and the fake shared by the tests of
 * the features alike. A subclass gives an empty library; what is proper to an implementation (a file, its format, its
 * errors) is tested in the subclass.
 */
abstract class GardenLibraryContract {
    /**
     * A library without any kept garden, whatever it needs to live in [scope] (a DataStore does), that dates the
     * changes by [clock].
     */
    protected abstract fun emptyLibrary(
        scope: CoroutineScope,
        clock: Clock,
    ): GardenLibrary

    /** A library without any kept garden, its clock stopped at [MODIFIED_AT]. */
    protected fun emptyLibrary(scope: CoroutineScope): GardenLibrary = emptyLibrary(scope, MutableClock())

    @Test
    fun `without a kept garden the list is empty`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)

        // When / Then
        assertThat(library.gardens.first()).isEmpty()
    }

    @Test
    fun `a kept garden goes after the others, with its name, its pieces, its mood and the time it was kept`() =
        runTest {
            // Given a library whose clock tells ten past ten, a garden kept
            val clock = MutableClock(Instant.parse("2026-10-20T10:10:00Z"))
            val library = emptyLibrary(backgroundScope, clock)
            val first = library.keep("Jardin calme", A_GARDEN)

            // When another one is kept
            val second = library.keep("Pluie douce", ANOTHER_GARDEN)

            // Then
            assertThat(library.gardens.first()).containsExactly(first, second)
            assertThat(second.name).isEqualTo("Pluie douce")
            assertThat(second.garden).isEqualTo(ANOTHER_GARDEN)
            assertThat(second.modifiedAt).isEqualTo(Instant.parse("2026-10-20T10:10:00Z"))
        }

    @Test
    fun `the time of a change is kept to the millisecond, as the gardens file keeps it`() = runTest {
        // Given a clock that tells the time to the nanosecond
        val clock = MutableClock(Instant.parse("2026-10-20T10:00:00.123456789Z"))
        val library = emptyLibrary(backgroundScope, clock)

        // When
        val kept = library.keep("Jardin calme", A_GARDEN)

        // Then the garden read now is the one read after the file is opened again
        assertThat(kept.modifiedAt).isEqualTo(Instant.parse("2026-10-20T10:00:00.123Z"))
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a garden whose name another one has, the case ignored, is refused and nothing is kept`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Pluie douce", A_GARDEN)

        // When
        val failure = runCatching { library.keep("PLUIE DOUCE", ANOTHER_GARDEN) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a library holds 30 gardens at the most, a 31st is refused and the 30 are kept`() = runTest {
        // Given a full library
        val library = emptyLibrary(backgroundScope)
        val full = fill(library)

        // When
        val failure = runCatching { library.keep("Jardin 31", ANOTHER_GARDEN) }.exceptionOrNull()

        // Then
        assertThat(GardenLibrary.MAX_GARDENS).isEqualTo(30)
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).isEqualTo(full)
    }

    @Test
    fun `a replaced garden takes the new pieces and the time of the change, and keeps its identity, name and place`() =
        runTest {
            // Given two gardens kept at ten
            val clock = MutableClock()
            val library = emptyLibrary(backgroundScope, clock)
            val first = library.keep("Jardin calme", A_GARDEN)
            val second = library.keep("Pluie douce", A_GARDEN)

            // When the first one is replaced the next day: « Enregistrer « Jardin calme » »
            clock.now = Instant.parse("2026-10-21T08:00:00Z")
            val replaced = library.replace(first.id, ANOTHER_GARDEN)

            // Then
            val expected = first.copy(garden = ANOTHER_GARDEN, modifiedAt = Instant.parse("2026-10-21T08:00:00Z"))
            assertThat(replaced).isEqualTo(expected)
            assertThat(library.gardens.first()).containsExactly(expected, second)
        }

    @Test
    fun `a garden of a full library may still be replaced`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val full = fill(library)

        // When
        library.replace(full.first().id, ANOTHER_GARDEN)

        // Then
        assertThat(library.gardens.first()).hasSize(GardenLibrary.MAX_GARDENS)
        assertThat(library.gardens.first().first().garden).isEqualTo(ANOTHER_GARDEN)
    }

    @Test
    fun `replacing a garden that is not kept is refused`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)

        // When
        val failure = runCatching { library.replace(GardenId("unknown"), ANOTHER_GARDEN) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a renamed garden keeps its place and its pieces, and takes the time of its renaming`() = runTest {
        // Given two gardens kept at ten
        val clock = MutableClock()
        val library = emptyLibrary(backgroundScope, clock)
        val first = library.keep("Jardin calme", A_GARDEN)
        val second = library.keep("Pluie douce", ANOTHER_GARDEN)

        // When the first one is renamed the next day
        clock.now = Instant.parse("2026-10-21T08:00:00Z")
        library.rename(first.id, "Berceuse")

        // Then
        assertThat(library.gardens.first()).containsExactly(
            first.copy(name = "Berceuse", modifiedAt = Instant.parse("2026-10-21T08:00:00Z")),
            second,
        )
    }

    @Test
    fun `a garden cannot be renamed with the name of another one, the case ignored`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val first = library.keep("Jardin calme", A_GARDEN)
        val second = library.keep("Pluie douce", ANOTHER_GARDEN)

        // When
        val failure = runCatching { library.rename(first.id, "pluie douce") }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).containsExactly(first, second)
    }

    @Test
    fun `a garden renamed with its own name, as it is, keeps the time of its last change`() = runTest {
        // Given a garden kept at ten
        val clock = MutableClock()
        val library = emptyLibrary(backgroundScope, clock)
        val kept = library.keep("Jardin calme", A_GARDEN)

        // When it is « renamed » the next day with the very same name: nothing changes
        clock.now = Instant.parse("2026-10-21T08:00:00Z")
        library.rename(kept.id, "Jardin calme")

        // Then
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a garden may be renamed with its own name in another case`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)

        // When
        library.rename(kept.id, "JARDIN CALME")

        // Then
        assertThat(library.gardens.first().map { it.name }).containsExactly("JARDIN CALME")
    }

    @Test
    fun `renaming a garden that is not kept is refused`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)

        // When
        val failure = runCatching { library.rename(GardenId("unknown"), "Berceuse") }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a deleted garden leaves the list, and what is deleted tells its place`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val first = library.keep("Jardin calme", A_GARDEN)
        val second = library.keep("Pluie douce", A_GARDEN)
        val third = library.keep("Orage", ANOTHER_GARDEN)

        // When
        val deleted = library.delete(second.id)

        // Then
        assertThat(deleted).isEqualTo(DeletedGarden(second, place = 1))
        assertThat(library.gardens.first()).containsExactly(first, third)
    }

    @Test
    fun `deleting a garden that is not kept gives nothing back and changes nothing`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)

        // When
        val deleted = library.delete(GardenId("unknown"))

        // Then
        assertThat(deleted).isNull()
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a deleted garden restored comes back at its place, as it was kept`() = runTest {
        // Given three gardens kept at ten, the second one deleted
        val clock = MutableClock()
        val library = emptyLibrary(backgroundScope, clock)
        val first = library.keep("Jardin calme", A_GARDEN)
        val second = library.keep("Pluie douce", A_GARDEN)
        val third = library.keep("Orage", ANOTHER_GARDEN)
        val deleted = checkNotNull(library.delete(second.id))

        // When it is restored at eleven: « Annuler » undoes the deletion, it changes nothing of the garden
        clock.now = Instant.parse("2026-10-20T11:00:00Z")
        library.restore(deleted)

        // Then
        assertThat(library.gardens.first()).containsExactly(first, second, third)
    }

    @Test
    fun `a garden restored after the gardens before it were deleted comes back at the end of the list`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val first = library.keep("Jardin calme", A_GARDEN)
        val second = library.keep("Pluie douce", A_GARDEN)
        val deleted = checkNotNull(library.delete(second.id))
        library.delete(first.id)

        // When
        library.restore(deleted)

        // Then
        assertThat(library.gardens.first()).containsExactly(second)
    }

    @Test
    fun `a garden restored twice is in the list once`() = runTest {
        // Given
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)
        val deleted = checkNotNull(library.delete(kept.id))
        library.restore(deleted)

        // When
        val restored = library.restore(deleted)

        // Then
        assertThat(restored).isEqualTo(kept)
        assertThat(library.gardens.first()).containsExactly(kept)
    }

    @Test
    fun `a garden whose name was taken since its deletion is restored under a free name`() = runTest {
        // Given a garden deleted, then another one kept under its name, the case aside
        val library = emptyLibrary(backgroundScope)
        val kept = library.keep("Jardin calme", A_GARDEN)
        val deleted = checkNotNull(library.delete(kept.id))
        val other = library.keep("jardin calme", ANOTHER_GARDEN)

        // When
        val restored = library.restore(deleted)

        // Then it comes back numbered, the first number that makes its name free
        assertThat(restored).isEqualTo(kept.copy(name = "Jardin calme 2"))
        assertThat(library.gardens.first()).containsExactly(restored, other)
    }

    @Test
    fun `a garden of a name of 40 characters, taken since, is restored under a free name cut to fit`() = runTest {
        // Given a garden of a name of 40 characters deleted, then another one kept under its name
        val library = emptyLibrary(backgroundScope)
        val longName = "x".repeat(KeptGarden.MAX_NAME_LENGTH)
        val deleted = checkNotNull(library.delete(library.keep(longName, A_GARDEN).id))
        library.keep(longName, ANOTHER_GARDEN)

        // When
        val restored = library.restore(deleted)

        // Then
        assertThat(restored.name).isEqualTo("x".repeat(KeptGarden.MAX_NAME_LENGTH - 2) + " 2")
    }

    @Test
    fun `a garden of a name of 40 characters ending with a number, taken since, is restored cut and numbered 2`() =
        runTest {
            // Given a garden of a name of 40 characters ending with « 99 » deleted, then another one kept under its
            // name: counting on, « 100 », would not fit
            val library = emptyLibrary(backgroundScope)
            val longName = "x".repeat(KeptGarden.MAX_NAME_LENGTH - 3) + " 99"
            val deleted = checkNotNull(library.delete(library.keep(longName, A_GARDEN).id))
            library.keep(longName, ANOTHER_GARDEN)

            // When
            val restored = library.restore(deleted)

            // Then its name is cut so that a number fits, and that number starts again from 2
            assertThat(restored.name).isEqualTo("x".repeat(KeptGarden.MAX_NAME_LENGTH - 3) + " 2")
        }

    @Test
    fun `a deleted garden cannot be restored once the library is full again`() = runTest {
        // Given a garden deleted, then the library filled again
        val library = emptyLibrary(backgroundScope)
        val deleted = checkNotNull(library.delete(library.keep("Pluie douce", A_GARDEN).id))
        val full = fill(library)

        // When
        val failure = runCatching { library.restore(deleted) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(library.gardens.first()).isEqualTo(full)
    }

    @Test
    fun `the list emits each change once, and nothing for a change that changes nothing`() = runTest {
        // Given
        val clock = MutableClock()
        val library = emptyLibrary(backgroundScope, clock)

        library.gardens.test {
            assertThat(awaitItem()).isEmpty()

            // When a garden is kept, replaced, then renamed, each change an hour later
            val kept = library.keep("Jardin calme", A_GARDEN)
            clock.now = Instant.parse("2026-10-20T11:00:00Z")
            val replaced = library.replace(kept.id, ANOTHER_GARDEN)
            clock.now = Instant.parse("2026-10-20T12:00:00Z")
            library.rename(kept.id, "Orage")

            // Then each change is emitted once
            assertThat(awaitItem()).containsExactly(kept)
            assertThat(awaitItem()).containsExactly(replaced)
            val renamed = replaced.copy(name = "Orage", modifiedAt = Instant.parse("2026-10-20T12:00:00Z"))
            assertThat(awaitItem()).containsExactly(renamed)

            // When it is « renamed » with its very same name, then a garden not kept is deleted
            library.rename(kept.id, "Orage")
            library.delete(GardenId("unknown"))
            testScheduler.advanceUntilIdle()

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `a garden restored with a time to the nanosecond is kept to the millisecond, as the gardens file keeps it`() =
        runTest {
            // Given a deleted garden made up by hand, dated to the nanosecond
            val library = emptyLibrary(backgroundScope)
            val time = Instant.parse("2026-10-20T10:00:00.123456789Z")
            val deleted = DeletedGarden(KeptGarden(GardenId("a"), "Orage", A_GARDEN, time), place = 0)

            // When
            val restored = library.restore(deleted)

            // Then the garden read now is the one read after the file is opened again
            assertThat(restored.modifiedAt).isEqualTo(Instant.parse("2026-10-20T10:00:00.123Z"))
            assertThat(library.gardens.first()).containsExactly(restored)
        }

    @Test
    fun `each kept garden has an identity of its own, a deleted one's included`() = runTest {
        // Given a garden kept then deleted
        val library = emptyLibrary(backgroundScope)
        val deleted = library.keep("Jardin calme", A_GARDEN)
        library.delete(deleted.id)

        // When
        val ids = List(3) { library.keep("Jardin $it", A_GARDEN).id }

        // Then
        assertThat(ids + deleted.id).doesNotHaveDuplicates()
    }

    /** Keeps gardens in [library] until it holds [GardenLibrary.MAX_GARDENS], « Jardin 1 » first; gives them. */
    private suspend fun fill(library: GardenLibrary): List<KeptGarden> {
        val kept = library.gardens.first().size
        repeat(GardenLibrary.MAX_GARDENS - kept) { library.keep("Jardin ${kept + it + 1}", A_GARDEN) }
        return library.gardens.first()
    }
}
