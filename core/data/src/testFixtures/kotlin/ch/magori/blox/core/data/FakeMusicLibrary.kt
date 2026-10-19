package ch.magori.blox.core.data

import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Saved musics kept in memory, that keep the contract of [MusicLibrary] (`MusicLibraryContract`): the rules are
 * those of the real library, [MusicLibraryContent], only the file is left out.
 */
class FakeMusicLibrary : MusicLibrary {
    private val content = MutableStateFlow(MusicLibraryContent())

    /** The identities given so far: the next one is « music-<count + 1> », or the first after it no music has. */
    private var ids = 0

    override fun newId(): ScoreId.Personal {
        val saved = content.value.musics.map { it.id }.toSet()
        return generateSequence { ScoreId.Personal("music-${++ids}") }.first { it !in saved }
    }

    override val musics: Flow<List<SavedMusic>> = content.map { it.musics }.distinctUntilChanged()

    override val gameMusic: Flow<SavedMusic?> = content.map { it.gameMusic }.distinctUntilChanged()

    override suspend fun save(music: SavedMusic) {
        content.update { it.saving(music) }
    }

    override suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    ) {
        content.update { it.renaming(id, name) }
    }

    override suspend fun setGameMusic(id: ScoreId.Personal?) {
        content.update { it.choosingForGames(id) }
    }

    override suspend fun delete(id: ScoreId.Personal): DeletedMusic? {
        var deleted: DeletedMusic? = null
        content.update { current -> current.deleting(id).also { deleted = it.second }.first }
        return deleted
    }

    override suspend fun restore(deleted: DeletedMusic) {
        content.update { it.restoring(deleted) }
    }
}
