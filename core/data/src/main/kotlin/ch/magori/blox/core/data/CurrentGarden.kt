package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden

/**
 * The garden under way of the Jardin musical, [garden], and the garden of « Mes jardins » it comes from, [keptFrom]:
 * the one it was opened from or last kept as, null if it comes from none. The link stays while the garden changes, so
 * that it can be saved in place of its kept garden (P10.29).
 */
data class CurrentGarden(val garden: Garden, val keptFrom: GardenId?) {
    companion object {
        /** The empty garden, calm, from no kept garden: the garden under way before any. */
        val EMPTY: CurrentGarden = CurrentGarden(Garden.EMPTY, keptFrom = null)
    }
}
