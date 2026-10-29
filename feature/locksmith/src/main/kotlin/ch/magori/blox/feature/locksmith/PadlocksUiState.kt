package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.locksmith.PadlockResult

/**
 * What the screen « Serrures » shows: the best opening of each open padlock, [openings], padlock 1 first. The padlocks
 * open in order: the open ones are 1 to [open], the next to open is the [frontier], which Reprendre opens.
 *
 * Immutable for Compose: the list of [openings] is never changed once built.
 */
@Immutable
internal data class PadlocksUiState(val openings: List<PadlockResult>) {
    /** How many padlocks are open. */
    val open: Int get() = openings.size

    /** How many padlocks are open perfectly. */
    val perfect: Int get() = openings.count(PadlockResult::isPerfect)

    /** The next padlock to open. */
    val frontier: Int get() = open + 1

    /** Whether no padlock is open yet: the screen asks to open padlock 1. */
    val isEmpty: Boolean get() = openings.isEmpty()
}
