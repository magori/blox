package ch.magori.blox.core.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import okio.Path

/**
 * The progress of the padlocks kept in [dataStore], on [PADLOCKS_FILE], in a binary format of BloX that carries its
 * version and the generation of the padlocks ([PadlockProgressSerializer]). The DataStore is created once, by the
 * application container (ADR 0007): a file has a single DataStore instance. Each change reads and replaces the content
 * in one atomic update.
 */
class DataStorePadlockProgressRepository internal constructor(private val dataStore: DataStore<PadlockProgress>) :
    PadlockProgressRepository {
    /** A repository on the file [produceFile] gives, its DataStore living in [scope]: one instance per file. */
    constructor(
        scope: CoroutineScope,
        produceFile: () -> Path,
    ) : this(padlockProgressDataStore(scope, produceFile))

    /**
     * The progress; an empty one, once, if the file cannot be read, and the flow then ends (a corrupted file is
     * replaced by an empty progress, see [padlockProgressDataStore]).
     */
    override val progress: Flow<PadlockProgress> = dataStore.readOr(PadlockProgress())

    override suspend fun record(
        padlock: Int,
        opening: PadlockOpening,
    ): OpeningOutcome {
        var outcome: OpeningOutcome? = null
        dataStore.updateData { progress -> progress.recording(padlock, opening).also { outcome = it.second }.first }
        return checkNotNull(outcome)
    }

    override suspend fun clear() {
        dataStore.updateData { PadlockProgress() }
    }
}
