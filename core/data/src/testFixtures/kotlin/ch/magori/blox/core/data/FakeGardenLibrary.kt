package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kept gardens in memory, that keep the contract of [GardenLibrary] (`GardenLibraryContract`): the rules are those of
 * the real library, [GardenLibraryContent], only the file is left out. [clock] dates the changes: stopped at
 * [MODIFIED_AT] unless a test gives another one. For the tests of a view model: each change then fails with [failure]
 * when there is one, the gardens unchanged, as a file that cannot be written.
 */
class FakeGardenLibrary(private val clock: Clock = MutableClock()) : GardenLibrary {
    var failure: Exception? = null

    private val content = MutableStateFlow(GardenLibraryContent())

    /** The identities given so far: the next one is « garden-<count + 1> », never given before. */
    private val ids = AtomicInteger()

    override val gardens: Flow<List<KeptGarden>> = content.map { it.gardens }

    override suspend fun keep(
        name: String,
        garden: Garden,
    ): KeptGarden {
        val id = GardenId("garden-${ids.incrementAndGet()}")
        return changing { it.keeping(id, name, garden, clock.instant()) }.gardenOf(id)
    }

    override suspend fun replace(
        id: GardenId,
        garden: Garden,
    ): KeptGarden = changing { it.replacing(id, garden, clock.instant()) }.gardenOf(id)

    override suspend fun rename(
        id: GardenId,
        name: String,
    ) {
        changing { it.renaming(id, name, clock.instant()) }
    }

    override suspend fun delete(id: GardenId): DeletedGarden? {
        var deleted: DeletedGarden? = null
        changing { current -> current.deleting(id).also { deleted = it.second }.first }
        return deleted
    }

    override suspend fun restore(deleted: DeletedGarden): KeptGarden =
        changing { it.restoring(deleted) }.gardenOf(deleted.garden.id)

    /** The content changed by [change], unless a [failure] is set: it is then thrown, the content unchanged. */
    private fun changing(change: (GardenLibraryContent) -> GardenLibraryContent): GardenLibraryContent {
        failure?.let { throw it }
        return content.updateAndGet(change)
    }
}
