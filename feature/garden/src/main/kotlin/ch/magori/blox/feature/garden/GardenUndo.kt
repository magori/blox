package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.DeletedGarden
import ch.magori.blox.core.data.GardenId
import ch.magori.blox.core.garden.Garden

/** What « Annuler » gives back while it is offered, and what its message says (docs/jardin-musical.md). */
@Immutable
internal sealed interface GardenUndo {
    /** Tout effacer or Au hasard: the [garden] from before, « Jardin effacé ». */
    data class Cleared(val garden: Garden) : GardenUndo

    /**
     * Rouvrir, the kept garden called [name] opened, « « X » ouvert »: the [garden] from before, and the kept garden it
     * came from, [keptFrom], null if none.
     */
    data class Opened(val name: String, val garden: Garden, val keptFrom: GardenId?) : GardenUndo

    /**
     * Supprimer, the kept garden [deleted], « « X » supprimé. »: Annuler puts it back at its place, and the garden under
     * way comes from it again if it [linked] before.
     */
    data class Deleted(val deleted: DeletedGarden, val linked: Boolean) : GardenUndo
}
