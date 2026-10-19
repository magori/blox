package ch.magori.blox.core.data

import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * A music file kept in memory, [initial] at first (none by default). With [failWrites], every write fails as a full
 * disk would, and nothing changes. It keeps the contract of the real one (`MusicFileRepositoryContract`).
 */
class FakeMusicFileRepository(initial: MusicFile? = null, private val failWrites: Boolean = false) :
    MusicFileRepository {
    /** The music file kept; the test may change it, as another screen would. */
    val state = MutableStateFlow(initial)

    override val musicFile: Flow<MusicFile?> get() = state

    override suspend fun setMusicFile(file: MusicFile) {
        if (failWrites) throw IOException("The disk is full")
        state.value = file
    }
}
