package ch.magori.blox.core.data

import ch.magori.blox.core.music.MusicFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import java.io.IOException

/**
 * A music file kept in memory, [initial] at first (none by default), without a name unless it is given one. With
 * [failWrites], every write fails as a full disk would, and nothing changes. It keeps the contract of the real one
 * (`MusicFileRepositoryContract`).
 */
class FakeMusicFileRepository(initial: MusicFile? = null, private val failWrites: Boolean = false) :
    MusicFileRepository {
    /** The music file kept; the test may change it, as another screen would. */
    val state = MutableStateFlow(initial)

    /** The names of the music files, by file: a name always goes with its own file, never with the one before. */
    private val names = MutableStateFlow(emptyMap<MusicFile, String>())

    override val musicFile: Flow<MusicFile?> get() = state

    override val namedMusicFile: Flow<NamedMusicFile?>
        get() = combine(state, names) { file, named -> file?.let { NamedMusicFile(it, named[it]) } }
            // A file chosen with its name changes the names, then the file: the file before, unchanged, is not told again.
            .distinctUntilChanged()

    override suspend fun setMusicFile(
        file: MusicFile,
        name: String?,
    ) {
        if (failWrites) throw IOException("The disk is full")
        names.update { if (name != null) it + (file to name) else it - file }
        state.value = file
    }
}
