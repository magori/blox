package ch.magori.blox.feature.garden

import ch.magori.blox.core.data.FakeGardenRepository
import ch.magori.blox.core.data.GardenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.random.Random

/** The view model of the garden, opened on the garden [kept], its writes made at once, its rolls drawn from [random]. */
internal fun gardenViewModel(
    kept: GardenRepository = FakeGardenRepository(),
    random: Random = Random(0),
): GardenViewModel =
    GardenViewModel(kept, CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()), Mutex(), random)
