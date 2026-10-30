package ch.magori.blox.core.locksmith

/**
 * The tiers of the padlocks, named after compagnonnage, each one from its [first] padlock to the first of the next
 * (`regles-serrurier.md`, Paliers): apprentice, journeyman, craftsman, then master for ever, beyond 30 on the cycle.
 * Its padlocks hold from the fewest to the most of its [pieces]. Their names and how they are shown are not part of the
 * contract of generation, which [PadlockProfile] holds.
 */
public enum class PadlockTier(private val first: Int, public val pieces: IntRange) {
    APPRENTICE(1, 3..5),
    JOURNEYMAN(4, 6..6),
    CRAFTSMAN(13, 7..7),
    MASTER(21, 7..8),
    ;

    public companion object {
        /** The tier of padlock [level], from 1. */
        public fun of(level: Int): PadlockTier {
            require(level >= 1) { "Padlocks are numbered from 1, got $level" }
            return entries.last { level >= it.first }
        }
    }
}
