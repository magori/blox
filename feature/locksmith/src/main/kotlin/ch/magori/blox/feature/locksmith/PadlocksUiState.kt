package ch.magori.blox.feature.locksmith

import androidx.compose.runtime.Immutable
import ch.magori.blox.core.locksmith.PadlockResult

/**
 * What the screen « Serrures » shows: the best opening of each open padlock, [openings], padlock 1 first. The padlocks
 * open in order: the open ones are 1 to [open], the next to open is the [frontier], which Reprendre opens. Where
 * starting over from the beginning stands, [clearing].
 *
 * Immutable for Compose: the list of [openings] is never changed once built.
 */
@Immutable
internal data class PadlocksUiState(
    val openings: List<PadlockResult>,
    val clearing: ProgressClearing = ProgressClearing.NONE,
) {
    /** How many padlocks are open. */
    val open: Int get() = openings.size

    /** How many padlocks are open perfectly. */
    val perfect: Int get() = openings.count(PadlockResult::isPerfect)

    /** The next padlock to open. */
    val frontier: Int get() = open + 1

    /** Whether no padlock is open yet: the screen asks to open padlock 1. */
    val isEmpty: Boolean get() = openings.isEmpty()
}

/** Where starting over from the beginning stands (P7.37). */
internal enum class ProgressClearing {
    /** Nothing asked: the button « Recommencer depuis le début » only. */
    NONE,

    /** The confirmation asks whether to clear the progress, « Garder » first. */
    ASKED,

    /** The clear is being written: the confirmation stays open, its answers disabled, and nothing closes it. */
    CLEARING,

    /** The progress could not be cleared: the confirmation, still open, says it failed. */
    FAILED,

    /**
     * The progress is cleared: `app` takes the player back to padlock 1; the confirmation stays open until then, its
     * answers disabled, so that the grid emptied never shows.
     */
    CLEARED,
    ;

    /** Whether the confirmation is open: from the question until the screen goes. */
    val isOpen: Boolean get() = this != NONE

    /** Whether the answers of the confirmation wait: while the clear is written, and once done. */
    val isWaiting: Boolean get() = this == CLEARING || this == CLEARED

    /** Whether the clear has been confirmed: being written, failed or done. */
    val isUnderWay: Boolean get() = this == CLEARING || this == FAILED || this == CLEARED
}
