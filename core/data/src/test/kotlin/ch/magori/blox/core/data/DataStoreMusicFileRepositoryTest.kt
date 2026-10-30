package ch.magori.blox.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The contract of the music file, on a DataStore file of its own, and what is proper to a file: kept, its errors. */
class DataStoreMusicFileRepositoryTest :
    MusicFileRepositoryContract(),
    DataStoreFileContract<MusicFileRepository> {
    @TempDir
    lateinit var directory: File

    private val chosen = MusicFile("content://com.android.providers.media.documents/document/audio%3A42")

    @Test
    fun `the music file survives a new repository on the same file`() = runTest {
        // Given a file chosen by a repository whose DataStore is then closed
        inScopeClosedAfter { scope -> repositoryIn(scope).setMusicFile(chosen, name = null) }

        // When
        val reopened = repositoryIn(backgroundScope)

        // Then
        assertThat(reopened.musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `the uri is stored as text under a key that must not change`() = runTest {
        // Given a file written by an earlier version
        inScopeClosedAfter { scope ->
            preferencesDataStore(scope) { file.absolutePath.toPath() }
                .edit { it[stringPreferencesKey("uri")] = chosen.uri }
        }

        // When / Then
        assertThat(repositoryIn(backgroundScope).musicFile.first()).isEqualTo(chosen)
    }

    @Test
    fun `a music file kept by an earlier version, without a name, is read back without one`() = runTest {
        // Given a file written before the names were kept (P5.38): its uri only
        inScopeClosedAfter { scope ->
            preferencesDataStore(scope) { file.absolutePath.toPath() }
                .edit { it[stringPreferencesKey("uri")] = chosen.uri }
        }

        // When / Then
        assertThat(repositoryIn(backgroundScope).namedMusicFile.first()).isEqualTo(NamedMusicFile(chosen, name = null))
    }

    @Test
    fun `the name is stored as text under a key that must not change`() = runTest {
        // Given a file chosen with its name by a repository whose DataStore is then closed
        inScopeClosedAfter { scope -> repositoryIn(scope).setMusicFile(chosen, name = "orage-d-ete.ogg") }

        // When the file is read as it is stored
        val stored = preferencesDataStore(backgroundScope) { file.absolutePath.toPath() }.data.first()

        // Then
        assertThat(stored[stringPreferencesKey("name")]).isEqualTo("orage-d-ete.ogg")
    }

    @Test
    fun `a blank uri in the file gives no music file`() = runTest {
        // Given a file whose uri is blank, which no version writes
        inScopeClosedAfter { scope ->
            preferencesDataStore(scope) {
                file.absolutePath.toPath()
            }.edit { it[stringPreferencesKey("uri")] = " " }
        }

        // When / Then
        assertThat(repositoryIn(backgroundScope).musicFile.first()).isNull()
    }

    override fun emptyRepository(scope: CoroutineScope): MusicFileRepository = repositoryIn(scope)

    /** The file of the music file, in [directory]. */
    override val file: File get() = File(directory, MY_MUSIC_FILE)

    override fun repositoryIn(scope: CoroutineScope): MusicFileRepository =
        DataStoreMusicFileRepository(preferencesDataStore(scope) { file.absolutePath.toPath() })

    override fun repositoryFailingWith(failure: Exception): MusicFileRepository =
        DataStoreMusicFileRepository(FailingDataStore(failure))

    override fun readsOf(repository: MusicFileRepository): List<Flow<Any?>> =
        listOf(repository.musicFile, repository.namedMusicFile)

    /** No music file, nor any name. */
    override val emptyReads: List<Any?> = listOf(null, null)

    /** A music file chosen, which is kept. */
    override suspend fun write(repository: MusicFileRepository) = repository.setMusicFile(chosen, name = null)

    override val readsAfterWrite: List<Any?> get() = listOf(chosen, NamedMusicFile(chosen, name = null))
}
