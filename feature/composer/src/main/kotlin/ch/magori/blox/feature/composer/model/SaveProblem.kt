package ch.magori.blox.feature.composer.model

import ch.magori.blox.core.data.NameProblem

/** Why the save dialog cannot save the music, or the rename dialog rename it. */
internal sealed interface SaveProblem {
    /** Neither grid has a note: the music cannot be saved. */
    data object EmptyGrid : SaveProblem

    /** The music cannot take the name typed, for this [reason], the rule of the library (`nameProblem`, core:data). */
    data class Name(val reason: NameProblem) : SaveProblem
}
