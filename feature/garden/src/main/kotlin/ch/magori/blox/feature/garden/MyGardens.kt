package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.KeptGarden

/** What the screen knows of « Mes jardins » (docs/jardin-musical.md, Mes jardins), the library of the kept gardens. */
@Immutable
internal sealed interface MyGardens {
    /** Not read yet: the sheet greys its buttons and shows no list. */
    data object Loading : MyGardens

    /** The kept [gardens], in the order they were first kept, as the library gives them. */
    data class Listed(val gardens: List<KeptGarden>) : MyGardens

    /**
     * Gardens that cannot be read, a file of a later version: the list is empty, and nothing can be kept until that
     * version comes.
     */
    data object Unreadable : MyGardens
}
