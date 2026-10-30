package ch.magori.blox.feature.garden

import ch.magori.blox.core.data.CurrentGarden
import ch.magori.blox.core.data.DeletedGarden
import ch.magori.blox.core.data.FakeGardenLibrary
import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.data.GardenLibrary
import ch.magori.blox.core.data.GardenRepository
import ch.magori.blox.core.data.KeptGarden
import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

/** A library that never gives its gardens, nor changes them: the gardens of a slow disk, still being read. */
internal class SilentGardenLibrary : UnwritableGardenLibrary() {
    override val gardens: Flow<List<KeptGarden>> = flow { awaitCancellation() }
}

/**
 * A library whose file cannot be read, as one of a later version: an empty list, once, then the flow ends; nothing can
 * be written (the contract of [GardenLibrary]).
 */
internal class UnreadableGardenLibrary : UnwritableGardenLibrary() {
    override val gardens: Flow<List<KeptGarden>> = flowOf(emptyList())
}

/** A library whose every change fails, as a file of a later version. */
internal abstract class UnwritableGardenLibrary : GardenLibrary {
    /** How many gardens were asked to be kept. */
    var keeps = 0
        private set

    override suspend fun keep(
        name: String,
        garden: Garden,
    ): KeptGarden {
        keeps++
        throw IOException("Gardens of a later version")
    }

    override suspend fun replace(
        id: GardenId,
        garden: Garden,
    ): KeptGarden = throw IOException("Gardens of a later version")

    override suspend fun rename(
        id: GardenId,
        name: String,
    ): Unit = throw IOException("Gardens of a later version")

    override suspend fun delete(id: GardenId): DeletedGarden = throw IOException("Gardens of a later version")

    override suspend fun restore(deleted: DeletedGarden): KeptGarden = throw IOException("Gardens of a later version")
}

/**
 * A library of [FakeGardenLibrary] whose writes of a kept garden ([keep], [replace], [rename], [delete]) wait for
 * [gate]: a slow disk, its changes still being written.
 */
internal class GatedGardenLibrary(private val library: FakeGardenLibrary = FakeGardenLibrary()) :
    GardenLibrary by library {
    val gate = CompletableDeferred<Unit>()

    /** How many gardens were asked to be kept. */
    var keeps = 0
        private set

    override suspend fun keep(
        name: String,
        garden: Garden,
    ): KeptGarden {
        keeps++
        gate.await()
        return library.keep(name, garden)
    }

    override suspend fun replace(
        id: GardenId,
        garden: Garden,
    ): KeptGarden {
        gate.await()
        return library.replace(id, garden)
    }

    /** How many gardens were asked to be renamed. */
    var renames = 0
        private set

    override suspend fun rename(
        id: GardenId,
        name: String,
    ) {
        renames++
        gate.await()
        library.rename(id, name)
    }

    /** How many gardens were asked to be deleted. */
    var deletes = 0
        private set

    override suspend fun delete(id: GardenId): DeletedGarden? {
        deletes++
        gate.await()
        return library.delete(id)
    }
}

/**
 * The garden under way of [kept], whose writes of a garden with its link ([keep] of a [CurrentGarden]) wait for [gate]
 * while one is set: a slow disk. [linkWrites] counts them.
 */
internal class GatedGardenRepository(private val kept: FakeGardenRepository = FakeGardenRepository()) :
    GardenRepository by kept {
    var gate: CompletableDeferred<Unit>? = null

    var linkWrites = 0
        private set

    override suspend fun keep(current: CurrentGarden) {
        linkWrites++
        gate?.await()
        kept.keep(current)
    }
}
