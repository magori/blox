package ch.magori.blox.core.data

import ch.magori.blox.core.garden.Garden
import java.time.Instant

/**
 * What a [GardenLibrary] holds: the kept [gardens], in the order they were first kept. Its changes are the rules of the
 * library, shared by the real one and the fake: each one gives a new content, or refuses with an
 * [IllegalArgumentException] what would break them (more than [GardenLibrary.MAX_GARDENS] gardens, two gardens of the
 * same name, a garden that is not kept). A change dates the garden it changes, to the millisecond, as the gardens file
 * keeps it: the garden read before the file is opened again is the one read after.
 */
internal data class GardenLibraryContent(val gardens: List<KeptGarden> = emptyList()) {
    /** The kept garden of [id]; refused if none has it. */
    fun gardenOf(id: GardenId): KeptGarden = gardens[placeOf(id)]

    /** [garden] called [name], kept under [id] [at] that time, after the others. */
    fun keeping(
        id: GardenId,
        name: String,
        garden: Garden,
        at: Instant,
    ): GardenLibraryContent {
        requireRoom()
        requireFreeName(name, id)
        return copy(gardens = gardens + KeptGarden(id, name, garden, at.toFileTime()))
    }

    /** The garden of [id] made of [garden], at its place, under its name, changed [at] that time. */
    fun replacing(
        id: GardenId,
        garden: Garden,
        at: Instant,
    ): GardenLibraryContent = changing(placeOf(id)) { it.copy(garden = garden, modifiedAt = at.toFileTime()) }

    /**
     * The garden of [id] called [name], at its place, changed [at] that time; unchanged if it is called [name] already,
     * the very same.
     */
    fun renaming(
        id: GardenId,
        name: String,
        at: Instant,
    ): GardenLibraryContent {
        val place = placeOf(id)
        requireFreeName(name, id)
        if (gardens[place].name == name) return this
        return changing(place) { it.copy(name = name, modifiedAt = at.toFileTime()) }
    }

    /** The garden of [id] taken out, and what [restoring] needs to put it back; unchanged and null if none has it. */
    fun deleting(id: GardenId): Pair<GardenLibraryContent, DeletedGarden?> {
        val place = gardens.indexOfFirst { it.id == id }
        if (place < 0) return this to null
        val left = copy(gardens = gardens.filterIndexed { index, _ -> index != place })
        return left to DeletedGarden(gardens[place], place)
    }

    /**
     * The [deleted] garden back at its place, at the end of the list at the most; unchanged if it is back already. If
     * another garden took its name since, it comes back under a free one ([TakenNames.fittingFreeName]).
     */
    fun restoring(deleted: DeletedGarden): GardenLibraryContent {
        if (gardens.any { it.id == deleted.garden.id }) return this
        requireRoom()
        val garden = deleted.garden.copy(
            name = gardens.takenNames().fittingFreeName(deleted.garden.name),
            modifiedAt = deleted.garden.modifiedAt.toFileTime(),
        )
        return copy(gardens = gardens.toMutableList().apply { add(deleted.place.coerceAtMost(size), garden) })
    }

    /** The place of the garden of [id]; refused if none has it. */
    private fun placeOf(id: GardenId): Int =
        gardens.indexOfFirst { it.id == id }.also { require(it >= 0) { "No kept garden is known by $id" } }

    /** The garden at [place] changed by [change], the others as they are. */
    private fun changing(
        place: Int,
        change: (KeptGarden) -> KeptGarden,
    ): GardenLibraryContent = copy(gardens = gardens.toMutableList().apply { set(place, change(gardens[place])) })

    /** Refuses one more garden in a full library, of [GardenLibrary.MAX_GARDENS]. */
    private fun requireRoom() {
        require(gardens.size < GardenLibrary.MAX_GARDENS) {
            "A library holds ${GardenLibrary.MAX_GARDENS} gardens at the most"
        }
    }

    private fun requireFreeName(
        name: String,
        apartFrom: GardenId,
    ) {
        require(!gardens.takenNames().isTaken(name, apartFrom)) { "Another kept garden is called \"$name\"" }
    }
}
