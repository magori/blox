package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow

/**
 * The garden under way of the Jardin musical, its pieces and its mood, kept on the device without a button: written at
 * each change ([keep]), read when the garden opens ([garden]). It remembers the garden of « Mes jardins » it comes from
 * ([current]), if any. No export to the saved musics.
 */
interface GardenRepository {
    /**
     * The last garden kept; [Garden.EMPTY], empty and calm, before any.
     *
     * Never fails with an [java.io.IOException]: a garden that cannot be read gives [Garden.EMPTY], once, and the flow
     * then ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val garden: Flow<Garden>

    /**
     * The last garden kept and the kept garden it comes from; [CurrentGarden.EMPTY], empty, calm and from no kept
     * garden, before any. Never fails with an [java.io.IOException], as [garden].
     */
    val current: Flow<CurrentGarden>

    /**
     * Keeps [garden] in place of the last one, as a change of it: it still comes from the same kept garden.
     *
     * @throws java.io.IOException if the garden cannot be written; any other failure is a bug.
     */
    suspend fun keep(garden: Garden)

    /**
     * Keeps [current] in place of the last one, the kept garden it comes from with it: a kept garden opened, the garden
     * under way kept in « Mes jardins », or its kept garden deleted (`keptFrom` null).
     *
     * @throws java.io.IOException if the garden cannot be written; any other failure is a bug.
     */
    suspend fun keep(current: CurrentGarden)
}
