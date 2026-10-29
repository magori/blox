package ch.magori.blox.core.data

/**
 * The progress of the Serrurier: the best opening of each open padlock, [openings], padlock 1 first. The padlocks open
 * in order, so the open ones are padlocks 1 to N, and the next one to open is the [frontier], N + 1. Its changes are
 * the rules of the progress, shared by the real repository and the fake.
 */
data class PadlockProgress(val openings: List<PadlockOpening> = emptyList()) {
    /** The next padlock to open, the first one that is not open. */
    val frontier: Int get() = openings.size + 1

    /**
     * The progress after [opening] of [padlock], and what it was, by the rules and preconditions of
     * [PadlockProgressRepository.record].
     */
    internal fun recording(
        padlock: Int,
        opening: PadlockOpening,
    ): Pair<PadlockProgress, OpeningOutcome> {
        require(padlock in 1..frontier) { "Padlock $padlock is neither open nor the frontier, padlock $frontier" }
        val place = padlock - 1
        return when {
            padlock == frontier -> PadlockProgress(openings + opening) to OpeningOutcome.FIRST_OPENING

            opening.isBetterThan(openings[place]) ->
                PadlockProgress(openings.toMutableList().apply { set(place, opening) }) to OpeningOutcome.NEW_BEST

            else -> this to OpeningOutcome.NOTHING_NEW
        }
    }
}
