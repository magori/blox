package ch.magori.blox.core.locksmith

import ch.magori.blox.core.game.model.PieceType

/**
 * What padlock [PadlockProfile.of] a level asks of the generator: how many pieces, of which types, a hole of which
 * shape, and whether few solutions are preferred. The table of the tiers is part of the contract of generation
 * (`regles-serrurier.md`, Progression).
 */
internal data class PadlockProfile(
    val pieceCount: Int,
    val types: PieceSet,
    val shape: HoleShape,
    val solutions: SolutionPreference,
) {
    init {
        require(types != PieceSet.ONE_OF_EACH || pieceCount <= PieceType.entries.size) {
            "One of each holds ${PieceType.entries.size} pieces at most, got $pieceCount"
        }
    }

    companion object {
        /** The last padlock of the tiers; the following ones turn on [CYCLE]. */
        private const val LAST_TIERED = 30

        /** The padlocks beyond [LAST_TIERED], by their number modulo 5. */
        private val CYCLE = listOf(
            PadlockProfile(7, PieceSet.ONE_OF_EACH, HoleShape.MIXED, SolutionPreference.FEW),
            PadlockProfile(8, PieceSet.ALL, HoleShape.COMPACT, SolutionPreference.UNIQUE),
            PadlockProfile(8, PieceSet.ALL, HoleShape.SINUOUS, SolutionPreference.FREE),
            PadlockProfile(8, PieceSet.S_Z_T, HoleShape.MIXED, SolutionPreference.FEW),
            PadlockProfile(8, PieceSet.ALL, HoleShape.COMPACT, SolutionPreference.FEW),
        )

        /** The profile of padlock [level], from 1. */
        fun of(level: Int): PadlockProfile {
            require(level >= 1) { "Padlocks are numbered from 1, got $level" }
            return when (level) {
                1 -> PadlockProfile(3, PieceSet.WITHOUT_S_Z, HoleShape.SINUOUS, SolutionPreference.FREE)
                2 -> PadlockProfile(4, PieceSet.WITHOUT_S_Z, HoleShape.SINUOUS, SolutionPreference.FREE)
                3 -> PadlockProfile(5, PieceSet.ALL, HoleShape.SINUOUS, SolutionPreference.FREE)
                in 4..6 -> PadlockProfile(6, PieceSet.ALL, HoleShape.SINUOUS, SolutionPreference.FREE)
                in 7..12 -> PadlockProfile(6, PieceSet.ALL, HoleShape.MIXED, SolutionPreference.FEW)
                in 13..20 -> PadlockProfile(7, PieceSet.ALL, HoleShape.MIXED, SolutionPreference.FEW)
                in 21..LAST_TIERED -> PadlockProfile(8, PieceSet.ALL, HoleShape.COMPACT, SolutionPreference.FEW)
                else -> CYCLE[level % CYCLE.size]
            }
        }
    }
}

/**
 * The types a padlock draws its pieces from, always in the order of [PieceType.entries]; each type [once] at most,
 * or as often as drawn.
 */
internal enum class PieceSet(private val types: List<PieceType>, private val once: Boolean = false) {
    ALL(PieceType.entries),
    WITHOUT_S_Z(PieceType.entries - setOf(PieceType.S, PieceType.Z)),
    ONE_OF_EACH(PieceType.entries, once = true),
    S_Z_T(PieceType.entries.filter { it in setOf(PieceType.S, PieceType.Z, PieceType.T) }),
    ;

    /** The types the next piece is drawn from, the pieces of a candidate [placed] so far. */
    fun drawable(placed: List<PieceType>): List<PieceType> = if (once) types - placed.toSet() else types
}

/** The shape the rating prefers for the hole: winding corridors are easy, a compact hole hides false leads. */
internal enum class HoleShape(val outlineWeight: Int) {
    SINUOUS(10),
    MIXED(5),
    COMPACT(2),
}

/** Whether the rating prefers padlocks of few solutions, or of one: a term of the rating, never a filter. */
internal enum class SolutionPreference(val weightPerSolution: Int) {
    FREE(0),
    FEW(15),
    UNIQUE(45),
}
