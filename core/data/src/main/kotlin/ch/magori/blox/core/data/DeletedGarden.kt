package ch.magori.blox.core.data

/**
 * A garden [GardenLibrary.delete] took out of the list, kept by the screen until its next action so that « Annuler »
 * may give it back ([GardenLibrary.restore]): the [garden] as it was kept, and its [place] in the list, 0 or more.
 */
data class DeletedGarden(val garden: KeptGarden, val place: Int) {
    init {
        require(place >= 0) { "A deleted garden has a place of 0 or more, was $place" }
    }
}
