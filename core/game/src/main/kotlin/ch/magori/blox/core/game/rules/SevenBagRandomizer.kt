package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.PieceType
import kotlin.random.Random

/**
 * Piece randomizer of the Modern mode: a "bag" of the seven pieces.
 *
 * The seven pieces are shuffled into a bag and drawn one by one; when the bag is empty, a new one is
 * shuffled. Each group of seven draws is thus the seven pieces, and at most twelve draws separate two
 * appearances of the same piece. The previous piece plays no part: the bag is the only state.
 */
public class SevenBagRandomizer internal constructor(private val random: Random) : Randomizer {
    /**
     * A randomizer whose draws are reproducible: the same [seed] gives the same pieces.
     *
     * The sequence comes from `kotlin.random.Random(seed)` and from the shuffle of the standard library
     * (`shuffled`), neither of which is a contract between Kotlin versions: the same seed may give other
     * pieces after a Kotlin upgrade.
     */
    public constructor(seed: Long) : this(Random(seed))

    /** The pieces left in the current bag, in the order they will be drawn. */
    private val bag = ArrayDeque<PieceType>()

    override fun next(previous: PieceType?): PieceType {
        if (bag.isEmpty()) bag.addAll(bagOrder.shuffled(random))
        return bag.removeFirst()
    }

    private companion object {
        /**
         * The pieces of a bag before the shuffle. Written here rather than taken from [PieceType.entries], so
         * that reordering the enum does not change the pieces a seed gives.
         */
        val bagOrder = listOf(
            PieceType.I,
            PieceType.J,
            PieceType.L,
            PieceType.O,
            PieceType.S,
            PieceType.T,
            PieceType.Z,
        )
    }
}
