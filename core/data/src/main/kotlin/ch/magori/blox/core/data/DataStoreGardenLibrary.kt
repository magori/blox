package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path
import java.time.Clock
import java.util.UUID

/**
 * The gardens kept in [dataStore], on [GARDENS_FILE], in a binary format of BloX that carries its version
 * ([GardenLibrarySerializer]). The DataStore is created once, by the application container (ADR 0007): a file has a
 * single DataStore instance. Each change reads and replaces the content in one atomic update; [clock] dates it.
 */
class DataStoreGardenLibrary internal constructor(
    private val dataStore: DataStore<GardenLibraryContent>,
    private val clock: Clock,
) : GardenLibrary {
    /** A library on the file [produceFile] gives, its DataStore living in [scope]: one instance per file. */
    constructor(
        scope: CoroutineScope,
        clock: Clock,
        produceFile: () -> Path,
    ) : this(gardenLibraryDataStore(scope, produceFile), clock)

    /**
     * The kept gardens; an empty list, once, if the file cannot be read, and the flow then ends (a corrupted file is
     * replaced by an empty list, see [gardenLibraryDataStore]).
     */
    override val gardens: Flow<List<KeptGarden>> = dataStore.readOr(GardenLibraryContent()).map { it.gardens }

    /** Keeps [garden] under a random UUID: an identity drawn twice is as good as impossible. */
    override suspend fun keep(
        name: String,
        garden: Garden,
    ): KeptGarden {
        val id = GardenId(UUID.randomUUID().toString())
        return dataStore.updateData { it.keeping(id, name, garden, clock.instant()) }.gardenOf(id)
    }

    override suspend fun replace(
        id: GardenId,
        garden: Garden,
    ): KeptGarden = dataStore.updateData { it.replacing(id, garden, clock.instant()) }.gardenOf(id)

    override suspend fun rename(
        id: GardenId,
        name: String,
    ) {
        dataStore.updateData { it.renaming(id, name, clock.instant()) }
    }

    override suspend fun delete(id: GardenId): DeletedGarden? {
        var deleted: DeletedGarden? = null
        dataStore.updateData { content -> content.deleting(id).also { deleted = it.second }.first }
        return deleted
    }

    override suspend fun restore(deleted: DeletedGarden): KeptGarden =
        dataStore.updateData { it.restoring(deleted) }.gardenOf(deleted.garden.id)
}
