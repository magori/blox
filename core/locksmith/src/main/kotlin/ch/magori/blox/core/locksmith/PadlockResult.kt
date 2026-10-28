package ch.magori.blox.core.locksmith

/**
 * The opening of a padlock of [pieces] pieces, as one game counted it: its accepted placements ([moves], those taken
 * back included), its [refusals] and its [hints]. The padlock opens with its last piece placed, so it takes as many
 * moves as pieces at least; anything else is a programming error.
 */
public data class PadlockResult(
    public val pieces: Int,
    public val moves: Int,
    public val refusals: Int,
    public val hints: Int,
) {
    init {
        require(pieces >= 1) { "A padlock has pieces, got $pieces" }
        require(moves >= pieces) { "A padlock of $pieces pieces takes as many moves at least, got $moves" }
        require(refusals >= 0 && hints >= 0) { "No counter is negative, got $refusals refusals and $hints hints" }
    }

    /** Whether the padlock was opened in one move per piece, without refusal or hint. */
    public val isPerfect: Boolean get() = moves == pieces && refusals == 0 && hints == 0

    /**
     * Whether this opening is better than [other], of the same padlock: fewer hints first, then fewer moves, then
     * fewer refusals. An equal opening is not better.
     */
    public fun isBetterThan(other: PadlockResult): Boolean {
        require(pieces == other.pieces) { "Openings of $pieces and ${other.pieces} pieces are not of the same padlock" }
        return BEST_FIRST.compare(this, other) < 0
    }

    private companion object {
        val BEST_FIRST: Comparator<PadlockResult> =
            compareBy(PadlockResult::hints, PadlockResult::moves, PadlockResult::refusals)
    }
}
