package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import kotlinx.coroutines.flow.Flow

/**
 * The gardens of the player, « Mes jardins » (P10.27), kept on the device by name, apart from the garden in progress
 * ([GardenRepository]). [MAX_GARDENS] at the most.
 *
 * Each change fails with an [java.io.IOException] if the gardens cannot be written: the only failure a screen catches.
 * An [IllegalArgumentException] is a precondition the caller broke, a bug, not to be caught: the screen checks a name
 * by the rules of every list ([TakenNames]) and the room left before it keeps or renames a garden, and changes only a
 * garden of the list.
 */
interface GardenLibrary {
    /**
     * The kept gardens, in the order they were first kept.
     *
     * Never fails with an [java.io.IOException]: gardens that cannot be read give an empty list, once, and the flow then
     * ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val gardens: Flow<List<KeptGarden>>

    /**
     * Keeps [garden] called [name] after the others, under an identity of its own, dated by the library: « Garder ce
     * jardin », « Garder une copie ». Gives it as kept.
     *
     * @throws java.io.IOException if the gardens cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if [MAX_GARDENS] gardens are kept already, if another garden is called [name],
     * the case and the spaces around it ignored, or if [name] is not one a garden may have ([KeptGarden]):
     * preconditions.
     */
    suspend fun keep(
        name: String,
        garden: Garden,
    ): KeptGarden

    /**
     * Replaces the pieces and the mood of the garden of [id] with those of [garden], « Enregistrer « X » »: it keeps
     * its identity, its name and its place, and takes the time of the change. A full library may still replace one.
     * Gives it as kept.
     *
     * @throws java.io.IOException if the gardens cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if no garden is known by [id]: a precondition.
     */
    suspend fun replace(
        id: GardenId,
        garden: Garden,
    ): KeptGarden

    /**
     * Renames the garden of [id] [name]; it keeps its place and its pieces, and takes the time of the change, unless
     * [name] is its very name, and nothing changes.
     *
     * @throws java.io.IOException if the gardens cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if no garden is known by [id], if another one is called [name], or if [name] is
     * not one a garden may have: preconditions.
     */
    suspend fun rename(
        id: GardenId,
        name: String,
    )

    /**
     * Deletes the garden of [id]. What it gives back lets [restore] put the garden back; null when no garden is known by
     * [id], and nothing changes.
     *
     * @throws java.io.IOException if the gardens cannot be written; any other failure is a bug.
     */
    suspend fun delete(id: GardenId): DeletedGarden?

    /**
     * Puts a [deleted] garden back at its place, « Annuler » after a deletion: at the end of the list at the most, as it
     * was kept, its time included. If another garden took its name since, under a free one, numbered
     * (« Jardin calme 2 »). A garden back already stays as it is. Gives it as kept.
     *
     * @throws java.io.IOException if the gardens cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if [MAX_GARDENS] gardens are kept again since: a precondition.
     */
    suspend fun restore(deleted: DeletedGarden): KeptGarden

    companion object {
        /** The most gardens a library holds: a list that stays readable. */
        const val MAX_GARDENS: Int = 30
    }
}
