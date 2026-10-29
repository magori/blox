package ch.magori.blox.core.data

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The contract of [PadlockProgressRepository], what every implementation does, the real one and the fake shared by the
 * tests of the features alike. A subclass gives an empty repository; what is proper to an implementation (a file, its
 * format, its errors) is tested in the subclass.
 */
abstract class PadlockProgressRepositoryContract {
    /** A repository without any opening, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): PadlockProgressRepository

    @Test
    fun `without an opening no padlock is open and the frontier is padlock 1`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        val progress = repository.progress.first()

        // Then
        assertThat(progress.openings).isEmpty()
        assertThat(progress.frontier).isEqualTo(1)
    }

    @Test
    fun `opening the frontier is a first opening, kept after the others, and the frontier moves on`() = runTest {
        // Given padlock 1 open
        val repository = emptyRepository(backgroundScope)
        repository.record(1, FIRST)

        // When padlock 2 opens
        val outcome = repository.record(2, SECOND)

        // Then
        val progress = repository.progress.first()
        assertThat(outcome).isEqualTo(OpeningOutcome.FIRST_OPENING)
        assertThat(progress.openings).containsExactly(FIRST, SECOND)
        assertThat(progress.frontier).isEqualTo(3)
    }

    @Test
    fun `a better opening of an open padlock is a new best, and takes its place`() = runTest {
        // Given padlocks 1 and 2 open
        val repository = emptyRepository(backgroundScope)
        repository.record(1, FIRST)
        repository.record(2, SECOND)

        // When padlock 1 opens again without its refusal
        val perfect = FIRST.copy(refusals = 0)
        val outcome = repository.record(1, perfect)

        // Then
        val progress = repository.progress.first()
        assertThat(outcome).isEqualTo(OpeningOutcome.NEW_BEST)
        assertThat(progress.openings).containsExactly(perfect, SECOND)
        assertThat(progress.frontier).isEqualTo(3)
    }

    @Test
    fun `an opening no better than the best of its padlock is nothing new, and the best is kept`() = runTest {
        // Given padlock 1 open
        val repository = emptyRepository(backgroundScope)
        repository.record(1, FIRST)

        // When it opens again with a hint, though without refusal
        val outcome = repository.record(1, FIRST.copy(refusals = 0, hints = 1))

        // Then
        assertThat(outcome).isEqualTo(OpeningOutcome.NOTHING_NEW)
        assertThat(repository.progress.first().openings).containsExactly(FIRST)
    }

    @ParameterizedTest(name = "padlock {0}")
    @ValueSource(ints = [0, 3, Int.MAX_VALUE])
    fun `a padlock beyond the frontier or before padlock 1 is a programming error, and nothing is kept`(padlock: Int) =
        runTest {
            // Given padlock 1 open: the frontier is padlock 2
            val repository = emptyRepository(backgroundScope)
            repository.record(1, FIRST)

            // When
            val failure = runCatching { repository.record(padlock, SECOND) }.exceptionOrNull()

            // Then the padlocks open in order
            assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(repository.progress.first().openings).containsExactly(FIRST)
        }

    @Test
    fun `an opening of another number of pieces than the best of its padlock is a programming error`() = runTest {
        // Given padlock 1 open, of 3 pieces
        val repository = emptyRepository(backgroundScope)
        repository.record(1, FIRST)

        // When it opens again with 4 pieces, without refusal: not the same padlock
        val failure = runCatching { repository.record(1, SECOND.copy(moves = 4, hints = 0)) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repository.progress.first().openings).containsExactly(FIRST)
    }

    @Test
    fun `clearing the progress forgets every opening, and the frontier is padlock 1 again`() = runTest {
        // Given padlocks 1 and 2 open
        val repository = emptyRepository(backgroundScope)
        repository.record(1, FIRST)
        repository.record(2, SECOND)

        // When
        repository.clear()

        // Then
        val progress = repository.progress.first()
        assertThat(progress.openings).isEmpty()
        assertThat(progress.frontier).isEqualTo(1)
    }

    @Test
    fun `the progress flows each change to whoever watches it`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.progress.test {
            assertThat(awaitItem()).isEqualTo(PadlockProgress())

            // When / Then
            repository.record(1, FIRST)
            assertThat(awaitItem()).isEqualTo(PadlockProgress(listOf(FIRST)))
            repository.clear()
            assertThat(awaitItem()).isEqualTo(PadlockProgress())
        }
    }

    private companion object {
        /** An opening of padlock 1, of 3 pieces, with a refusal. */
        val FIRST = PadlockOpening(pieces = 3, moves = 3, refusals = 1, hints = 0)

        /** An opening of padlock 2, of 4 pieces, with a piece taken back and a hint. */
        val SECOND = PadlockOpening(pieces = 4, moves = 5, refusals = 0, hints = 1)
    }
}
