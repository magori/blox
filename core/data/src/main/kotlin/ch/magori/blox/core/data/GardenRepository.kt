package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow

/**
 * The last garden of the Jardin musical, its pieces and its mood, kept on the device without a button: written at each
 * change ([keep]), read when the garden opens ([garden]). No export to the saved musics.
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
     * Keeps [garden] in place of the last one.
     *
     * @throws java.io.IOException if the garden cannot be written; any other failure is a bug.
     */
    suspend fun keep(garden: Garden)
}
