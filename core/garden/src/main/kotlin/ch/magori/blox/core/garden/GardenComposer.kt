package ch.magori.blox.core.garden

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.StableRandom

/**
 * The dice of the garden, « Au hasard »: it composes gardens the way the music and the padlocks are composed,
 * candidates drawn at random, rated by their [GardenRating], the best one kept. Public as the entry point of the
 * button; everything it composes with stays internal.
 */
public object GardenComposer {
    /** How many pieces a candidate places. */
    internal const val PIECES: Int = 6

    /** How many candidates a seed composes, those given up included. */
    internal const val CANDIDATES: Int = 20

    /** How many draws each piece of a candidate gets before the candidate is given up. */
    internal const val ATTEMPTS_PER_PIECE: Int = 200

    /**
     * The garden composed in [mood] from [seed], a pure function of both: the same garden for the same mood and seed,
     * as the draws come from a [StableRandom]. It holds [PIECES] pieces, and is the best of [CANDIDATES] candidates.
     *
     * @throws IllegalStateException if every candidate is given up, which breaks an invariant rather than a chance of
     * the draw: six pieces cover 24 of the 224 cells, so that each draw is placed more often than not, and a piece
     * has [ATTEMPTS_PER_PIECE] draws.
     */
    public fun compose(
        mood: GardenMood,
        seed: Long,
    ): Garden = choose(mood, seed)

    /**
     * The garden of the most [points] among [CANDIDATES] candidates composed in [mood] by [compose], those given up
     * (`null`) left out, the first one composed among those of equal points. Each candidate is composed with its own
     * random, drawn from a master random built on [seed], so that it does not depend on the draws of the previous
     * ones. An `IllegalStateException` when every candidate is given up.
     */
    internal fun choose(
        mood: GardenMood,
        seed: Long,
        compose: (GardenMood, StableRandom) -> Garden? = { candidateMood, random -> candidate(candidateMood, random) },
        points: (Garden) -> Int = { GardenRating.of(it).points },
    ): Garden {
        val master = StableRandom(seed)
        val candidates = List(CANDIDATES) { compose(mood, StableRandom(master.nextLong())) }.filterNotNull()
        return checkNotNull(candidates.maxByOrNull(points)) { "The $CANDIDATES candidates of seed $seed were given up" }
    }

    /**
     * One candidate garden in [mood], of [pieces] pieces composed with [random]: each piece is drawn (type, Classic
     * orientation, pivot on a cell of the grid) until the garden accepts it, inside the grid and on free cells.
     * `null`, the candidate given up, when a piece is not placed within [attempts] draws of its own.
     */
    internal fun candidate(
        mood: GardenMood,
        random: StableRandom,
        attempts: Int = ATTEMPTS_PER_PIECE,
        pieces: Int = PIECES,
    ): Garden? {
        var garden = Garden.EMPTY.withMood(mood)
        repeat(pieces) {
            garden = (1..attempts).asSequence()
                .map { garden.place(draw(random)) }
                .filterIsInstance<GardenPlacement.Placed>()
                .firstOrNull()
                ?.garden
                ?: return null
        }
        return garden
    }

    /**
     * A piece of any type, in any of its Classic orientations, its pivot on any cell of the grid. Only the draws of
     * the contract of [StableRandom] are used: `List.random` and `nextInt(until)`.
     */
    private fun draw(random: StableRandom): Piece {
        val type = PieceType.entries.random(random)
        val orientation = ClassicRotationSystem.orientations(type).random(random)
        return Piece(type, orientation, Position(random.nextInt(Garden.COLUMNS), random.nextInt(Garden.ROWS)))
    }
}
