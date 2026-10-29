package ch.magori.blox.feature.garden

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.data.NameProblem

/**
 * The sheet « Mes jardins », open over the garden (docs/jardin-musical.md, Mes jardins): the window where the name of a
 * garden to keep is typed, [naming], while it is open; while a change of the gardens is being written, [writing], its
 * buttons are greyed; when it could not be written, [failed], the sheet says so and stays open.
 */
@Immutable
internal data class GardensSheet(
    val naming: GardenNaming? = null,
    val failed: Boolean = false,
    val writing: Boolean = false,
)

/**
 * The window « Garder ce jardin » or « Garder une copie » (`NamingWindow`): the [name] typed, why a garden cannot take
 * it, [problem], by the rules of the names of the gardens, and, when it could not be written, [failed]: it stays open.
 */
@Immutable
internal data class GardenNaming(val name: String, val problem: NameProblem? = null, val failed: Boolean = false)

/** Why the garden under way cannot be kept as a new garden, « Garder ce jardin » or « Garder une copie » greyed. */
internal enum class KeepRefusal {
    /** The kept gardens are still being read. */
    LOADING,

    /** The kept gardens cannot be read, a file of a later version: nothing can be written until it comes. */
    UNREADABLE,

    /** Not a piece placed: an empty garden is not kept. */
    EMPTY_GARDEN,

    /** [ch.magori.blox.core.data.GardenLibrary.MAX_GARDENS] gardens kept already. */
    FULL,
}
