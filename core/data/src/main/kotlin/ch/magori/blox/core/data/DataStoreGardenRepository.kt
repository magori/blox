package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import okio.Path

/**
 * The garden kept in [dataStore], on [GARDEN_FILE], in a binary format of BloX that carries its version
 * ([GardenSerializer]). The DataStore is created once, by the application container (ADR 0007): a file has a single
 * DataStore instance.
 */
class DataStoreGardenRepository internal constructor(private val dataStore: DataStore<Garden>) : GardenRepository {
    /** A repository on the file [produceFile] gives, its DataStore living in [scope]: one instance per file. */
    constructor(
        scope: CoroutineScope,
        produceFile: () -> Path,
    ) : this(gardenDataStore(scope, produceFile))

    /**
     * The garden kept; [Garden.EMPTY], once, if the file cannot be read, and the flow then ends (a corrupted file is
     * replaced by the empty garden, see [gardenDataStore]).
     */
    override val garden: Flow<Garden> = dataStore.readOr(Garden.EMPTY)

    override suspend fun keep(garden: Garden) {
        dataStore.updateData { garden }
    }
}
