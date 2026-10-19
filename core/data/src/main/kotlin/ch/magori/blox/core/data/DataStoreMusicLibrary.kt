package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import ch.magori.blox.core.music.ScoreId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.Path
import java.util.UUID

/**
 * The musics kept in [dataStore], on [MUSICS_FILE]. The DataStore is created once, by the application container
 * (ADR 0007): a file has a single DataStore instance. Each change reads and replaces the content in one atomic update.
 */
class DataStoreMusicLibrary internal constructor(private val dataStore: DataStore<MusicLibraryContent>) : MusicLibrary {
    /** A library on the file [produceFile] gives, its DataStore living in [scope]: one instance per file. */
    constructor(scope: CoroutineScope, produceFile: () -> Path) : this(musicLibraryDataStore(scope, produceFile))

    /** A random UUID: an identity drawn twice is as good as impossible. */
    override fun newId(): ScoreId.Personal = ScoreId.Personal(UUID.randomUUID().toString())

    override val musics: Flow<List<SavedMusic>> = dataStore.readOr(MusicLibraryContent()).map {
        it.musics
    }.distinctUntilChanged()

    override val gameMusic: Flow<SavedMusic?> = dataStore.readOr(MusicLibraryContent()).map {
        it.gameMusic
    }.distinctUntilChanged()

    override suspend fun save(music: SavedMusic) {
        dataStore.updateData { it.saving(music) }
    }

    override suspend fun rename(
        id: ScoreId.Personal,
        name: String,
    ) {
        dataStore.updateData { it.renaming(id, name) }
    }

    override suspend fun setGameMusic(id: ScoreId.Personal?) {
        dataStore.updateData { it.choosingForGames(id) }
    }

    override suspend fun delete(id: ScoreId.Personal): DeletedMusic? {
        var deleted: DeletedMusic? = null
        dataStore.updateData { content -> content.deleting(id).also { deleted = it.second }.first }
        return deleted
    }

    override suspend fun restore(deleted: DeletedMusic) {
        dataStore.updateData { it.restoring(deleted) }
    }
}
