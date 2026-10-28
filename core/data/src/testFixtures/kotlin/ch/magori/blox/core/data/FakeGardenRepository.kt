package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The garden kept in memory, [initial] at first (by default [Garden.EMPTY], empty and calm) and from no kept garden,
 * that keeps the contract of [GardenRepository] (`GardenRepositoryContract`). For the tests of a view model: a garden
 * kept then fails with [failure] when there is one, the garden unchanged, as a file that cannot be written.
 */
class FakeGardenRepository(initial: Garden = Garden.EMPTY) : GardenRepository {
    var failure: Exception? = null

    /** The garden kept and the kept garden it comes from, a single state changed at once; the test may change it. */
    val currentState = MutableStateFlow(CurrentGarden(initial, keptFrom = null))

    /** The garden kept, in [currentState]; the test may change it, as an earlier visit would have, its link kept. */
    val state: Field<Garden> = Field({ it.garden }) { current, garden -> current.copy(garden = garden) }

    /** The kept garden the garden kept comes from, in [currentState], null if none; the test may change it too. */
    val keptFrom: Field<GardenId?> = Field({ it.keptFrom }) { current, id -> current.copy(keptFrom = id) }

    override val current: Flow<CurrentGarden> get() = currentState

    override val garden: Flow<Garden> get() = currentState.map { it.garden }.distinctUntilChanged()

    override suspend fun keep(garden: Garden) {
        failure?.let { throw it }
        state.value = garden
    }

    override suspend fun keep(current: CurrentGarden) {
        failure?.let { throw it }
        currentState.value = current
    }

    /** A part of [currentState], read and changed through [value], the rest of the state unchanged. */
    inner class Field<T> internal constructor(
        private val read: (CurrentGarden) -> T,
        private val change: (CurrentGarden, T) -> CurrentGarden,
    ) {
        var value: T
            get() = read(currentState.value)
            set(value) = currentState.update { change(it, value) }
    }
}
