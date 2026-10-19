package ch.magori.blox.core.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * The fake shared by the tests of the features keeps the contract of the real records; its own settings
 * (a failure, a gate) are tested here.
 */
class FakeHighScoreRepositoryTest : HighScoreRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): HighScoreRepository = FakeHighScoreRepository()

    @Test
    fun `a failing submit throws and leaves the record and saved unchanged`() = runTest {
        // Given a record of 1200, then records that cannot be written
        val repository = FakeHighScoreRepository()
        repository.submit(1200)
        repository.failure = IOException("disk full")

        // When
        val failure = runCatching { repository.submit(5000) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IOException::class.java).hasMessage("disk full")
        assertThat(repository.record.value).isEqualTo(1200)
        assertThat(repository.saved).containsExactly(1200)
        assertThat(repository.submitted).containsExactly(1200, 5000)
    }

    @Test
    fun `a submit waits for the gate before it saves`() = runTest {
        // Given records that take their time to write
        val repository = FakeHighScoreRepository()
        val writing = CompletableDeferred<Unit>()
        repository.gate = writing

        // When a score is submitted
        val result = async { repository.submit(1200) }
        runCurrent()

        // Then it is submitted, but neither saved nor the record yet
        assertThat(repository.submitted).containsExactly(1200)
        assertThat(repository.saved).isEmpty()
        assertThat(repository.record.value).isEqualTo(0)

        // When the records answer
        writing.complete(Unit)

        // Then
        assertThat(result.await()).isEqualTo(RecordResult(previous = 0, isNewRecord = true))
        assertThat(repository.saved).containsExactly(1200)
        assertThat(repository.record.value).isEqualTo(1200)
    }
}
