package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The garden kept in memory, [initial] at first (by default [Garden.EMPTY], empty and calm), that keeps the contract
 * of [GardenRepository] (`GardenRepositoryContract`). For the tests of a view model: a garden kept then fails with
 * [failure] when there is one, the garden unchanged, as a file that cannot be written.
 */
class FakeGardenRepository(initial: Garden = Garden.EMPTY) : GardenRepository {
    var failure: Exception? = null

    /** The garden kept; the test may change it, as an earlier visit would have. */
    val state = MutableStateFlow(initial)

    override val garden: Flow<Garden> get() = state

    override suspend fun keep(garden: Garden) {
        failure?.let { throw it }
        state.value = garden
    }
}
