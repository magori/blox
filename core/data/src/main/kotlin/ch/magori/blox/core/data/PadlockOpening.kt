package ch.magori.blox.core.data

/**
 * The opening of a padlock of the Serrurier as the progress keeps it: the integers of a `PadlockResult` of
 * `core:locksmith`, on which `core:data` does not depend (ADR 0025, amendment P7.28). A padlock of [pieces] pieces,
 * opened in [moves] accepted placements (those taken back included), with [refusals] and [hints]. The rules are those
 * of `PadlockResult`: a padlock has pieces, takes as many moves at least, and no counter is negative; anything else is
 * a programming error, and a file that holds it is corrupted.
 */
data class PadlockOpening(val pieces: Int, val moves: Int, val refusals: Int, val hints: Int) {
    init {
        require(pieces >= 1) { "A padlock has pieces, got $pieces" }
        require(moves >= pieces) { "A padlock of $pieces pieces takes as many moves at least, got $moves" }
        require(refusals >= 0 && hints >= 0) { "No counter is negative, got $refusals refusals and $hints hints" }
    }

    /**
     * Whether this opening is better than [other], of the same padlock: fewer hints first, then fewer moves, then
     * fewer refusals. An equal opening is not better.
     */
    fun isBetterThan(other: PadlockOpening): Boolean {
        require(pieces == other.pieces) { "Openings of $pieces and ${other.pieces} pieces are not of the same padlock" }
        return BEST_FIRST.compare(this, other) < 0
    }

    private companion object {
        val BEST_FIRST: Comparator<PadlockOpening> =
            compareBy(PadlockOpening::hints, PadlockOpening::moves, PadlockOpening::refusals)
    }
}
