package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/** The fake shared by the tests of the features keeps the contract of the real progress of the padlocks. */
class FakePadlockProgressRepositoryTest : PadlockProgressRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): PadlockProgressRepository = FakePadlockProgressRepository()

    @Test
    fun `given a failure, each change fails with it as a file that cannot be written, and the progress is unchanged`() =
        runTest {
            // Given padlock 1 open, then a disk full
            val repository = FakePadlockProgressRepository()
            val opening = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
            repository.record(1, opening)
            repository.failure = IOException("disk full")

            // When
            val failures = listOf<suspend () -> Unit>(
                { repository.record(2, opening) },
                { repository.clear() },
            ).map { change -> runCatching { change() }.exceptionOrNull() }

            // Then
            assertThat(failures).allSatisfy { assertThat(it).isInstanceOf(IOException::class.java) }
            assertThat(repository.progress.first().openings).containsExactly(opening)
        }
}
