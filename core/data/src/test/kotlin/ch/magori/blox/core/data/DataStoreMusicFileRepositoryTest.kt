package ch.magori.blox.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The contract of the music file, on a DataStore file of its own, and what is proper to a file: kept, its errors. */
class DataStoreMusicFileRepositoryTest : MusicFileRepositoryContract() {
    @TempDir
    lateinit var directory: File

    private val chosen = MusicFile("content://com.android.providers.media.documents/document/audio%3A42")

    @Test
    fun `the music file survives a new repository on the same file`() = runTest {
        // Given a file chosen by a repository whose DataStore is then closed
        inScopeClosedAfter { scope -> repositoryIn(scope).setMusicFile(chosen) }

        // When
        val reopened = repositoryIn(backgroundScope)

        // Then
        assertThat(reopened.musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `the uri is stored as text under a key that must not change`() = runTest {
        // Given a file written by an earlier version
        inScopeClosedAfter { scope ->
            preferencesDataStore(scope) { musicFile.absolutePath.toPath() }
                .edit { it[stringPreferencesKey("uri")] = chosen.uri }
        }

        // When / Then
        assertThat(repositoryIn(backgroundScope).musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `a blank uri in the file gives no music file`() = runTest {
        // Given a file whose uri is blank, which no version writes
        inScopeClosedAfter { scope ->
            preferencesDataStore(scope) {
                musicFile.absolutePath.toPath()
            }.edit { it[stringPreferencesKey("uri")] = " " }
        }

        // When / Then
        assertThat(repositoryIn(backgroundScope).musicFile.first()).isNull()
    }

    @Test
    fun `a corrupted file gives no music file, and a new one is kept`() = runTest {
        // Given a file that is not a preferences file
        musicFile.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42))
        val repository = repositoryIn(backgroundScope)

        // When / Then
        assertThat(repository.musicFile.first()).isNull()
        repository.setMusicFile(chosen)
        assertThat(repository.musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `a file that cannot be read gives no music file`() = runTest {
        // Given a directory where the file should be: reading it fails with an IOException
        musicFile.mkdirs()

        // When / Then
        assertThat(repositoryIn(backgroundScope).musicFile.first()).isNull()
    }

    @Test
    fun `a failure other than an IOException while the music file is read is not absorbed`() = runTest {
        // Given a DataStore whose reading fails with a bug
        val repository = DataStoreMusicFileRepository(FailingDataStore(IllegalStateException("a bug")))

        // When
        val failure = runCatching { repository.musicFile.first() }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IllegalStateException::class.java).hasMessage("a bug")
    }

    override fun emptyRepository(scope: CoroutineScope): MusicFileRepository = repositoryIn(scope)

    /** Runs [block] in a scope of its own, then closes it: the DataStores living in it release the file. */
    private suspend fun TestScope.inScopeClosedAfter(block: suspend (CoroutineScope) -> Unit) {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        block(scope)
        scope.coroutineContext.job.cancelAndJoin()
    }

    /** The music file of the test, in [directory]. */
    private val musicFile: File get() = File(directory, MY_MUSIC_FILE)

    /** A repository on the music file of [directory], its DataStore living in [scope], as in production. */
    private fun repositoryIn(scope: CoroutineScope): MusicFileRepository =
        DataStoreMusicFileRepository(preferencesDataStore(scope) { musicFile.absolutePath.toPath() })
}
