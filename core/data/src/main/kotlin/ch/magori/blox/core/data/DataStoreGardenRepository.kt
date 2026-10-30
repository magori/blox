package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path

/**
 * The garden kept in [dataStore], on [GARDEN_FILE], in a binary format of BloX that carries its version
 * ([GardenSerializer]). The DataStore is created once, by the application container (ADR 0007): a file has a single
 * DataStore instance.
 */
class DataStoreGardenRepository internal constructor(private val dataStore: DataStore<CurrentGarden>) :
    GardenRepository {
    /** A repository on the file [produceFile] gives, its DataStore living in [scope]: one instance per file. */
    constructor(
        scope: CoroutineScope,
        produceFile: () -> Path,
    ) : this(gardenDataStore(scope, produceFile))

    /**
     * The garden kept and the kept garden it comes from; [CurrentGarden.EMPTY], once, if the file cannot be read, and
     * the flow then ends (a corrupted file is replaced by the empty garden, see [gardenDataStore]).
     */
    override val current: Flow<CurrentGarden> = dataStore.readOr(CurrentGarden.EMPTY)

    /** The garden kept, as [current] gives it. */
    override val garden: Flow<Garden> = current.map { it.garden }

    /** Keeps [garden] in a single write with the kept garden it comes from, read from the file: no change is lost. */
    override suspend fun keep(garden: Garden) {
        dataStore.updateData { it.copy(garden = garden) }
    }

    override suspend fun keep(current: CurrentGarden) {
        dataStore.updateData { current }
    }
}
