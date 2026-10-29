package ch.magori.blox.core.data

import kotlinx.coroutines.flow.Flow

/**
 * The progress of the Serrurier (P7.32): the best opening of each open padlock, padlocks 1 to N, and the next one to
 * open, the frontier N + 1 ([PadlockProgress]). Only integers: `core:data` does not depend on `core:locksmith`
 * (ADR 0025, amendment P7.28).
 *
 * Each change fails with an [java.io.IOException] if the progress cannot be written: the only failure a screen
 * catches. An [IllegalArgumentException] is a precondition the caller broke, a bug, not to be caught.
 */
interface PadlockProgressRepository {
    /**
     * The progress, padlock 1 first.
     *
     * Never fails with an [java.io.IOException]: a progress that cannot be read gives an empty one, once, and the flow
     * then ends. Any other failure is a bug, and goes through to the collector: it is not to be caught.
     */
    val progress: Flow<PadlockProgress>

    /**
     * Records [opening] of [padlock], and tells what it was: the first opening of the frontier, which moves on; a new
     * best of an open padlock, fewer hints, then moves, then refusals ([PadlockOpening.isBetterThan]), which takes the
     * place of its best; or nothing new, the progress unchanged.
     *
     * @throws java.io.IOException if the progress cannot be written; any other failure is a bug.
     * @throws IllegalArgumentException if [padlock] is neither open nor the frontier (the padlocks open in order), or
     * if [opening] has not as many pieces as the best of its padlock: preconditions.
     */
    suspend fun record(
        padlock: Int,
        opening: PadlockOpening,
    ): OpeningOutcome

    /**
     * Forgets every opening, « Recommencer depuis le début » (P7.37): the frontier is padlock 1 again; the padlocks
     * stay the same.
     *
     * @throws java.io.IOException if the progress cannot be written; any other failure is a bug.
     */
    suspend fun clear()
}

/** What an opening recorded by [PadlockProgressRepository.record] was, for the card of the open padlock. */
enum class OpeningOutcome {
    /** The first opening of the frontier, which moves on. */
    FIRST_OPENING,

    /** A better opening of an open padlock, now its best. */
    NEW_BEST,

    /** An opening of an open padlock no better than its best, which is kept. */
    NOTHING_NEW,
}
