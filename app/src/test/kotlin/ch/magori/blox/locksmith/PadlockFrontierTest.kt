package ch.magori.blox.locksmith

import app.cash.turbine.test
import ch.magori.blox.core.data.PadlockOpening
import ch.magori.blox.core.data.PadlockProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PadlockFrontierTest {
    @Test
    fun `with no padlock open, the frontier is padlock 1`() = runTest {
        // Given an empty progress, as on a new device
        val progress = flowOf(PadlockProgress())

        // When / Then
        assertThat(PadlockFrontier(progress, backgroundScope).await()).isEqualTo(1)
    }

    @Test
    fun `once the progress is read, the frontier is known at once, without waiting for a collection`() = runTest {
        // Given two padlocks open, the progress read once and kept warm
        val progress = MutableStateFlow(PadlockProgress(listOf(OPENING, OPENING)))
        val frontier = PadlockFrontier(progress, backgroundScope)
        runCurrent()

        // When / Then
        assertThat(frontier.current.value).isEqualTo(3)
    }

    @Test
    fun `the frontier moves on as a padlock opens, at each reading of the progress`() = runTest {
        // Given padlock 1 open
        val progress = MutableStateFlow(PadlockProgress(listOf(OPENING)))
        val frontier = PadlockFrontier(progress, backgroundScope)
        runCurrent()

        frontier.current.test {
            assertThat(awaitItem()).isEqualTo(2)

            // When padlock 2 opens in its turn
            progress.value = PadlockProgress(listOf(OPENING, OPENING))

            // Then
            assertThat(awaitItem()).isEqualTo(3)
        }
    }

    @Test
    fun `before the progress is read, no frontier is known, so that no line shows that may have to change`() = runTest {
        // Given a progress not read yet
        val frontier = PadlockFrontier(emptyFlow(), backgroundScope)
        runCurrent()

        // When / Then
        assertThat(frontier.current.value).isNull()
    }

    @Test
    fun `a tap before the progress is read waits for it, rather than open padlock 1 when it is not the frontier`() =
        runTest {
            // Given a progress that is still being read
            val progress = MutableSharedFlow<PadlockProgress>()
            val frontier = PadlockFrontier(progress, backgroundScope)
            val opened = async { frontier.await() }
            runCurrent()
            assertThat(opened.isCompleted).isFalse()

            // When it is read, two padlocks open
            progress.emit(PadlockProgress(listOf(OPENING, OPENING)))

            // Then
            assertThat(opened.await()).isEqualTo(3)
        }
}

/** An opening of a padlock of three pieces, without a refusal nor a hint. */
private val OPENING = PadlockOpening(pieces = 3, moves = 3, refusals = 0, hints = 0)
