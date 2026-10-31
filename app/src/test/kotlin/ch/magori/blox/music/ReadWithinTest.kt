package ch.magori.blox.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds

/**
 * A blocking read (the name of a document, asked of its provider) is waited for a while only: a provider that does not
 * answer never holds up the choice of the file.
 */
class ReadWithinTest {
    @Test
    fun `a read that answers in time gives its answer`() = runTest {
        // When the read runs on the clock of the test: on Dispatchers.IO, a real thread, the virtual clock could reach
        // the timeout before the read answers
        val read = readWithinOrNull(2.seconds, StandardTestDispatcher(testScheduler)) { "orage-d-ete.ogg" }

        // Then
        assertThat(read).isEqualTo("orage-d-ete.ogg")
    }

    @Test
    fun `a read that does not answer in time gives nothing, without waiting for it any longer`() = runTest {
        // Given a provider that never answers until the test ends
        val stuck = CountDownLatch(1)
        try {
            // When
            val name = readWithinOrNull(2.seconds, Dispatchers.IO) {
                stuck.await()
                "too late"
            }

            // Then nothing, two seconds later on the clock of the test
            assertThat(name).isNull()
            assertThat(testScheduler.currentTime).isEqualTo(2_000)
        } finally {
            stuck.countDown()
        }
    }
}
