package ch.magori.blox.core.data

import app.cash.turbine.test
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The contract of [MusicFileRepository], what every implementation does, the real one and the fake shared by the
 * tests of the features alike. A subclass gives an empty repository; what is proper to an implementation (a file,
 * its errors) is tested in the subclass.
 */
abstract class MusicFileRepositoryContract {
    /** A repository without any music file kept, whatever it needs to live in [scope] (a DataStore does). */
    protected abstract fun emptyRepository(scope: CoroutineScope): MusicFileRepository

    private val chosen = MusicFile("content://com.android.providers.media.documents/document/audio%3A42")
    private val other = MusicFile("content://com.android.externalstorage.documents/document/primary%3AMusic%2Fa.ogg")

    @Test
    fun `without a music file chosen there is none`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When / Then
        assertThat(repository.musicFile.first()).isNull()
    }

    @Test
    fun `the music file chosen is read back`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setMusicFile(chosen)

        // Then
        assertThat(repository.musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `a music file chosen again replaces the one before`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)
        repository.setMusicFile(chosen)

        // When
        repository.setMusicFile(other)

        // Then
        assertThat(repository.musicFile.first()).isEqualTo(other)
    }

    @Test
    fun `the music file flow emits each change once, and nothing when the same file is chosen again`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.musicFile.test {
            assertThat(awaitItem()).isNull()

            // When a file is chosen, then chosen again, then another one
            repository.setMusicFile(chosen)
            val first = awaitItem()
            repository.setMusicFile(chosen)
            repository.setMusicFile(other)

            // Then
            assertThat(first).isEqualTo(chosen)
            assertThat(awaitItem()).isEqualTo(other)
            expectNoEvents()
        }
    }
}
