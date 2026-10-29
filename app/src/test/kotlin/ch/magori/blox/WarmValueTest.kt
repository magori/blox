package ch.magori.blox

import app.cash.turbine.test
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WarmValueTest {
    @Test
    fun `before the source is read, no value is known`() = runTest {
        // Given a source not read yet
        val value = WarmValue(emptyFlow<Int>(), backgroundScope)
        runCurrent()

        // When / Then
        assertThat(value.current.value).isNull()
    }

    @Test
    fun `awaiting the value before the source is read waits for it`() = runTest {
        // Given a source that is still being read
        val source = MutableSharedFlow<Int>()
        val value = WarmValue(source, backgroundScope)
        val awaited = async { value.await() }
        runCurrent()
        assertThat(awaited.isCompleted).isFalse()

        // When it is read
        source.emit(3)

        // Then
        assertThat(awaited.await()).isEqualTo(3)
    }

    @Test
    fun `the updates give each value read, from the first, without the unknown one before`() = runTest {
        // Given a source not read yet
        val source = MutableSharedFlow<Int>()
        val value = WarmValue(source, backgroundScope)

        value.updates.test {
            runCurrent()
            expectNoEvents()

            // When it is read twice
            source.emit(1)
            source.emit(2)

            // Then
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
        }
    }

    @Test
    fun `once the source is read, the value is known at once, without waiting for a collection`() = runTest {
        // Given a source read once and kept warm
        val value = WarmValue(MutableStateFlow(3), backgroundScope)
        runCurrent()

        // When / Then
        assertThat(value.current.value).isEqualTo(3)
    }

    @Test
    fun `awaiting the value once it is known gives the last one read at once`() = runTest {
        // Given a source read twice
        val source = MutableStateFlow(1)
        val value = WarmValue(source, backgroundScope)
        runCurrent()
        source.value = 2
        runCurrent()

        // When / Then
        assertThat(value.await()).isEqualTo(2)
    }

    @Test
    fun `a wait cancelled before the source is read ends, and the value stays warm for the others`() = runTest {
        // Given a wait on a source still being read
        val source = MutableSharedFlow<Int>()
        val value = WarmValue(source, backgroundScope)
        val awaited = async { value.await() }
        runCurrent()

        // When the wait is cancelled, then the source is read
        awaited.cancel()
        runCurrent()
        source.emit(3)
        runCurrent()

        // Then the wait ended without a value, and the source is still read
        assertThat(awaited.isCancelled).isTrue()
        assertThat(value.current.value).isEqualTo(3)
    }
}
