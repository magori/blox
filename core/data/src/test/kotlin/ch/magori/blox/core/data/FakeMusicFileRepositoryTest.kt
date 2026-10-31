package ch.magori.blox.core.data

import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/** The fake shared by the tests of the features keeps the contract of the real music file; its failure is here. */
class FakeMusicFileRepositoryTest : MusicFileRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): MusicFileRepository = FakeMusicFileRepository()

    @Test
    fun `a write that fails, as a full disk, throws and keeps the file before`() = runTest {
        // Given a file kept, then a disk that refuses every write
        val kept = MusicFile("content://music/1")
        val repository = FakeMusicFileRepository(initial = kept, failWrites = true)

        // When
        val other = MusicFile("content://music/2")
        val failure = runCatching { repository.setMusicFile(other, name = null) }.exceptionOrNull()

        // Then
        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(repository.musicFile.first()).isEqualTo(kept)
    }
}
