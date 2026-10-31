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
        repository.setMusicFile(chosen, name = null)

        // Then
        assertThat(repository.musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `the name of the music file chosen is read back with it`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        // When
        repository.setMusicFile(chosen, name = "orage-d-ete.ogg")

        // Then
        assertThat(repository.namedMusicFile.first()).isEqualTo(NamedMusicFile(chosen, "orage-d-ete.ogg"))
    }

    @Test
    fun `a music file chosen without a name has none, even when it was kept with one before`() = runTest {
        // Given the file kept with its name
        val repository = emptyRepository(backgroundScope)
        repository.setMusicFile(chosen, name = "orage-d-ete.ogg")

        // When it is chosen again, its name unknown
        repository.setMusicFile(chosen, name = null)

        // Then
        assertThat(repository.namedMusicFile.first()).isEqualTo(NamedMusicFile(chosen, name = null))
    }

    @Test
    fun `a music file chosen again replaces the one before`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)
        repository.setMusicFile(chosen, name = null)

        // When
        repository.setMusicFile(other, name = null)

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
            repository.setMusicFile(chosen, name = null)
            val first = awaitItem()
            repository.setMusicFile(chosen, name = null)
            repository.setMusicFile(other, name = null)

            // Then
            assertThat(first).isEqualTo(chosen)
            assertThat(awaitItem()).isEqualTo(other)
            expectNoEvents()
        }
    }

    @Test
    fun `another music file chosen with its name replaces the name of the one before`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)
        repository.setMusicFile(chosen, name = "orage-d-ete.ogg")

        // When
        repository.setMusicFile(other, name = "a.ogg")

        // Then
        assertThat(repository.namedMusicFile.first()).isEqualTo(NamedMusicFile(other, "a.ogg"))
    }

    @Test
    fun `the named music file flow emits each change once, and nothing when the same file is chosen again`() = runTest {
        // Given
        val repository = emptyRepository(backgroundScope)

        repository.namedMusicFile.test {
            assertThat(awaitItem()).isNull()

            // When a file is chosen with its name, then chosen again, then another one with its own
            repository.setMusicFile(chosen, name = "orage-d-ete.ogg")
            val first = awaitItem()
            repository.setMusicFile(chosen, name = "orage-d-ete.ogg")
            repository.setMusicFile(other, name = "a.ogg")

            // Then
            assertThat(first).isEqualTo(NamedMusicFile(chosen, "orage-d-ete.ogg"))
            assertThat(awaitItem()).isEqualTo(NamedMusicFile(other, "a.ogg"))
            expectNoEvents()
        }
    }
}
