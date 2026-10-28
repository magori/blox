package ch.magori.blox.core.data

import kotlinx.coroutines.CoroutineScope
import java.io.IOException

/** The fake shared by the tests of the features keeps the contract of the real garden kept. */
class FakeGardenRepositoryTest : GardenRepositoryContract() {
    override fun emptyRepository(scope: CoroutineScope): GardenRepository = FakeGardenRepository()

    override suspend fun unwritableRepository(scope: CoroutineScope): GardenRepository =
        FakeGardenRepository(initial = A_GARDEN).apply { failure = IOException("disk full") }
}
