package ch.magori.blox.core.game.rules

import ch.magori.blox.core.game.model.PieceType
import kotlin.random.Random

/**
 * Piece randomizer of the Classic mode (NES, 1989).
 *
 * A first draw is made among the seven pieces plus one "no piece" value. If it gives "no piece" or
 * the previous piece, a second draw is made, once, among the seven pieces, and kept whatever it is:
 * repeats are rare but possible.
 *
 * The ROM biases that second draw slightly, by adding the orientation id of the previous piece.
 * That bias is a deliberate gap: the second draw here is uniform.
 */
public class ClassicRandomizer internal constructor(private val random: Random) : Randomizer {
    /**
     * A randomizer whose draws are reproducible: the same [seed] gives the same pieces.
     *
     * The sequence comes from `kotlin.random.Random(seed)`, which is not a contract between Kotlin
     * versions: the same seed may give other pieces after a Kotlin upgrade.
     */
    public constructor(seed: Long) : this(Random(seed))

    override fun next(previous: PieceType?): PieceType {
        val firstDraw = random.nextInt(romOrder.size + 1)
        if (firstDraw == noPiece || romOrder[firstDraw] == previous) {
            return romOrder[random.nextInt(romOrder.size)]
        }
        return romOrder[firstDraw]
    }

    private companion object {
        /** Pieces in the order of the ROM, indexed by the draw. */
        val romOrder = listOf(
            PieceType.T,
            PieceType.J,
            PieceType.Z,
            PieceType.O,
            PieceType.S,
            PieceType.L,
            PieceType.I,
        )

        /** The first draw value that gives no piece, and calls for a second draw. */
        val noPiece = romOrder.size
    }
}
